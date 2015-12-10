package kr.ac.kaist.nmsl.pushmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import kr.ac.kaist.nmsl.pushmanager.defer.DeferService;
import kr.ac.kaist.nmsl.pushmanager.notification.NotificationService;
import kr.ac.kaist.nmsl.pushmanager.util.ServiceUtil;
import kr.ac.kaist.nmsl.pushmanager.warning.WarningService;

public class MainActivity extends Activity {
    private static final int ACTIVITY_RESULT_NOTIFICATION_LISTENER_SETTINGS = 142;

    private Context context;

    enum ServiceState{
        NoService,
        NoIntervention,
        DeferService,
        WarningService
    }

    private static final int SERVICE_DURATION_ = 10*1000;

    private ServiceState currentServiceState;

    private Timer mTimer;
    private CountDownTimer mCountDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = getApplicationContext();
        mCountDownTimer = new CountDownTimer(0,0) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
            }
        };

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
                        startNoInterventionService();

                        mTimer = new Timer();
                        mTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (currentServiceState == ServiceState.NoIntervention) {
                                            startDeferService();
                                            startCountDownTimer(ServiceState.DeferService);
                                        } else if (currentServiceState == ServiceState.DeferService) {
                                            startWarningService();
                                            startCountDownTimer(ServiceState.WarningService);
                                        } else if (currentServiceState == ServiceState.WarningService) {
                                            stopAllServices();
                                        }
                                    }
                                });
                            }
                        }, SERVICE_DURATION_, SERVICE_DURATION_);
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
    }

    private void startWarningService(){
        context.startService(new Intent(context, WarningService.class));
        currentServiceState = ServiceState.WarningService;
        updateUIComponents();
    }

    private void startDeferService(){
        context.startService(new Intent(context, DeferService.class));
        currentServiceState = ServiceState.DeferService;
        updateUIComponents();
    }

    private void startNoInterventionService() {
        currentServiceState = ServiceState.NoIntervention;
        updateUIComponents();
        startCountDownTimer(ServiceState.NoIntervention);
        Log.d(Constants.DEBUG_TAG, "NoIntervention started");
    }

    private void stopAllServices(){
        context.stopService(new Intent(context, WarningService.class));
        context.stopService(new Intent(context, DeferService.class));
        currentServiceState = ServiceState.NoService;
        mTimer.cancel();
        mCountDownTimer.cancel();
        updateUIComponents();
    }

    private void startCountDownTimer(final ServiceState serviceState) {
        mCountDownTimer = new CountDownTimer(SERVICE_DURATION_, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                final TextView txtRemainingTime = (TextView) findViewById(R.id.txt_ramaining_time);
                String remaningTime = String.format("%02d:%02d (%d/%d)",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)),
                        currentServiceState.ordinal(), ServiceState.values().length -1
                        );
                txtRemainingTime.setText(remaningTime);
            }

            @Override
            public void onFinish() {
                final TextView txtRemainingTime = (TextView) findViewById(R.id.txt_ramaining_time);
                txtRemainingTime.setText(String.format(getString(R.string.time_zero) + " (%d/%d)", currentServiceState.ordinal(), ServiceState.values().length-1));

                if (serviceState == ServiceState.DeferService) {
                    context.stopService(new Intent(context, DeferService.class));
                } else if (serviceState == ServiceState.WarningService) {
                    context.stopService(new Intent(context, WarningService.class));
                } else if (serviceState == ServiceState.NoIntervention) {
                    Log.d(Constants.DEBUG_TAG, "NoIntervention ended");
                }
            }
        };
        mCountDownTimer.start();
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
            txtRemainingTime.setText(String.format(getString(R.string.time_zero) + " (%d/%d)", currentServiceState.ordinal(), ServiceState.values().length-1));
        }
    }
}
