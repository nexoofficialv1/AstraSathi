package com.astratechnologies.astrasathi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FullAccessActivity extends Activity {
    private static final int REQUEST_RUNTIME = 301;
    private static final int REQUEST_DIALER = 302;
    private TextView masterStatus;
    private TextView accessibilityStatus;
    private TextView notificationStatus;
    private TextView runtimeStatus;
    private TextView specialStatus;
    private TextView wakeStatus;
    private TextView memoryStatus;
    private TextView dialerStatus;
    private Button wakeButton;
    private Button memoryButton;
    private Button dialerButton;
    private SecureMemoryRepository memory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_center);
        memory = new SecureMemoryRepository(this);
        bind();
        wireActions();
    }

    private void bind() {
        masterStatus = findViewById(R.id.masterAccessStatus);
        accessibilityStatus = findViewById(R.id.accessibilityStatus);
        notificationStatus = findViewById(R.id.notificationAccessStatus);
        runtimeStatus = findViewById(R.id.runtimeStatus);
        specialStatus = findViewById(R.id.specialStatus);
        wakeStatus = findViewById(R.id.wakeStatus);
        memoryStatus = findViewById(R.id.memoryStatus);
        dialerStatus = findViewById(R.id.dialerStatus);
        wakeButton = findViewById(R.id.wakeButton);
        memoryButton = findViewById(R.id.memoryButton);
        dialerButton = findViewById(R.id.dialerButton);
    }

    private void wireActions() {
        findViewById(R.id.accessibilityButton).setOnClickListener(v -> open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.notificationAccessButton).setOnClickListener(v -> open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        findViewById(R.id.runtimeButton).setOnClickListener(v -> requestRuntimePermissions());
        dialerButton.setOnClickListener(v -> requestDefaultDialer());
        findViewById(R.id.overlayButton).setOnClickListener(v -> open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        findViewById(R.id.usageButton).setOnClickListener(v -> open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        findViewById(R.id.writeSettingsButton).setOnClickListener(v -> open(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        findViewById(R.id.batteryButton).setOnClickListener(v -> open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        findViewById(R.id.exactAlarmButton).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31) open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())));
            else Toast.makeText(this, "এই Android version-এ আলাদা exact alarm access লাগে না।", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.dndButton).setOnClickListener(v -> open(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        wakeButton.setOnClickListener(v -> toggleWakeMode());
        memoryButton.setOnClickListener(v -> {
            memory.setEnabled(!memory.isEnabled());
            updateStatus();
        });
        findViewById(R.id.clearMemoryButton).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("সব Personal Memory মুছবেন?")
                .setMessage("এই কাজটি ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("স্থায়ীভাবে মুছুন", (dialog, which) -> {
                    memory.clearAll();
                    new LifeContextRepository(this).clearAll();
                    Toast.makeText(this, "সব Personal Memory ও Life Context মুছে দেওয়া হয়েছে।", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("বাতিল", null).show());
        findViewById(R.id.backToAssistantButton).setOnClickListener(v -> finish());
    }

    private void requestRuntimePermissions() {
        List<String> permissions = new ArrayList<>();
        addIfMissing(permissions, Manifest.permission.RECORD_AUDIO);
        addIfMissing(permissions, Manifest.permission.READ_CONTACTS);
        addIfMissing(permissions, Manifest.permission.CAMERA);
        addIfMissing(permissions, Manifest.permission.CALL_PHONE);
        addIfMissing(permissions, Manifest.permission.READ_PHONE_STATE);
        addIfMissing(permissions, Manifest.permission.READ_CALL_LOG);
        addIfMissing(permissions, Manifest.permission.ANSWER_PHONE_CALLS);
        if (Build.VERSION.SDK_INT >= 33) addIfMissing(permissions, Manifest.permission.POST_NOTIFICATIONS);
        if (permissions.isEmpty()) Toast.makeText(this, "প্রয়োজনীয় runtime permissions ইতিমধ্যে দেওয়া আছে।", Toast.LENGTH_SHORT).show();
        else requestPermissions(permissions.toArray(new String[0]), REQUEST_RUNTIME);
    }

    private void addIfMissing(List<String> list, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) list.add(permission);
    }

    private void toggleWakeMode() {
        if (WakeWordService.isRunning()) {
            stopService(new Intent(this, WakeWordService.class));
            updateStatus();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermissions();
            Toast.makeText(this, "Microphone permission দেওয়ার পর আবার ‘হ্যালো সাথী’ চালু করুন।", Toast.LENGTH_LONG).show();
            return;
        }
        Intent service = new Intent(this, WakeWordService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        wakeButton.postDelayed(this::updateStatus, 600);
    }

    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (Exception e) { Toast.makeText(this, "এই setting-টি ফোনে খোলা গেল না।", Toast.LENGTH_SHORT).show(); }
    }

    private void requestDefaultDialer() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager roles = getSystemService(RoleManager.class);
                if (!roles.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(this, "এই ফোনে Default Dialer role পাওয়া যায়নি।", Toast.LENGTH_LONG).show(); return;
                }
                if (roles.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(this, "Astra Sathi ইতিমধ্যে Default Phone app।", Toast.LENGTH_SHORT).show(); return;
                }
                startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_DIALER), REQUEST_DIALER);
            } else {
                Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                        .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
                startActivityForResult(intent, REQUEST_DIALER);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Default Phone app নির্বাচন screen খোলা গেল না।", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() { super.onResume(); updateStatus(); }

    private void updateStatus() {
        boolean accessibility = isAccessibilityEnabled();
        boolean notifications = isNotificationAccessEnabled();
        boolean runtime = hasRuntimePermissions();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean usage = hasUsageAccess();
        boolean write = Settings.System.canWrite(this);
        boolean battery = isBatteryUnrestricted();
        boolean exact = hasExactAlarmAccess();
        boolean dnd = getSystemService(NotificationManager.class).isNotificationPolicyAccessGranted();
        boolean dialer = isDefaultDialer();
        int enabled = (accessibility ? 1 : 0) + (notifications ? 1 : 0) + (runtime ? 1 : 0)
                + (overlay ? 1 : 0) + (usage ? 1 : 0) + (write ? 1 : 0)
                + (battery ? 1 : 0) + (exact ? 1 : 0) + (dnd ? 1 : 0) + (dialer ? 1 : 0);
        masterStatus.setText("সক্রিয় access: " + enabled + "/১০\n"
                + (enabled == 10 ? "Astra Sathi সর্বোচ্চ অনুমোদিত control-এর জন্য প্রস্তুত।"
                : "প্রয়োজন অনুযায়ী বাকি access চালু করুন।"));
        showStatus(accessibilityStatus, accessibility, accessibility ? "চালু — screen interaction প্রস্তুত" : "বন্ধ — UI control কাজ করবে না");
        showStatus(notificationStatus, notifications, notifications ? "চালু — notification control প্রস্তুত" : "বন্ধ");
        showStatus(runtimeStatus, runtime, runtime ? "Voice, Contacts, Camera, Phone ও notification প্রস্তুত" : "এক বা একাধিক permission বাকি আছে");
        showStatus(dialerStatus, dialer, dialer ? "চালু — caller announcement ও call control প্রস্তুত"
                : "বন্ধ — call ধরতে Astra Sathi-কে Default Phone app করুন");
        dialerButton.setText(dialer ? "Default Phone app সক্রিয়" : "Astra Sathi-কে Default Phone করুন");
        specialStatus.setText("Overlay: " + yesNo(overlay) + "  •  Usage: " + yesNo(usage)
                + "\nSystem settings: " + yesNo(write) + "  •  Battery: " + yesNo(battery)
                + "  •  Exact reminder: " + yesNo(exact) + "\nDo Not Disturb: " + yesNo(dnd));
        showStatus(specialStatus, overlay && usage && write && battery && exact && dnd, specialStatus.getText().toString());
        boolean running = WakeWordService.isRunning();
        showStatus(wakeStatus, running, running ? "চালু — persistent microphone indicator দৃশ্যমান" : "বন্ধ");
        wakeButton.setText(running ? "‘হ্যালো সাথী’ বন্ধ করুন" : "‘হ্যালো সাথী’ চালু করুন");
        boolean remember = memory.isEnabled();
        showStatus(memoryStatus, remember, remember ? "চালু — encrypted on-device memory সক্রিয়" : "বন্ধ — ব্যক্তিগত তথ্য মনে রাখা হবে না");
        memoryButton.setText(remember ? "Personal Memory বন্ধ করুন" : "Personal Memory চালু করুন");
    }

    private void showStatus(TextView view, boolean enabled, String text) {
        view.setText((enabled ? "● " : "○ ") + text);
        view.setTextColor(getColor(enabled ? R.color.forest : R.color.danger));
    }

    private String yesNo(boolean value) { return value ? "চালু" : "বন্ধ"; }

    private boolean hasRuntimePermissions() {
        boolean base = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED;
        return base && (Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean isAccessibilityEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String target = new ComponentName(this, SathiAccessibilityService.class).flattenToString();
        return enabled != null && enabled.toLowerCase().contains(target.toLowerCase());
    }

    private boolean isNotificationAccessEnabled() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 27)
            return manager.isNotificationListenerAccessGranted(new ComponentName(this, SathiNotificationService.class));
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    private boolean hasUsageAccess() {
        AppOpsManager manager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        return manager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isBatteryUnrestricted() {
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return manager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean hasExactAlarmAccess() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms();
    }

    private boolean isDefaultDialer() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager roles = getSystemService(RoleManager.class);
                return roles.isRoleAvailable(RoleManager.ROLE_DIALER)
                        && roles.isRoleHeld(RoleManager.ROLE_DIALER);
            }
            return getPackageName().equals(getSystemService(TelecomManager.class).getDefaultDialerPackage());
        } catch (Exception e) { return false; }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DIALER) updateStatus();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateStatus();
    }
}
