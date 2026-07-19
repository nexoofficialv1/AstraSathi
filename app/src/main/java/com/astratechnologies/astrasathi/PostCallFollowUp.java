package com.astratechnologies.astrasathi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Visible post-call prompt; no call audio is recorded or secretly transcribed. */
public final class PostCallFollowUp {
    private static final String CHANNEL = "astra_sathi_post_call";

    private PostCallFollowUp() { }

    public static void post(Context context, String caller) {
        if (!new SecureMemoryRepository(context).isEnabled()) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "কথোপকথনের পর মনে রাখা", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("কলের পর ব্যবহারকারীর দেওয়া প্রতিশ্রুতি মনে রাখার ঐচ্ছিক prompt");
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("post_call_follow_up", true)
                .putExtra("caller", caller == null ? "" : caller);
        PendingIntent pending = PendingIntent.getActivity(context, 7104, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String who = caller == null || caller.isEmpty() ? "এই কলে" : caller + "-এর সঙ্গে কলে";
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("কোনো দেওয়া কথা মনে রাখব?")
                .setContentText(who + " কোনো কাজের প্রতিশ্রুতি দিলে ট্যাপ করে বলুন।")
                .setStyle(new Notification.BigTextStyle().bigText(who
                        + " কোনো কাজের প্রতিশ্রুতি দিয়ে থাকলে ট্যাপ করুন এবং বাংলায় সংক্ষেপে বলুন। কলের audio রেকর্ড করা হয়নি।"))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
        try { manager.notify(7104, notification); }
        catch (Exception ignored) { }
    }
}
