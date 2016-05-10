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

import kr.ac.kaist.nmsl.pushmanager.Constants;

public class StepCounterService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mStepCounter;

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
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, mStepCounter);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(Constants.DEBUG_TAG, "detected step: " + event.values[0]);
        //Toast.makeText(getApplicationContext(), event.values[0] + " steps.", Toast.LENGTH_SHORT).show();
        Intent i = new  Intent(Constants.INTENT_FILTER_ACTIVITY);
        i.putExtra("activity_probability", 100);
        i.putExtra("activity_type", "STEP");

        //sendBroadcast(i);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
