package com.astratechnologies.astrasathi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

public final class WhatsAppAutomation {
    private static final String CHANNEL = "astra_sathi_app_actions";
    private static final int NOTIFICATION_ID = 7004;
    private WhatsAppAutomation() { }

    public static DeviceController.Result sendApproved(Context context, String rawNumber, String message) {
        String number = rawNumber == null ? "" : rawNumber.replaceAll("[^0-9]", "");
        if (number.length() == 10) number = "91" + number;
        if (number.length() < 7) return new DeviceController.Result(false, "WhatsApp-এর জন্য সঠিক নম্বর পাইনি।");
        String text = message == null ? "" : message.trim();
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + number + "?text=" + Uri.encode(text)))
                .setPackage("com.whatsapp").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception primary) {
            try { intent.setPackage("com.whatsapp.w4b"); context.startActivity(intent); }
            catch (Exception business) { return new DeviceController.Result(false, "WhatsApp বা WhatsApp Business খোলা গেল না।"); }
        }
        if (text.isEmpty()) return new DeviceController.Result(true, "WhatsApp chat খুলেছি। Message লিখে পাঠাতে পারবেন।");
        if (!SathiAccessibilityService.isConnected()) return new DeviceController.Result(true,
                "WhatsApp-এ message প্রস্তুত করেছি। Screen Control বন্ধ থাকায় Send নিজে চাপুন।");
        Context app = context.getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            int attempts;
            @Override public void run() {
                SathiAccessibilityService service = SathiAccessibilityService.get();
                if (service != null && service.clickByExactText("Send", "পাঠান", "পাঠাও")) {
                    postResult(app, "WhatsApp message", "অনুমোদিত message-এর Send control চাপা হয়েছে।");
                    return;
                }
                if (service != null) service.clickByText("Continue to chat");
                if (++attempts < 24) handler.postDelayed(this, 450);
                else postResult(app, "WhatsApp message প্রস্তুত",
                        "Send control পাওয়া যায়নি। WhatsApp খুলে message পরীক্ষা করুন।");
            }
        }, 1100);
        return new DeviceController.Result(true,
                "WhatsApp chat খুলেছি। অনুমোদিত message-এর Send control খুঁজছি; ফল notification-এ জানাব।");
    }

    private static void postResult(Context context, String title, String text) {
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    "Astra Sathi app actions", NotificationManager.IMPORTANCE_DEFAULT));
            manager.notify(NOTIFICATION_ID, new Notification.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher).setContentTitle(title).setContentText(text)
                    .setAutoCancel(true).build());
        } catch (Exception ignored) { }
    }
}
