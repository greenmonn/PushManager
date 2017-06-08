package kr.ac.kaist.nmsl.pushmanager;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.TimeUnit;

import kr.ac.kaist.nmsl.pushmanager.util.BLEUtil;
import kr.ac.kaist.nmsl.pushmanager.util.ServiceUtil;

public class MainActivity extends Activity {
    private static final int ACTIVITY_RESULT_NOTIFICATION_LISTENER_SETTINGS = 142;

    private Context mContext;

    private ServiceState mServiceState = null;
    private long mRemainingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mContext = this;

        initializeUIComponents();

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.LOCATION_HARDWARE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        },0);
    }

    @Override
    protected void onResume() {

        // Register broadcast receiver
        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(Constants.INTENT_FILTER_MAINSERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalBroadcastReceiver, localFilter);

        IntentFilter globalFilter = new IntentFilter();
        globalFilter.addAction(Constants.INTENT_FILTER_BLE);
        globalFilter.addAction(Constants.INTENT_FILTER_BREAKPOINT);
        this.registerReceiver(mGlobalBroadcastReceiver, globalFilter);

        updateUIComponents();

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        this.unregisterReceiver(mGlobalBroadcastReceiver);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);

        super.onStop();
    }

    private boolean checkIfOkayToStart() {
        if (!BLEUtil.isAdvertisingSupportedDevice(this)) {
            showErrorMessage("BLE is not supported. Make sure it's on.", true);
            return false;
        }

        return true;
    }

    private void initializeUIComponents() {
        showErrorMessage("", false);
        updateUIComponents();

        // Initialize Button
        final Button btnControl = (Button) findViewById(R.id.btn_control);
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnControl.getText().equals(getString(R.string.stop))) {
                    Toast.makeText(mContext, "Stopping service", Toast.LENGTH_SHORT).show();
                    mContext.stopService(new Intent(mContext, MainService.class));

                    updateUIComponents();
                } else {
                    if (!checkIfOkayToStart()) {
                        return;
                    }

                    long duration = 0;
                    try {
                        EditText edtDuration = (EditText) findViewById(R.id.edt_duration);
                        duration = Long.parseLong(edtDuration.getText().toString());
                    } catch (Exception e) {
                        showErrorMessage("Failed to parse duration. " + e.getMessage(), true);
                        return;
                    }

                    int firstPushManagementServiceToExecute = 0;
                    try {
                        RadioGroup radioPushManagementMethod = (RadioGroup) findViewById(R.id.group_mode);
                        firstPushManagementServiceToExecute = radioPushManagementMethod.getCheckedRadioButtonId();
                    } catch (Exception e) {
                        showErrorMessage("Failed to parse first push management service to execute. " + e.getMessage(), true);
                        return;
                    }

                    Toast.makeText(mContext, "Starting service", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(mContext, MainService.class);
                    intent.putExtra("duration", duration);
                    intent.putExtra("firstPushManagementServiceToExecute", firstPushManagementServiceToExecute);
                    mContext.startService(intent);

                    updateUIComponents();
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

        final Button btnAppInfo = (Button) findViewById(R.id.btn_app_info);
        btnAppInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivity(intent);
            }
        });

        final Button btnNotificationTest = (Button) findViewById(R.id.btn_notification_test);
        btnNotificationTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int notificationCount = 0;
                try {
                    notificationCount = Integer.parseInt(((EditText) findViewById(R.id.edt_duration)).getText().toString());
                } catch (Exception e) {
                    notificationCount = 0;
                }

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder b = new NotificationCompat.Builder(getApplicationContext());
                b.setAutoCancel(false)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setTicker("KAIST CS")
                        .setContentTitle("SCAN Notification Manager")
                        .setContentText("You have " + notificationCount + " new notifications.")
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                        .setContentIntent(contentIntent)
                        .setContentInfo("");
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(1234, b.build());
            }
        });

        final TextView txtOsVersion = (TextView) findViewById(R.id.txt_os_version);
        txtOsVersion.setText(Build.VERSION.RELEASE + " / BLE: " + BLEUtil.isAdvertisingSupportedDevice(this));

        final RadioGroup groupMode = (RadioGroup) findViewById(R.id.group_mode);
        groupMode.check(R.id.radio_btn_no_intervention);
    }

    private void showErrorMessage(String errorMessage, boolean isError) {
        final TextView txtErrorTextView = (TextView) findViewById(R.id.txt_error_status);
        txtErrorTextView.setVisibility(isError ? View.VISIBLE : View.INVISIBLE);
        txtErrorTextView.setText(errorMessage != null ? errorMessage : "");
    }

    private void updateUIComponents() {
        final Button btnControl = (Button) findViewById(R.id.btn_control);
        final TextView txtRemainingTime = (TextView) findViewById(R.id.txt_remaining_time);
        final TextView txtServiceStatus = (TextView) findViewById(R.id.txt_service_status);
        final EditText edtDuration = (EditText) findViewById(R.id.edt_duration);
        final RadioGroup radioPushManagementMethod = (RadioGroup) findViewById(R.id.group_mode);

        if (!ServiceUtil.isServiceRunning(this, MainService.class)) {
            mServiceState = ServiceState.NoService;
        }

        if (mServiceState == null) {
            return;
        }

        switch (mServiceState) {
            case NoService:
                btnControl.setText(getString(R.string.start));
                txtRemainingTime.setText(String.format(getString(R.string.time_zero)));
                txtServiceStatus.setText(mServiceState.toString());
                edtDuration.setEnabled(true);
                radioPushManagementMethod.setEnabled(true);
                break;

            case DeferService:
            case NoIntervention:
                btnControl.setText(getString(R.string.stop));
                String remainingTime = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(mRemainingTime),
                        TimeUnit.MILLISECONDS.toSeconds(mRemainingTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mRemainingTime))
                );
                txtRemainingTime.setText(remainingTime);
                txtServiceStatus.setText(mServiceState.toString());
                edtDuration.setEnabled(false);
                radioPushManagementMethod.setEnabled(false);
                showErrorMessage("", false);
                break;

        }
    }

    private final BroadcastReceiver mGlobalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_FILTER_BLE)) {
                if (intent.hasExtra(Constants.BLUETOOTH_NOT_FOUND)
                        && intent.getBooleanExtra(Constants.BLUETOOTH_NOT_FOUND, false)) {
                    showErrorMessage("Bluetooth not found.", true);
                }

                if (intent.hasExtra(Constants.BLUETOOTH_DISABLED)
                        && intent.getBooleanExtra(Constants.BLUETOOTH_DISABLED, false)) {
                    Intent enableBtIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_BREAKPOINT)) {
                String msg = "";
                msg += "isBreakpoint: " + intent.getBooleanExtra("breakpoint", false) + "\n";
                msg += "activity: " + intent.getStringExtra("activity") + "\n";
                msg += "is_talking: " + intent.getStringExtra("is_talking") + "\n";
                msg += "is_using: " + intent.getStringExtra("is_using") + "\n";
                msg += "with_others: " + intent.getDoubleExtra("with_others", 0.0) + "\n";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_FILTER_MAINSERVICE)) {
                if (intent.getExtras().containsKey("alive")) {
                    updateUIComponents();
                }

                if (intent.getExtras().containsKey("remainingTime") && intent.getExtras().containsKey("toggleCount") && intent.getExtras().containsKey("totalTime") && intent.getExtras().containsKey("pushManagementMethodId")) {
                    long remainingTimeForThisManagement = intent.getLongExtra("remainingTime", -1L);
                    int toggleCount = intent.getIntExtra("toggleCount", -1);
                    long totalTime = intent.getLongExtra("totalTime", -1L);
                    int pushManagementMethodId = intent.getIntExtra("pushManagementMethodId", -1);
                    long serviceToggleTimeToRun = (totalTime / MainService.TOTAL_NUMBER_OF_TOGGLES);
                    long elapsedTime = serviceToggleTimeToRun * toggleCount + (serviceToggleTimeToRun - remainingTimeForThisManagement);

                    mRemainingTime = totalTime - elapsedTime;
                    mServiceState = pushManagementMethodId == R.id.radio_btn_defer ? ServiceState.DeferService : ServiceState.NoIntervention;
                    updateUIComponents();
                }

                if (intent.getExtras().containsKey("error")) {
                    showErrorMessage(intent.getStringExtra("error"), true);
                }
            }
        }
    };
}
