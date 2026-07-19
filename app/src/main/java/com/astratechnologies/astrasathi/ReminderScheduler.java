package com.astratechnologies.astrasathi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ReminderScheduler {
    private ReminderScheduler() { }

    public static void schedule(Context context, String id, String text, long when) {
        scheduleInternal(context, id, text, when, 0);
    }

    public static void scheduleFollowUp(Context context, String id, String text, long when, int followUp) {
        scheduleInternal(context, id, text, when, followUp);
    }

    private static void scheduleInternal(Context context, String id, String text, long when, int followUp) {
        if (when <= 0) return;
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra("memory_id", id).putExtra("text", text).putExtra("follow_up", followUp);
        PendingIntent pending = PendingIntent.getBroadcast(context, id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms())
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
    }

    public static void rescheduleAll(Context context) {
        SecureMemoryRepository memory = new SecureMemoryRepository(context);
        long now = System.currentTimeMillis();
        for (SecureMemoryRepository.MemoryRecord record : memory.scheduled())
            schedule(context, record.id, record.content, Math.max(record.remindAt, now + 5000));
    }
}
