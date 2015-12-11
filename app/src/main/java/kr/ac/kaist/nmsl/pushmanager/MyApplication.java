package kr.ac.kaist.nmsl.pushmanager;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseInstallation;

/**
 * Created by wns349 on 2015-12-11.
 */
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();


        // Parse notification
        try {
            Parse.initialize(this, Credentials.PARSE_APPLICATION_ID, Credentials.PARSE_CLIENT_KEY);
            ParseInstallation.getCurrentInstallation().saveInBackground();
        } catch (IllegalStateException e){
            Log.e(Constants.TAG, e.getLocalizedMessage());
        }

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
