package kr.ac.kaist.nmsl.pushmanager.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URL;

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.R;
import kr.ac.kaist.nmsl.pushmanager.warning.WarningLayout;

/**
 * Created by wns349 on 2015-12-10.
 */
public class MyParsePushBroadcastReceiver  extends ParsePushBroadcastReceiver{

    private static PushNewsLayout pushNewsLayout = null;

    @Override
    protected void onPushReceive(Context context, Intent intent) {

        addPushNewsLayout(context);

        JSONObject pushData = null;
        String url = null;
        try {
            pushData = new JSONObject(intent.getStringExtra(KEY_PUSH_DATA));
            url = pushData.getString("alert");
        } catch (JSONException e) {
            Log.e(Constants.TAG, "Unexpected JSONException when receiving push data: ", e);
            return;
        }

        Log.i(Constants.TAG, "URL to read: " + url);

        if(url == null || url.isEmpty()){
            Log.e(Constants.TAG, "URL is empty.");
            return;
        }

        (new RetrieveWebContent(context)).execute(url);
    }

    private void addPushNewsLayout(Context context) {
        synchronized (this) {
            if (pushNewsLayout != null) {
                return;
            }
        }

        WindowManager.LayoutParams layoutParams = null;
        WindowManager windowManager = null;

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        layoutParams.flags &= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

        pushNewsLayout = new PushNewsLayout(context, null);

        Log.d(Constants.TAG, "NewsPush AddView called");
        windowManager.addView(pushNewsLayout, layoutParams);

        pushNewsLayout.setVisibility(View.GONE);
    }

    class WebContent {
        public String title;
        public String URL;
        public String content;
    }

    class RetrieveWebContent extends AsyncTask<String, Void, WebContent> {
        private final Context context;

        public RetrieveWebContent(Context context){
            this.context = context;
        }

        protected WebContent doInBackground(String ... urls) {
            try {
                String url = urls[0];
                //Document doc = Jsoup.parse(new URL(url).openStream(), "EUC-KR", url);
                Document doc = Jsoup.connect(urls[0]).get();
                Elements title = doc.getElementsByAttributeValue("property", "og:title");
                Elements content = doc.getElementsByAttributeValue("property", "og:description");

                WebContent webContent = new WebContent();
                webContent.title = title.attr("content");
                webContent.content = content.attr("content");
                webContent.URL = urls[0];

                return webContent;
            } catch (Exception e){
                Log.e(Constants.TAG, "Failed to parse web.");
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(WebContent webContent) {
            if(webContent == null){
                return;
            }

            pushNewsLayout.setNewsTitle(webContent.title);
            pushNewsLayout.setNewsContent(webContent.content);
            pushNewsLayout.setNewsURL(webContent.URL);
            if(pushNewsLayout.getVisibility() == View.VISIBLE){
                pushNewsLayout.setVisibility(View.GONE);
            }
            pushNewsLayout.setVisibility(View.VISIBLE);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(webContent.title)
                            .setContentText(webContent.content);
            builder.setVibrate(new long[]{1000, 1000});
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
            builder.setAutoCancel(true);

            Uri uri = Uri.parse(webContent.URL);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            );
            builder.setContentIntent(resultPendingIntent);

            int notificationID = 1923;
            NotificationManager notiManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // notificationID allows you to update the notification later on. //
            notiManager.notify(notificationID, builder.build());
        }
    }
}
