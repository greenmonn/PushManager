package kr.ac.kaist.nmsl.pushmanager.activity;

import com.google.android.gms.location.DetectedActivity;

import org.altbeacon.beacon.Beacon;

/**
 * Created by wns349 on 4/27/2016.
 */
public class PhoneState {
    private static PhoneState instance = null;

    private State myState = State.Unknown;
    private PhoneStateListener phoneStateListener = null;

    private PhoneState() {

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

    public State updateMyState(State newState) {
        if (this.myState != newState) {
            State oldState = this.myState;
            this.myState = newState;

            // call callbacks
            if (this.phoneStateListener != null) {
                phoneStateListener.onMyPhoneStateChanged(oldState, newState);
            }
        }

        return this.myState;
    }

    public static State getStateFromBeacon(Beacon beacon) {
        if (beacon == null || beacon.getDataFields().size() < 3) {
            return State.Unknown;
        }

        long stateCode = beacon.getDataFields().get(2);
        return State.parse(stateCode);
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
        public void onMyPhoneStateChanged(State oldState, State newState);
    }
}