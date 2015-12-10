package kr.ac.kaist.nmsl.pushmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import kr.ac.kaist.nmsl.pushmanager.defer.DeferService;
import kr.ac.kaist.nmsl.pushmanager.notification.NotificationService;
import kr.ac.kaist.nmsl.pushmanager.util.ServiceUtil;
import kr.ac.kaist.nmsl.pushmanager.warning.WarningService;

public class MainActivity extends Activity {
    private static final int ACTIVITY_RESULT_NOTIFICATION_LISTENER_SETTINGS = 142;

    private Context context;

    enum ServiceState{
        NoService,
        WarningService,
        DeferService
    }

    private ServiceState currentServiceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = getApplicationContext();

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
        final RadioGroup radioPushManagementMethod = (RadioGroup) findViewById(R.id.radio_push_management_method);
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentServiceState) {
                    case DeferService:
                        context.stopService(new Intent(context, DeferService.class));
                        currentServiceState = ServiceState.NoService;
                        updateUIComponents();
                        break;
                    case WarningService:
                        context.stopService(new Intent(context, WarningService.class));
                        currentServiceState = ServiceState.NoService;
                        updateUIComponents();
                        break;
                    case NoService:
                        int pushManagementMethodId = radioPushManagementMethod.getCheckedRadioButtonId();
                        RadioButton selectedRadio = (RadioButton) findViewById(pushManagementMethodId);
                        Toast.makeText(context, "Starting " + selectedRadio.getText(), Toast.LENGTH_SHORT).show();
                        switch (pushManagementMethodId) {
                            case R.id.radio_defer:
                                context.startService(new Intent(context, DeferService.class));
                                currentServiceState = ServiceState.DeferService;
                                updateUIComponents();
                                break;
                            case R.id.radio_warning:
                                context.startService(new Intent(context, WarningService.class));
                                currentServiceState = ServiceState.WarningService;
                                updateUIComponents();
                                break;
                        }
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

    private void updateUIComponents(){
        final RadioGroup radioPushManagementMethod = (RadioGroup) findViewById(R.id.radio_push_management_method);
        final RadioButton radioWarning = (RadioButton) findViewById(R.id.radio_warning);
        final RadioButton radioDefer = (RadioButton) findViewById(R.id.radio_defer);
        final Button btnControl = (Button) findViewById(R.id.btn_control);

        if(this.currentServiceState == ServiceState.WarningService) {
            // Warning service is already running
            String stopServiceMessage = String.format("%s", getString(R.string.stop));
            btnControl.setText(stopServiceMessage);

            radioWarning.setChecked(true);
            radioPushManagementMethod.setEnabled(false);
            radioDefer.setEnabled(false);
            radioWarning.setEnabled(false);
        } else if (this.currentServiceState == ServiceState.DeferService) {
            // Defer service is already running
            String stopServiceMessage = String.format("%s", getString(R.string.stop));
            btnControl.setText(stopServiceMessage);

            radioDefer.setChecked(true);
            radioDefer.setEnabled(false);
            radioWarning.setEnabled(false);
            radioPushManagementMethod.setEnabled(false);
        } else {
            // No service is running
            btnControl.setText(R.string.start);
            
            radioDefer.setEnabled(true);
            radioWarning.setEnabled(true);
            radioPushManagementMethod.setEnabled(true);
        }
    }
}
