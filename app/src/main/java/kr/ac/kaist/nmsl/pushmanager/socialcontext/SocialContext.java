package kr.ac.kaist.nmsl.pushmanager.socialcontext;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.location.DetectedActivity;

import org.altbeacon.beacon.Beacon;
import org.w3c.dom.Attr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import be.tarsos.dsp.AudioEvent;
import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.activity.PhoneState;
import kr.ac.kaist.nmsl.pushmanager.audio.AudioResult;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by cjpark on 2016-05-01.
 */
public class SocialContext {
    private ArrayList<ArrayList<DetectedActivity>> detectedActivitiesList;
    private ArrayList<ArrayList<Beacon>> detectedBeaconsList;
    private ArrayList<AudioResult> audioResults;

    private final Object activityLock = new Object();
    private final Object beaconLock = new Object();
    private final Object audioLock = new Object();

    private Instances data;
    private J48 tree;

    public SocialContext (InputStream file) {
        detectedActivitiesList = new ArrayList<>();
        detectedBeaconsList = new ArrayList<>();
        audioResults = new ArrayList<>();

        try {
            data = new Instances(new BufferedReader(new InputStreamReader(file)));
            data.setClassIndex(4);
            String[] options = new String[1];
            options[0] = "-U";

            tree = new J48();
            tree.setOptions(options);
            tree.buildClassifier(data);
        } catch (Exception e) {
            Log.e(Constants.DEBUG_TAG, e.getMessage());
        }
    }

    public boolean getIsBreakpoint (HashMap<Integer, Attribute> currentContext) {
        Instance instance = new DenseInstance(5);

        instance.setDataset(data);

        int i;
        for (i = 0; i < 5; i++) {
            if (currentContext.containsKey(i)) {
                switch (i) {
                    case Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS:
                        instance.setValue(i, currentContext.get(i).doubleValue);
                        break;
                    case Constants.CONTEXT_ATTRIBUTE_TYPES.IS_TALKING:
                    case Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY:
                    case Constants.CONTEXT_ATTRIBUTE_TYPES.OTHER_USING_SMARTPHONE:
                        instance.setValue(i, currentContext.get(i).stringValue);
                        break;
                }
            }
        }

        //instance.setValue(i, "?");

        try {
            double clsLabel = tree.classifyInstance(instance);

            String isBreakpoint = instance.classAttribute().value((int) clsLabel);
            Log.d(Constants.DEBUG_TAG, clsLabel + " -> " + instance.classAttribute().value((int) clsLabel));

            return Boolean.valueOf(isBreakpoint);
        } catch (Exception e) {
            return false;
        }
        /*try {
            unlabeled.setClassIndex(unlabeled.numAttributes() - 1);

            Instances labeled = new Instances(unlabeled);

            for (int i = 0; i < unlabeled.numInstances(); i++) {
                double clsLabel = tree.classifyInstance(unlabeled.instance(i));
                labeled.instance(i).setClassValue(clsLabel);
            }

            return labeled.get(0).classAttribute().toString().equals("true");

        } catch (Exception e) {
            return false;
        }*/
    }

    public HashMap<Integer, Attribute> getCurrentContext () {
        HashMap<Integer, Attribute> currentContext = new HashMap<>();

        for (Attribute attr: processActivities()) {
            currentContext.put(attr.type, attr);
        }

        for (Attribute attr: processBeacons()) {
            currentContext.put(attr.type, attr);
        }

        for (Attribute attr: processAudioResults()) {
            currentContext.put(attr.type, attr);
        }

        return currentContext;
    }

    public void addDetectedAcitivity (ArrayList<DetectedActivity> detectedActivities) {
        synchronized (activityLock) {
            detectedActivitiesList.add(detectedActivities);
        }
    }

    public void addBeacon (ArrayList<Beacon> beacons) {
        synchronized (beaconLock) {
            detectedBeaconsList.add(beacons);
        }
    }

    public void addAudioResult (AudioResult audioResult) {
        synchronized (audioLock) {
            audioResults.add(audioResult);
        }
    }

