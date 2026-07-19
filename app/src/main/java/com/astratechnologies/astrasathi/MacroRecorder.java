package com.astratechnologies.astrasathi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/** Records an explicitly initiated, privacy-filtered demonstration as a reusable Routine. */
public final class MacroRecorder {
    public static final class Result {
        public final boolean success;
        public final String message;
        Result(boolean success, String message) { this.success = success; this.message = message; }
    }

    private enum Kind { OPEN_APP, CLICK, TYPE, SCROLL }

    private static final class RecordedStep {
        final Kind kind;
        final String key;
        final String command;
        RecordedStep(Kind kind, String key, String command) {
            this.kind = kind; this.key = key; this.command = command;
        }
    }

    private static final MacroRecorder INSTANCE = new MacroRecorder();
    private static final int MAX_STEPS = 60;
    private static final int NOTIFICATION_ID = 7002;
    private static final String CHANNEL = "astra_sathi_teach_mode";
    private static final String OWN_PACKAGE = "com.astratechnologies.astrasathi";

    private final List<RecordedStep> steps = new ArrayList<>();
    private boolean recording;
    private String lastPackage = "";
    private long lastRecordedAt;
    private String skippedReason = "";
    private Context appContext;

    private MacroRecorder() { }
    public static MacroRecorder get() { return INSTANCE; }

    public synchronized boolean isRecording() { return recording; }
    public synchronized int stepCount() { return steps.size(); }

    public synchronized Result start(Context context, String foregroundPackage) {
        if (recording) return new Result(false, "Teach Mode ইতিমধ্যে রেকর্ড করছে।");
        if (!SathiAccessibilityService.isConnected())
            return new Result(false, "Teach Mode-এর জন্য Screen Control access চালু করুন।");
        if (!new SecureMemoryRepository(context).isEnabled())
            return new Result(false, "Routine নিরাপদে রাখার জন্য Personal Memory চালু করুন।");
        if (WorkflowRunner.get().isRunning())
            return new Result(false, "চলমান Workflow শেষ হলে Teach Mode শুরু করুন।");
        appContext = context.getApplicationContext();
        steps.clear();
        lastPackage = "";
        skippedReason = "";
        lastRecordedAt = 0;
        recording = true;
        recordPackage(foregroundPackage);
        showNotification();
        return new Result(true, "Teach Mode শুরু হয়েছে। কাজটি একবার করে দেখান। Password, OTP, PIN ও protected action রেকর্ড হবে না।");
    }

    public synchronized Result stopAndSave(Context context, String name) {
        if (!recording) return new Result(false, "Teach Mode এখন রেকর্ড করছে না।");
        if (name == null || name.trim().isEmpty())
            return new Result(false, "রেকর্ড চলছে। বন্ধ করতে একটি নাম বলুন—যেমন, ‘রেকর্ড বন্ধ করো সকাল নামে’।");
        recording = false;
        cancelNotification();
        if (steps.size() < 2) {
            int count = steps.size();
            reset();
            return new Result(false, "Routine বানাতে অন্তত ২টি নিরাপদ ধাপ দরকার; পেয়েছি " + count + "টি। আবার রেকর্ড করুন।");
        }
        StringBuilder source = new StringBuilder();
        for (RecordedStep step : steps) {
            if (source.length() > 0) source.append(" তারপর ");
            source.append(step.command);
        }
        boolean saved = new RoutineRepository(context).save(name.trim(), source.toString());
        int count = steps.size();
        String warning = skippedReason.isEmpty() ? "" : " " + skippedReason;
        reset();
        return new Result(saved, saved
                ? "‘" + name.trim() + "’ Routine হিসেবে " + count + "টি ধাপ শিখেছি।" + warning
                : "Routine সংরক্ষণ করা যায়নি। Personal Memory পরীক্ষা করুন।");
    }

    public synchronized Result cancel() {
        if (!recording) return new Result(false, "Teach Mode এখন রেকর্ড করছে না।");
        recording = false;
        cancelNotification();
        reset();
        return new Result(true, "Teach Mode-এর বর্তমান রেকর্ড বাতিল করেছি।");
    }

