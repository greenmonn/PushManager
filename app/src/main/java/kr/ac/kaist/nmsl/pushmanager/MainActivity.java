package kr.ac.kaist.nmsl.pushmanager;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import com.parse.Parse;
import com.parse.ParseInstallation;

import kr.ac.kaist.nmsl.pushmanager.defer.DeferService;
import kr.ac.kaist.nmsl.pushmanager.notification.NotificationService;
import kr.ac.kaist.nmsl.pushmanager.util.ServiceUtil;
import kr.ac.kaist.nmsl.pushmanager.util.Util;
import kr.ac.kaist.nmsl.pushmanager.warning.WarningService;

public class MainActivity extends Activity {
    private static final int ACTIVITY_RESULT_NOTIFICATION_LISTENER_SETTINGS = 142;
    public static final String FILE_UTIL_FILE_DATETIME_FORMAT = "yyyyMMdd_HHmmss";

    private Context context;

    enum ServiceState{
        NoService,
        NoIntervention,
        DeferService,
        WarningService
    }
    private ServiceState currentServiceState;

    private Timer mTimer;
    private CountDownTimer mCountDownTimer;

    private DevicePolicyManager mDPM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize SCAN folder
        File dir = new File(Environment.getExternalStoragePublicDirectory(Constants.DIR_NAME).getAbsolutePath());
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }

        this.context = getApplicationContext();
        /*mCountDownTimer = new CountDownTimer(0,0) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
            }
        };*/
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Parse notification
        try {
            Parse.initialize(this, Credentials.PARSE_APPLICATION_ID, Credentials.PARSE_CLIENT_KEY);
            ParseInstallation.getCurrentInstallation().saveInBackground();
        } catch (IllegalStateException e){
            Log.e(Constants.TAG, e.getLocalizedMessage());
        }

        // Check if service is already running or not
        if(ServiceUtil.isServiceRunning(context, WarningService.class)) {
            currentServiceState = ServiceState.WarningService;
        } else if (ServiceUtil.isServiceRunning(context, DeferService.class)){
            currentServiceState = ServiceState.DeferService;
        } else {
            currentServiceState = ServiceState.NoService;
        }

        context.startService(new Intent(context, NotificationService.class));

        // Update UI accordingly
        updateUIComponents();

        // Initialize Button
        final Button btnControl = (Button) findViewById(R.id.btn_control);
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentServiceState) {
                    case DeferService:
                    case WarningService:
                    case NoIntervention:
                        stopAllServices();
                        break;
                    case NoService:
                        Toast.makeText(context, "Start managing cellphone use", Toast.LENGTH_SHORT).show();

                        mTimer = new Timer();
                        final long duration = Long.parseLong(((EditText)findViewById(R.id.edt_duration)).getText().toString()) * 1000L;

                        final RadioGroup radioPushManagementMethod = (RadioGroup) findViewById(R.id.group_mode);
                        int pushManagementMethodId = radioPushManagementMethod.getCheckedRadioButtonId();

                        SimpleDateFormat fileDateFormat = new SimpleDateFormat(FILE_UTIL_FILE_DATETIME_FORMAT);
                        Constants.LOG_ENABLED = true;

                        switch (pushManagementMethodId) {
                            case R.id.radio_btn_no_intervention:
                                Constants.LOG_NAME = fileDateFormat.format(new Date()) + "_" + ServiceState.NoIntervention.toString();
                                startNoInterventionService();
                                break;
                            case R.id.radio_btn_defer:
                                Constants.LOG_NAME = fileDateFormat.format(new Date()) + "_" + ServiceState.DeferService.toString();
                                startDeferService();
                                break;
                            case R.id.radio_btn_warning:
                                Constants.LOG_NAME = fileDateFormat.format(new Date()) + "_" + ServiceState.WarningService.toString();
                                startWarningService();
                                break;
                        }

                        startCountDownTimer();
                        mTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        stopAllServices();
                                    }
                                });
                            }
                        }, duration);

                        mDPM.lockNow();
                        break;
                }
            }
        });

        // Initialize notification setting button
        final Button btnNotificationSetting = (Button) findViewById(R.id.btn_notification_setting);
        btnNotificationSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivityForResult(settingIntent, ACTIVITY_RESULT_NOTIFICATION_LISTENER_SETTINGS);
            }
        });

        // Initialize notification setting button
        final Button btnAccessibilitySetting = (Button) findViewById(R.id.btn_accessibility_setting);
        btnAccessibilitySetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(settingIntent);
            }
        });

        final Button btnEnableAdmin = (Button) findViewById(R.id.btn_admin);
        btnEnableAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                startActivity(intent);
            }
        });

        final RadioGroup groupMode = (RadioGroup) findViewById(R.id.group_mode);
        groupMode.check(R.id.radio_btn_no_intervention);
    }

    private void startWarningService(){
        context.startService(new Intent(context, WarningService.class));
        currentServiceState = ServiceState.WarningService;
        updateUIComponents();
        if (Constants.LOG_ENABLED) {
            Util.writeLogToFile(context, Constants.LOG_NAME, "==============Warning started===============");
        }
    }

    private void startDeferService(){
        final long duration = Long.parseLong(((EditText)findViewById(R.id.edt_duration)).getText().toString()) * 1000L;
        Intent intent = new Intent(context, DeferService.class);
        intent.putExtra("duration", duration);
        context.startService(intent);
        currentServiceState = ServiceState.DeferService;
        updateUIComponents();
        if (Constants.LOG_ENABLED) {
            Util.writeLogToFile(context, Constants.LOG_NAME, "==============Defer started===============");
        }
    }

    private void startNoInterventionService() {
        currentServiceState = ServiceState.NoIntervention;
        updateUIComponents();
        Log.d(Constants.DEBUG_TAG, "NoIntervention started");
        if (Constants.LOG_ENABLED) {
            Util.writeLogToFile(context, Constants.LOG_NAME, "==============NoIntervention started===============");
        }
    }

    private void stopAllServices(){
        context.stopService(new Intent(context, WarningService.class));
        context.stopService(new Intent(context, DeferService.class));
        currentServiceState = ServiceState.NoService;
        mTimer.cancel();
        mCountDownTimer.cancel();
        updateUIComponents();
        Log.d(Constants.DEBUG_TAG, "All ended");
        if (Constants.LOG_ENABLED) {
            Util.writeLogToFile(context, Constants.LOG_NAME, "==============All ended===============");
            Constants.LOG_ENABLED = false;
        }
    }

    private void startCountDownTimer() {
        final long duration = Long.parseLong(((EditText)findViewById(R.id.edt_duration)).getText().toString()) * 1000L;
        mCountDownTimer = new CountDownTimer(duration, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                final TextView txtRemainingTime = (TextView) findViewById(R.id.txt_ramaining_time);
                String remainingTime = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))
                        );
                txtRemainingTime.setText(remainingTime);
            }

            @Override
            public void onFinish() {
                final TextView txtRemainingTime = (TextView) findViewById(R.id.txt_ramaining_time);
                txtRemainingTime.setText(String.format(getString(R.string.time_zero), currentServiceState.ordinal(), ServiceState.values().length - 1));
            }
        }.start();
    }

    private void updateUIComponents(){
        final Button btnControl = (Button) findViewById(R.id.btn_control);
        final TextView txtRemainingTime = (TextView) findViewById(R.id.txt_ramaining_time);
        final TextView txtServiceStatus = (TextView) findViewById(R.id.txt_service_status);

        if(this.currentServiceState == ServiceState.WarningService) {
            // Warning service is already running
            String stopServiceMessage = String.format("%s", getString(R.string.stop));
            String serviceStatus = getString(R.string.service_status_warning);
            btnControl.setText(stopServiceMessage);
            txtServiceStatus.setText(serviceStatus);
        } else if (this.currentServiceState == ServiceState.DeferService) {
            // Defer service is already running
            String stopServiceMessage = String.format("%s", getString(R.string.stop));
            String serviceStatus = getString(R.string.service_status_defer);
            btnControl.setText(stopServiceMessage);
            txtServiceStatus.setText(serviceStatus);
        } else if (this.currentServiceState == ServiceState.NoIntervention) {
            txtServiceStatus.setText(getString(R.string.service_status_no_invention));
            ((Button) findViewById(R.id.btn_control)).setText(getString(R.string.stop));
        } else {
            // No service is running
            btnControl.setText(R.string.start);
            txtServiceStatus.setText(getString(R.string.service_status_nothing));
            txtRemainingTime.setText(String.format(getString(R.string.time_zero)));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Constants.LOG_ENABLED = false;
    }
}
