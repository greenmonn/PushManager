package kr.ac.kaist.nmsl.pushmanager.activity;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.audio.SilenceAudioProcessor;

public class StepCounterService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mStepCounter;
    private ArrayList<Float> stepCounterList;
    private Date lastStepUpdated;
    private Timer timer;

    private final int NO_STEP_DURATION = 5000;
    private final int STEP_THRESHOLD = 3;

    public StepCounterService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        stepCounterList = new ArrayList<>();
        lastStepUpdated = new Date();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (new Date().getTime() - lastStepUpdated.getTime() > NO_STEP_DURATION ) {
                    stepCounterList.clear();

                    Intent i = new  Intent(Constants.INTENT_FILTER_ACTIVITY);
                    i.putExtra("activity_probability", 100);
                    i.putExtra("activity_name", "NO_STEP");

                    sendBroadcast(i);
                }
            }
        }, NO_STEP_DURATION, NO_STEP_DURATION);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, mStepCounter);
        timer.cancel();
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(Constants.DEBUG_TAG, "detected step: " + event.values[0]);

        stepCounterList.add(event.values[0]);
        lastStepUpdated = new Date();

        if (stepCounterList.size() > STEP_THRESHOLD) {
            //Toast.makeText(getApplicationContext(), event.values[0] + " steps.", Toast.LENGTH_SHORT).show();
            Intent i = new  Intent(Constants.INTENT_FILTER_ACTIVITY);
            i.putExtra("activity_probability", 100);
            i.putExtra("activity_name", "STEP");

            sendBroadcast(i);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