    public synchronized void onAccessibilityEvent(AccessibilityEvent event) {
        if (!recording || event == null || steps.size() >= MAX_STEPS) return;
        String packageName = event.getPackageName() == null ? "" : event.getPackageName().toString();
        if (shouldIgnorePackage(packageName)) return;

        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            recordPackage(packageName);
        } else if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            recordClick(event, packageName);
        } else if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            recordTypedText(event);
        } else if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            recordScroll(event);
        }
    }

    private void recordPackage(String packageName) {
        if (!recording || shouldIgnorePackage(packageName) || packageName.equals(lastPackage)) return;
        lastPackage = packageName;
        String label = appLabel(packageName);
        if (label.isEmpty()) return;
        append(Kind.OPEN_APP, packageName, safe(label) + " খোলো", false);
    }

    private void recordClick(AccessibilityEvent event, String packageName) {
        AccessibilityNodeInfo node = event.getSource();
        if (event.isPassword() || (node != null && node.isPassword())) {
            markSensitiveSkip(); return;
        }
        String label = eventText(event);
        String viewId = node == null || node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName();
        if (label.isEmpty() && node != null) label = nodeLabel(node);
        if (label.isEmpty()) label = resourceTail(viewId);
        String hint = label + " " + viewId + " " + packageName;
        if (label.isEmpty() || SensitiveDataFilter.isSensitive("", hint)
                || SensitiveDataFilter.isProtectedAction(label)) {
            markSensitiveSkip(); return;
        }
        append(Kind.CLICK, viewId.isEmpty() ? BengaliText.normalize(label) : viewId,
                safe(label) + " চাপ দাও", false);
    }

    private void recordTypedText(AccessibilityEvent event) {
        AccessibilityNodeInfo node = event.getSource();
        String hint = node == null ? "" : nodeLabel(node) + " " + value(node.getViewIdResourceName());
        String value = eventText(event);
        if (event.isPassword() || (node != null && node.isPassword())
                || SensitiveDataFilter.isSensitive(value, hint)) {
            markSensitiveSkip(); return;
        }
        if (value.isEmpty()) return;
        String normalized = BengaliText.normalize(value);
        if (normalized.contains("তারপর") || normalized.contains("এরপর")
                || normalized.contains("তার পরে") || normalized.contains("এর পরে")) {
            skippedReason = "ধাপ-বিভাজক শব্দ থাকা একটি লেখা নিরাপদে রেকর্ড করা হয়নি।";
            return;
        }
        String key = node == null ? "focused" : value(node.getViewIdResourceName()) + "|" + value(node.getClassName());
        append(Kind.TYPE, key, "এখানে লিখো " + safe(value), true);
    }

    private void recordScroll(AccessibilityEvent event) {
        int delta = Build.VERSION.SDK_INT >= 28 ? event.getScrollDeltaY() : 0;
        boolean down = delta > 0 || (delta == 0 && event.getToIndex() > event.getFromIndex());
        append(Kind.SCROLL, down ? "down" : "up", down ? "নিচে স্ক্রল করো" : "উপরে স্ক্রল করো", false);
    }

    private void append(Kind kind, String key, String command, boolean replaceRecentType) {
        long now = System.currentTimeMillis();
        if (!steps.isEmpty()) {
            RecordedStep previous = steps.get(steps.size() - 1);
            if (replaceRecentType && previous.kind == Kind.TYPE && previous.key.equals(key) && now - lastRecordedAt < 4000) {
                steps.set(steps.size() - 1, new RecordedStep(kind, key, command));
                lastRecordedAt = now;
                showNotification();
                return;
            }
            if (previous.kind == kind && previous.key.equals(key) && now - lastRecordedAt < 900) return;
        }
        if (steps.size() < MAX_STEPS) steps.add(new RecordedStep(kind, key, command));
        lastRecordedAt = now;
        showNotification();
    }

    private String appLabel(String packageName) {
        if (appContext == null || packageName == null || packageName.isEmpty()) return "";
        try {
            PackageManager pm = appContext.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return value(pm.getApplicationLabel(info));
        } catch (Exception ignored) { return ""; }
    }

    private boolean shouldIgnorePackage(String packageName) {
        if (packageName == null || packageName.isEmpty() || packageName.equals(OWN_PACKAGE)
                || packageName.equals("com.android.systemui")) return true;
        if (appContext == null) return false;
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo info = appContext.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            return info != null && info.activityInfo != null && packageName.equals(info.activityInfo.packageName);
        } catch (Exception ignored) { return false; }
    }

    private String eventText(AccessibilityEvent event) {
        if (event.getText() != null) {
            for (CharSequence item : event.getText()) {
                String clean = value(item);
                if (!clean.isEmpty()) return clean;
            }
        }
        return value(event.getContentDescription());
    }

    private String nodeLabel(AccessibilityNodeInfo node) {
        String text = value(node.getText());
        return text.isEmpty() ? value(node.getContentDescription()) : text;
    }

    private String resourceTail(String viewId) {
        if (viewId == null || viewId.isEmpty()) return "";
        int slash = viewId.lastIndexOf('/');
        String tail = slash >= 0 ? viewId.substring(slash + 1) : viewId;
        return tail.replace('_', ' ').trim();
    }

    private String safe(String value) {
        return value(value).replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String value(Object value) { return value == null ? "" : value.toString().trim(); }

    private void markSensitiveSkip() {
        skippedReason = "Password, OTP, PIN বা protected action-এর ধাপ বাদ দেওয়া হয়েছে।";
    }

    private void showNotification() {
        if (appContext == null) return;
        try {
            NotificationManager manager = appContext.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    "Astra Sathi Teach Mode", NotificationManager.IMPORTANCE_LOW));
            Intent open = new Intent(appContext, MainActivity.class);
            PendingIntent pending = PendingIntent.getActivity(appContext, 2, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification notification = new Notification.Builder(appContext, CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("Teach Mode রেকর্ড করছে")
                    .setContentText(steps.size() + "টি নিরাপদ ধাপ • Password/OTP/PIN বাদ")
                    .setContentIntent(pending).setOngoing(true).setOnlyAlertOnce(true).build();
            manager.notify(NOTIFICATION_ID, notification);
        } catch (Exception ignored) { }
    }

    private void cancelNotification() {
        if (appContext == null) return;
        try { appContext.getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID); }
        catch (Exception ignored) { }
    }

    private void reset() {
        steps.clear();
        lastPackage = "";
        skippedReason = "";
        lastRecordedAt = 0;
        appContext = null;
    }
}
