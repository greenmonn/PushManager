package kr.ac.kaist.nmsl.pushmanager.activity;

import com.google.android.gms.location.DetectedActivity;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import kr.ac.kaist.nmsl.pushmanager.util.Util;

/**
 * Created by wns349 on 4/27/2016.
 */
public class PhoneState {
    private static PhoneState instance = null;

    private State myState = State.Unknown;
    private DetectedActivity myDetectedActivity;
    private boolean isUsingSmartphone = false;
    private Date lastIsUsingSmartphoneUpdated;
    private boolean isTalking = false;
    private Queue<Boolean> isTalkingQueue;
    private PhoneStateListener phoneStateListener = null;

    private PhoneState() {
        isTalkingQueue = new LinkedList<>();
        lastIsUsingSmartphoneUpdated = new Date();
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
        this.myState = newState;

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
        lastIsUsingSmartphoneUpdated = new Date();

        if (this.phoneStateListener != null && this.isUsingSmartphone != isUsingSmartphone) {
            this.isUsingSmartphone = isUsingSmartphone;
            phoneStateListener.onMyPhoneStateChanged();
        }
    }

    public void updateIsTalking (boolean isTalking) {
        if (this.phoneStateListener != null && this.isTalking != isTalking) {
            this.isTalking = isTalking;
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
    public Date getLastIsUsingSmartphoneUpdated () { return lastIsUsingSmartphoneUpdated; }

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