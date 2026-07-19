package com.astratechnologies.astrasathi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "astra_sathi_reminders";
    private static final String ACTION_COMPLETE = "com.astratechnologies.astrasathi.REMINDER_COMPLETE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String memoryId = intent.getStringExtra("memory_id");
        if (ACTION_COMPLETE.equals(intent.getAction())) {
            if (memoryId != null) {
                SecureMemoryRepository memory = new SecureMemoryRepository(context);
                SecureMemoryRepository.MemoryRecord record = memory.find(memoryId);
                memory.markCompleted(memoryId);
                if (record != null && "প্রতিশ্রুতি".equals(record.type))
                    new LifeContextRepository(context).completeLatestCommitment();
                if (record != null && "যানবাহনের কাজ".equals(record.type))
                    new LifeContextRepository(context).completeLatestRefuelTask();
            }
            context.getSystemService(NotificationManager.class).cancel(memoryId == null ? 501 : memoryId.hashCode());
            return;
        }
        String text = intent.getStringExtra("text");
        SecureMemoryRepository.MemoryRecord current = new SecureMemoryRepository(context).find(memoryId);
        if (current != null && current.completed) return;
        if (text == null || text.isEmpty()) text = "আপনার একটি অসম্পূর্ণ কাজ রয়েছে।";
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Astra Sathi মনে করানো", NotificationManager.IMPORTANCE_HIGH));
        Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("show_pending_memory", true);
        PendingIntent pending = PendingIntent.getActivity(context, 501, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent complete = new Intent(context, ReminderReceiver.class).setAction(ACTION_COMPLETE)
                .putExtra("memory_id", memoryId);
        PendingIntent completePending = PendingIntent.getBroadcast(context,
                memoryId == null ? 502 : memoryId.hashCode() ^ 0x51A7, complete,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("আপনি কি কাজটি ভুলে গেছেন?")
                .setContentText(text)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
                .setContentIntent(pending).setAutoCancel(true)
                .addAction(new android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_launcher),
                        "সম্পন্ন", completePending).build()).build();
        manager.notify(memoryId == null ? 501 : memoryId.hashCode(), notification);

        int followUp = intent.getIntExtra("follow_up", 0);
        SecureMemoryRepository.MemoryRecord record = new SecureMemoryRepository(context).find(memoryId);
        if (record != null && !record.completed && followUp < 3)
            ReminderScheduler.scheduleFollowUp(context, record.id, record.content,
                    System.currentTimeMillis() + 30 * 60_000L, followUp + 1);
    }
}
