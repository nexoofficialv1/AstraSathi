package com.astratechnologies.astrasathi;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SathiNotificationService extends NotificationListenerService {
    private static volatile SathiNotificationService instance;
    private volatile StatusBarNotification latest;
    private final ArrayDeque<StatusBarNotification> history = new ArrayDeque<>();

    public static boolean isConnected() { return instance != null; }
    public static SathiNotificationService get() { return instance; }

    @Override public void onListenerConnected() {
        instance = this;
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) for (StatusBarNotification item : active) remember(item);
        } catch (Exception ignored) { }
    }
    @Override public void onListenerDisconnected() { if (instance == this) instance = null; }
    @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        remember(sbn);
    }

    public String describeLatest() {
        StatusBarNotification item = latest;
        if (item == null) return "কোনো সাম্প্রতিক নোটিফিকেশন পাওয়া যায়নি।";
        Bundle extras = item.getNotification().extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        StringBuilder out = new StringBuilder("সর্বশেষ নোটিফিকেশন");
        if (title != null && title.length() > 0) out.append(", ").append(title);
        if (text != null && text.length() > 0) out.append("। ").append(text);
        return out.toString();
    }

    public boolean openLatest() {
        StatusBarNotification item = latest;
        if (item == null || item.getNotification().contentIntent == null) return false;
        try { item.getNotification().contentIntent.send(); return true; }
        catch (PendingIntent.CanceledException e) { return false; }
    }

    public boolean dismissLatest() {
        StatusBarNotification item = latest;
        if (item == null || !item.isClearable()) return false;
        cancelNotification(item.getKey()); return true;
    }

    public boolean replyLatest(String replyText) {
        return reply(latest, replyText);
    }

    public String describeRecentFromPackages(int limit, String... packages) {
        List<StatusBarNotification> items = recentFromPackages(Math.max(1, Math.min(8, limit)), packages);
        if (items.isEmpty()) return "এই app-এর সাম্প্রতিক কোনো message notification পাইনি।";
        StringBuilder out = new StringBuilder();
        for (StatusBarNotification item : items) {
            String description = describe(item);
            if (description.isEmpty()) continue;
            if (out.length() > 0) out.append("। ");
            out.append(description);
        }
        return out.length() == 0 ? "Message notification-এ পড়ার মতো লেখা পাইনি।" : out.toString();
    }

    public boolean openLatestFromPackages(String... packages) {
        StatusBarNotification item = latestFromPackages(packages);
        if (item == null || item.getNotification().contentIntent == null) return false;
        try { item.getNotification().contentIntent.send(); return true; }
        catch (PendingIntent.CanceledException e) { return false; }
    }

    public boolean replyLatestFromPackages(String replyText, String... packages) {
        return reply(latestFromPackages(packages), replyText);
    }

    private boolean reply(StatusBarNotification item, String replyText) {
        if (item == null || replyText == null || replyText.trim().isEmpty()) return false;
        Notification.Action[] actions = item.getNotification().actions;
        if (actions == null) return false;
        for (Notification.Action action : actions) {
            RemoteInput[] inputs = action.getRemoteInputs();
            if (inputs == null || inputs.length == 0) continue;
            Intent intent = new Intent();
            Bundle results = new Bundle();
            for (RemoteInput input : inputs) results.putCharSequence(input.getResultKey(), replyText);
            RemoteInput.addResultsToIntent(inputs, intent, results);
            try { action.actionIntent.send(this, 0, intent); return true; }
            catch (PendingIntent.CanceledException ignored) { }
        }
        return false;
    }

    private synchronized void remember(StatusBarNotification sbn) {
        if (sbn == null || getPackageName().equals(sbn.getPackageName())) return;
        latest = sbn;
        history.removeIf(item -> item.getKey().equals(sbn.getKey()));
        history.addFirst(sbn);
        while (history.size() > 60) history.removeLast();
    }

    private synchronized StatusBarNotification latestFromPackages(String... packages) {
        Set<String> accepted = packageSet(packages);
        for (StatusBarNotification item : history)
            if (accepted.contains(item.getPackageName())) return item;
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                StatusBarNotification selected = null;
                for (StatusBarNotification item : active) if (accepted.contains(item.getPackageName())
                        && (selected == null || item.getPostTime() > selected.getPostTime())) selected = item;
                return selected;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private synchronized List<StatusBarNotification> recentFromPackages(int limit, String... packages) {
        Set<String> accepted = packageSet(packages);
        List<StatusBarNotification> result = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        for (StatusBarNotification item : history) {
            if (!accepted.contains(item.getPackageName())) continue;
            String description = describe(item);
            if (!description.isEmpty() && seenText.add(description)) result.add(item);
            if (result.size() >= limit) break;
        }
        return result;
    }

    private Set<String> packageSet(String... packages) {
        Set<String> result = new HashSet<>();
        if (packages != null) for (String value : packages) if (value != null) result.add(value);
        return result;
    }

    private String describe(StatusBarNotification item) {
        Bundle extras = item.getNotification().extras;
        String title = clean(extras.getCharSequence(Notification.EXTRA_TITLE));
        String text = clean(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (text.isEmpty()) text = clean(extras.getCharSequence(Notification.EXTRA_TEXT));
        if (title.isEmpty() && text.isEmpty()) return "";
        return title.isEmpty() ? text : text.isEmpty() ? title : title + " বলেছেন, " + text;
    }

    private String clean(CharSequence value) {
        return value == null ? "" : value.toString().replaceAll("\\s+", " ").trim();
    }
}
