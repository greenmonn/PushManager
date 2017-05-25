package kr.ac.kaist.nmsl.pushmanager.event;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.MainService;
import kr.ac.kaist.nmsl.pushmanager.activity.PhoneState;
import kr.ac.kaist.nmsl.pushmanager.socialcontext.SocialContext;
import kr.ac.kaist.nmsl.pushmanager.util.ServiceUtil;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

public class EventRecorderService extends AccessibilityService {
    private Timer timer;

    public EventRecorderService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(Constants.DEBUG_TAG, "onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        //info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK; //- AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED + AccessibilityEvent.TYPE_VIEW_SCROLLED + AccessibilityEvent.TYPE_VIEW_CLICKED + AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        //info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //TODO : change to be expired by 'first' used time
                Log.d(Constants.DEBUG_TAG, "isUsing is about to be expired! " + (new Date().getTime() - PhoneState.getInstance().getLastIsUsingSmartphoneUpdated().getTime()) + ", " + PhoneState.getInstance().getIsUsingSmartphone());

                if (PhoneState.getInstance().getIsUsingSmartphone() && Constants.SMARTPHONE_USE_EXPIRE < (new Date().getTime() - PhoneState.getInstance().getFirstIsUsingSmartphoneUpdated().getTime())) {
                    PhoneState.getInstance().updateUseExpired(true);

                    Log.d(Constants.DEBUG_TAG, "isUsing expired due to first using timeout!");
                }

                if (PhoneState.getInstance().getIsUsingSmartphone() && Constants.SMARTPHONE_NOT_USING_INTERVAL < (new Date().getTime() - PhoneState.getInstance().getFirstIsUsingSmartphoneUpdated().getTime())) {

                    if (ServiceUtil.isServiceRunning(getApplicationContext(), MainService.class)) {
                        Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "SMARTPHONE_USE", "TYPE_WINDOW_STATE_CHANGED, android.widget.FrameLayout, com.android.systemui, Turn into idle mode");
                    }

                    SocialContext.getInstance().setMeUsingSmartphone(false);
                    PhoneState.getInstance().updateUseExpired(false); //ambiguous case : what if user starts to use immediately?
                    PhoneState.getInstance().updateIsUsingSmartphone(false);

                    //when to change 'expired' state?
                    //true -> false
                    //false -> true

                    Log.d(Constants.DEBUG_TAG, "isUsing expired due to last using timeout!");
                }
            }
        }, Constants.SMARTPHONE_NOT_USING_INTERVAL, Constants.SMARTPHONE_NOT_USING_INTERVAL);
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String msg = String.format(
                //"onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                //[time] [type] [class] [package] [text]",
                "%s, %s, %s, %s",
                AccessibilityEvent.eventTypeToString(event.getEventType()), event.getClassName(), event.getPackageName(),
                getEventText(event));
        //Log.d(Constants.DEBUG_TAG, "[accessibilityevent] " + msg);
        if (ServiceUtil.isServiceRunning(this, MainService.class)) {
            Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "SMARTPHONE_USE", msg);
        }


        Intent i = new Intent(Constants.INTENT_FILTER_USING_SMARTPHONE);
        i.putExtra("event_type", event.getEventType());
        i.putExtra("event_text", getEventText(event));

        i.putExtra("is_using", !(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                (getEventText(event).contains("lock") || getEventText(event).contains("Lock") || getEventText(event).contains("잠급") || getEventText(event).contains("잠금"))));
        // TODO : ignore during "expired state"
        sendBroadcast(i);
    }

    @Override
    public void onInterrupt() {
        Log.d(Constants.DEBUG_TAG, "onInterrupt");
    }

    private String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb.toString();
    }
}
