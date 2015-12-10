package kr.ac.kaist.nmsl.pushmanager.push;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import kr.ac.kaist.nmsl.pushmanager.Constants;

/**
 * Created by wns349 on 2015-12-10.
 */
public class MyParsePushBroadcastReceiver  extends ParsePushBroadcastReceiver{

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        JSONObject pushData = null;
        try {
            pushData = new JSONObject(intent.getStringExtra(KEY_PUSH_DATA));
        } catch (JSONException e) {
            Log.e(Constants.TAG, "Unexpected JSONException when receiving push data: ", e);
            return;
        }


        Log.i(Constants.TAG, pushData.toString());

        /*JSONObject pushData = null;
        try {
            pushData = new JSONObject(intent.getStringExtra(KEY_PUSH_DATA));
        } catch (JSONException e) {
            Log.e(Constants.TAG, "Unexpected JSONException when receiving push data: ", e);
        }

        // If the push data includes an action string, that broadcast intent is fired.
        String action = null;
        if (pushData != null) {
            action = pushData.optString("action", null);
        }
        if (action != null) {
            Bundle extras = intent.getExtras();
            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtras(extras);
            broadcastIntent.setAction(action);
            broadcastIntent.setPackage(context.getPackageName());
            context.sendBroadcast(broadcastIntent);
        }

        Notification notification = getNotification(context, intent);

        if (notification != null) {
            ParseNotificationManager.getInstance().showNotification(context, notification);
        }*/
    }
}
