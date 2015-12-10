package kr.ac.kaist.nmsl.pushmanager.push;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
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
import kr.ac.kaist.nmsl.pushmanager.warning.WarningLayout;

/**
 * Created by wns349 on 2015-12-10.
 */
public class MyParsePushBroadcastReceiver  extends ParsePushBroadcastReceiver{

    private PushNewsLayout pushNewsLayout = null;

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
        if (pushNewsLayout != null){
            return;
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

        pushNewsLayout = new PushNewsLayout(context, null);

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
            pushNewsLayout.setVisibility(View.VISIBLE);
        }
    }
}
