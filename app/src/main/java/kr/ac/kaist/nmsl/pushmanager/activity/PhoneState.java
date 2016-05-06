package kr.ac.kaist.nmsl.pushmanager.activity;

import com.google.android.gms.location.DetectedActivity;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import kr.ac.kaist.nmsl.pushmanager.util.Util;

/**
 * Created by wns349 on 4/27/2016.
 */
public class PhoneState {
    private static PhoneState instance = null;

    private State myState = State.Unknown;
    private boolean isUsingSmartphone = false;
    private boolean isTalking = false;
    private Queue<Boolean> isVoiceQueue;
    private PhoneStateListener phoneStateListener = null;

    private PhoneState() {
        isVoiceQueue = new LinkedList<>();
    }

    public synchronized static PhoneState getInstance() {
        if (instance == null) {
            instance = new PhoneState();
        }
        return instance;
    }

    public void addListener(PhoneStateListener phoneStateListener) {
        this.phoneStateListener = phoneStateListener;
    }

    public PhoneStateListener getListener() {
        return this.phoneStateListener;
    }

    public State updateMyState(State newState) {
        if (this.myState != newState) {
            State oldState = this.myState;
            this.myState = newState;

            // call callbacks
            if (this.phoneStateListener != null) {
                phoneStateListener.onMyPhoneStateChanged();
            }
        }

        return this.myState;
    }

    public State getStateFromBeacon(Beacon beacon) {
        if (beacon == null || beacon.getDataFields().size() < 3) {
            return State.Unknown;
        }

        long stateCode = beacon.getDataFields().get(2);
        return State.parse(stateCode);
    }

    public boolean getIsTalkingFromBeacon(Beacon beacon) {
        if (beacon == null || beacon.getDataFields().size() < 3) {
            return false;
        }

        long isTalkingValue = beacon.getDataFields().get(2);

        if (isTalkingValue == 0) return true;
        else return false;
    }

    public boolean getIsUsingSmartphoneFromBeacon (Beacon beacon) {
        if (beacon == null || beacon.getDataFields().size() < 4) {
            return false;
        }

        long isUsingValue = beacon.getDataFields().get(3);

        if (isUsingValue == 0) return true;
        else return false;
    }

    public void updateIsUsingSmartphone (boolean isUsingSmartphone) {
        this.isUsingSmartphone = isUsingSmartphone;

        if (this.phoneStateListener != null) {
            phoneStateListener.onMyPhoneStateChanged();
        }
    }

    public void updateIsTalking (boolean isVoice) {
        isVoiceQueue.add(isVoice);

        if (isVoiceQueue.size() > 5) {
            isVoiceQueue.poll();
        }

        this.isTalking = Util.isTalking(new ArrayList<>(isVoiceQueue));

        if (this.phoneStateListener != null) {
            phoneStateListener.onMyPhoneStateChanged();
        }
    }

    public static State getStateByDetectedActivity(int detectedActivity) {
        State newState = State.Unknown;
        switch (detectedActivity) {
            case DetectedActivity.IN_VEHICLE: {
                newState = State.InVehicle;
                break;
            }
            case DetectedActivity.ON_BICYCLE: {
                newState = State.OnBicycle;
                break;
            }
            case DetectedActivity.ON_FOOT: {
                newState = State.OnFoot;
                break;
            }
            case DetectedActivity.RUNNING: {
                newState = State.Running;
                break;
            }
            case DetectedActivity.STILL: {
                newState = State.Still;
                break;
            }
            case DetectedActivity.TILTING: {
                newState = State.Tilting;
                break;
            }
            case DetectedActivity.WALKING: {
                newState = State.Walking;
                break;
            }
            case DetectedActivity.UNKNOWN: {
                newState = State.Unknown;
                break;
            }
        }

        return newState;
    }

    public State getMyState(){
        return this.myState;
    }
    public boolean getIsUsingSmartphone () { return this.isUsingSmartphone; }
    public boolean getIsTalking () { return this.isTalking; }

    public enum State {
        Unknown(4),
        InVehicle(0),
        OnBicycle(1),
        OnFoot(2),
        Still(3),
        Walking(7),
        Tilting(5),
        Running(8),;

        final long code;

        State(int code) {
            this.code = code;
        }

        State(long code) {
            this.code = code;
        }

        public long getCode() {
            return this.code;
        }

        public static State parse(long code) {
            for (State s : State.values()) {
                if (s.getCode() == code) {
                    return s;
                }
            }

            return State.Unknown;
        }
    }

    public interface PhoneStateListener {
        void onMyPhoneStateChanged();
    }
}