    private ArrayList<Attribute> processActivities () {
        ArrayList<Attribute> results = new ArrayList<>();

        ArrayList<ArrayList<DetectedActivity>> prev = new ArrayList<>();
        synchronized (activityLock) {
            prev.addAll(detectedActivitiesList);
            detectedActivitiesList.clear();
        }

        if (prev.size() <= 0) {
            return results;
        }

        int totalCount = prev.size();
        int walkingCount = 0;
        int stillCount = 0;

        for (ArrayList<DetectedActivity> detectedActivities: prev) {
            if (detectedActivities.get(0).getType() == DetectedActivity.WALKING ||
                    detectedActivities.get(0).getType() == DetectedActivity.ON_FOOT) {
                walkingCount++;
            } else {
                stillCount++;
            }
        }

        if (walkingCount >= stillCount) {
            results.add(new Attribute(Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY, "MOVING", new Date().getTime()));
        } else {
            results.add(new Attribute(Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY, "STILL", new Date().getTime()));
        }

        Log.d(Constants.DEBUG_TAG, "moving count: " + walkingCount + ", still coung: " + stillCount);
        return results;
    }

    private ArrayList<Attribute> processAudioResults () {
        ArrayList<Attribute> results = new ArrayList<>();

        ArrayList<AudioResult> prev = new ArrayList<>();
        synchronized (audioLock) {
            prev.addAll(audioResults);
            audioResults.clear();
        }

        if (prev.size() <= 0) {
            return results;
        }

        boolean isAnyoneTalking = false;

        for (AudioResult audioResult: prev) {
            if (audioResult.isTalking) {
                isAnyoneTalking = true;
                break;
            }
        }

        results.add(new Attribute(Constants.CONTEXT_ATTRIBUTE_TYPES.IS_TALKING, Boolean.valueOf(isAnyoneTalking).toString(), new Date().getTime()));

        return results;
    }

    private ArrayList<Attribute> processBeacons () {
        ArrayList<Attribute> results = new ArrayList<>();
        ArrayList<ArrayList<Beacon>> prev = new ArrayList<>();
        int othersCount = 0;
        boolean isOtherUsingSmartphone = true;
        int isUsingCount = 0;

        synchronized (beaconLock) {
            prev.addAll(detectedBeaconsList);
            detectedBeaconsList.clear();
        }

        if (prev.size() <= 0) {
            return results;
        }

        ArrayList<Beacon> prevBeacons = prev.get(prev.size()-1);

        //TODO: might need to set RSSI threshold.
       //if (prevBeacons.size() <= 0) {
            othersCount = prevBeacons.size();
        //}

        for (Beacon beacon: prevBeacons) {
            if (PhoneState.getInstance().getIsUsingSmartphoneFromBeacon(beacon)) {
                isUsingCount++;
            }

            //Log.d(Constants.DEBUG_TAG, "talking detected from BLE: " + PhoneState.getInstance().getIsTalkingFromBeacon(beacon));
            if (PhoneState.getInstance().getIsTalkingFromBeacon(beacon)) {
                addAudioResult(new AudioResult(PhoneState.getInstance().getIsTalkingFromBeacon(beacon)));
            }
        }

        isOtherUsingSmartphone = isUsingCount > 0;// && isUsingCount < prevBeacons.size() - 1;

        results.add(new Attribute(Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS, othersCount, new Date().getTime()));
        results.add(new Attribute(Constants.CONTEXT_ATTRIBUTE_TYPES.OTHER_USING_SMARTPHONE, Boolean.valueOf(isOtherUsingSmartphone).toString(), new Date().getTime()));

        return results;
    }

    public class Attribute {
        public int type;
        public long time;
        public double doubleValue;
        public String stringValue;
        public boolean boolValue;

        public Attribute (int type, double doubleValue, long time) {
            this.type = type;
            this.doubleValue = doubleValue;
            this.time = time;
        }

        public Attribute (int type, String stringValue, long time) {
            this.type = type;
            this.stringValue = stringValue;
            this.time = time;
        }

        public Attribute (int type, boolean boolValue, long time) {
            this.type = type;
            this.boolValue = boolValue;
            this.time = time;
        }
    }
}
