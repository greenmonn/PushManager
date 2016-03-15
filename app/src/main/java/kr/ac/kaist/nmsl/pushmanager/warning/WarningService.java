package kr.ac.kaist.nmsl.pushmanager.warning;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import java.util.TimerTask;

import kr.ac.kaist.nmsl.pushmanager.Constants;

public class WarningService extends Service {
    private static final String INTENT_FILTER = "kr.ac.kaist.nmsl.pushmanager";

    private Context context;

    private WarningLayout warningLayout;

    private WindowManager.LayoutParams layoutParams = null;
    private WindowManager windowManager = null;

    private WarningServiceReceiver warningServiceReceiver = null;

    private long previousWarningAt = 0L;

    @Override
    public void onCreate(){
        super.onCreate();
        this.context = getApplicationContext();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        warningLayout = new WarningLayout(getApplicationContext(), null);

        windowManager.addView(warningLayout, layoutParams);
        hideWarningLayout();

        warningServiceReceiver = new WarningServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_FILTER);
        registerReceiver(warningServiceReceiver, filter);

        Log.d(Constants.DEBUG_TAG, "WarningService Created");
    }

    public void showWarningLayout(){
        warningLayout.setVisibility(View.VISIBLE);
        Log.d(Constants.DEBUG_TAG, "Show Warning Layout called");
    }

    public void hideWarningLayout(){
        warningLayout.setVisibility(View.GONE);

        Log.d(Constants.DEBUG_TAG, "Hide Warning Layout called");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(warningServiceReceiver != null) {
            unregisterReceiver(warningServiceReceiver);
        }
        if(warningLayout != null){
            windowManager.removeView(warningLayout);
        }

        Log.d(Constants.DEBUG_TAG, "WarningService Destroyed");

        stopSelf();
    }

    class WarningServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String pack = intent.getStringExtra("package");
            Log.d(Constants.TAG, "Package: "+pack);
            if(intent.getStringExtra("command").equals("posted")) {
                if( (System.currentTimeMillis() - previousWarningAt) >= Constants.WARNING_DELAY_INTERVAL && !isWhiteListedApp(pack)) {
                    Log.d(Constants.TAG, "Showing warning message");
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showWarningLayout();
                        }
                    }, Constants.WARNING_POPUP_SHOW_DELAY);
                    previousWarningAt = System.currentTimeMillis();
                } else {
                    Log.d(Constants.TAG, "Too many warning messages...");
                }
            } else if (intent.getStringExtra("command").equals("removed")) {
                hideWarningLayout();
            }
        }

        private boolean isWhiteListedApp(String pack){
            if(pack == null || pack.isEmpty()){
                return true;
            }

            for(String s : Constants.WHITELIST_APPS){
                if(s.equals(pack)){
                    return true;
                }
            }

            return false;
        }
    }
}