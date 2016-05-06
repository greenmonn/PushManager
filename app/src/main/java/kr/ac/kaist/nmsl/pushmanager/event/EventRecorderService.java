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

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

public class EventRecorderService extends AccessibilityService {
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
        //info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK - AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String msg = String.format(
                //"onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
                //[time] [type] [class] [package] [text]",
                "%s, %s, %s, %s, %s",
                event.getEventTime(), AccessibilityEvent.eventTypeToString(event.getEventType()), event.getClassName(), event.getPackageName(),
                getEventText(event));
        //Log.d(Constants.DEBUG_TAG, msg);
        if (Constants.LOG_ENABLED) {
            Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, msg);
        }

        Intent i = new Intent(Constants.INTENT_FILTER_USING_SMARTPHONE);
        i.putExtra("event_type", event.getEventType());
        i.putExtra("event_text", getEventText(event));

        //TODO: change it after testing
        //i.putExtra("is_using", !getEventText(event).equals("Lock screen."));
        i.putExtra("is_using", false);

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
