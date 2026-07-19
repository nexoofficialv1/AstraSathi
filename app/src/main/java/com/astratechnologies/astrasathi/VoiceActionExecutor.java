package com.astratechnologies.astrasathi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;

import java.util.Calendar;
import java.util.List;

public final class VoiceActionExecutor {
    private VoiceActionExecutor() { }

    public static DeviceController.Result execute(Context context, Command command) {
        if (isDeviceCommand(command.type)) return DeviceController.execute(context, command);
        try {
            return switch (command.type) {
                case CALL, SMS, WHATSAPP -> contact(context, command);
                case ALARM -> alarm(context, command);
                case REMINDER -> reminder(context, command);
                case NOTE_SAVE -> note(context, command);
                case NOTE_SHOW -> notes(context);
                case MEMORY_SAVE -> remember(context, command);
                case MEMORY_RECALL -> recall(context, false);
                case MEMORY_PENDING -> recall(context, true);
                case MEMORY_COMPLETE -> completeMemory(context);
                case MACRO_START -> teachStart(context);
                case MACRO_STOP -> teachStop(context, command.target);
                case MACRO_CANCEL -> teachCancel();
                case TRADE_ORDER -> open(context, new Intent(context, FinancialActionActivity.class)
                        .putExtra("trade_command", command.original), "Financial order preview খুলেছি।");
                case VEHICLE_FUEL_LOG, VEHICLE_DISTANCE_LOG, VEHICLE_MILEAGE_SET,
                        VEHICLE_STATUS, VEHICLE_REFUEL_NEEDED, COMMITMENT_SAVE,
                        COMMITMENT_LIST, COMMITMENT_COMPLETE, MEDICINE_LOG,
                        SYMPTOM_LOG, HEALTH_STATUS, LIFE_STATUS, LIFE_CONTEXT_OPEN ->
                        LifeContextEngine.execute(context, command);
                case OPEN_APP -> DeviceController.openApp(context, command.target);
                case WEB_SEARCH -> open(context, new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(command.content))), "তথ্য খুঁজছি।");
                case WEATHER -> open(context, new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode((command.target.isEmpty() ? "আমার এলাকার" : command.target) + " আজকের আবহাওয়া"))), "আবহাওয়া খুঁজছি।");
                case NAVIGATE -> open(context, new Intent(Intent.ACTION_VIEW,
                        Uri.parse("google.navigation:q=" + Uri.encode(command.target))).setPackage("com.google.android.apps.maps"), "দিকনির্দেশ খুলেছি।");
                case FULL_ACCESS -> open(context, new Intent(context, FullAccessActivity.class), "Full Access Setup খুলেছি।");
                case HELP -> new DeviceController.Result(true, "বলতে পারেন—স্ক্রিন পড়ো, WhatsApp খোলো, মাকে ফোন করো, অ্যালার্ম দাও, গাড়ির তেলের অবস্থা বলো, দেওয়া কথা দেখাও অথবা আমার বর্তমান পরিস্থিতি কেমন।");
                default -> new DeviceController.Result(false, "এই command-টি background voice mode-এ এখনও কার্যকর নয়।");
            };
        } catch (Exception e) { return new DeviceController.Result(false, "কাজটি এখন করা গেল না। প্রয়োজনীয় permission পরীক্ষা করুন।"); }
    }

    public static String describe(Command command) {
        return switch (command.type) {
            case CALL -> command.target + "-কে ফোনের dialer খুলব";
            case SMS -> command.target + "-কে SMS প্রস্তুত করব";
            case WHATSAPP -> command.target + "-কে WhatsApp message প্রস্তুত করব";
            case REMINDER -> "রিমাইন্ডার তৈরি করব";
            case ALARM -> "অ্যালার্ম তৈরি করব";
            case TYPE_TEXT -> "বর্তমান ঘরে ‘" + command.content + "’ লিখব";
            case CLICK_TEXT -> "‘" + command.target + "’-এ চাপ দেব";
            case CLICK_NUMBER -> command.target + " নম্বর control-এ চাপ দেব";
            case NOTIFICATION_REPLY -> "সর্বশেষ message-এর উত্তর পাঠাব";
            case APP_NOTIFICATION_REPLY -> command.target + " message-এর উত্তর পাঠাব";
            case CALL_ANSWER -> "incoming call ধরব";
            case CALL_END -> "active call শেষ করব";
            case UNINSTALL_APP -> command.target + " app uninstall screen খুলব";
            case LOCK_SCREEN -> "ফোন lock করব";
            default -> command.original;
        };
    }

    private static boolean isDeviceCommand(Command.Type type) {
        return switch (type) {
            case HOME, BACK, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, LOCK_SCREEN, SCREENSHOT,
                    CLICK_TEXT, CLICK_NUMBER, ACTIONS_LIST, TYPE_TEXT, READ_SCREEN, SCROLL_UP, SCROLL_DOWN, SWIPE_LEFT, SWIPE_RIGHT,
                    VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE, MEDIA_PLAY_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS,
                    NOTIFICATION_READ, NOTIFICATION_OPEN, NOTIFICATION_DISMISS, NOTIFICATION_REPLY,
                    APP_NOTIFICATION_READ, APP_NOTIFICATION_OPEN, APP_NOTIFICATION_REPLY,
                    CALL_ANSWER, CALL_END, CALL_WHO, CALL_SPEAKER_ON, CALL_SPEAKER_OFF,
                    CALL_MUTE, CALL_UNMUTE,
                    OPEN_APP, TORCH_ON, TORCH_OFF, WIFI_CONTROL, BLUETOOTH_CONTROL, LOCATION_SETTINGS,
                    SECURITY_SETTINGS, PRIVACY_SETTINGS, DISPLAY_SETTINGS, SOUND_SETTINGS, APP_INFO,
                    UNINSTALL_APP, BRIGHTNESS, SCREEN_TIMEOUT, AUTO_ROTATE_ON, AUTO_ROTATE_OFF,
                    RINGER_NORMAL, RINGER_VIBRATE, RINGER_SILENT -> true;
            default -> false;
        };
    }

    private static DeviceController.Result teachStart(Context context) {
        SathiAccessibilityService service = SathiAccessibilityService.get();
        MacroRecorder.Result result = MacroRecorder.get().start(context,
                service == null ? "" : service.getCurrentPackageName());
        return new DeviceController.Result(result.success, result.message);
    }

    private static DeviceController.Result teachStop(Context context, String name) {
        MacroRecorder.Result result = MacroRecorder.get().stopAndSave(context, name);
        return new DeviceController.Result(result.success, result.message);
    }

    private static DeviceController.Result teachCancel() {
        MacroRecorder.Result result = MacroRecorder.get().cancel();
        return new DeviceController.Result(result.success, result.message);
    }

    private static DeviceController.Result contact(Context context, Command command) {
        boolean direct = BengaliText.toAsciiDigits(command.target).replaceAll("[^0-9]", "").length() >= 7;
        if (!direct && context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return new DeviceController.Result(false, "Contacts permission প্রয়োজন।");
        String number = new ContactResolver(context).resolvePhone(command.target);
        if (number.isEmpty()) return new DeviceController.Result(false, command.target + "-এর নম্বর খুঁজে পাইনি।");
        number = number.replaceAll("[^0-9+]", "");
        Intent intent;
        if (command.type == Command.Type.CALL) intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)));
        else if (command.type == Command.Type.SMS) {
            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))).putExtra("sms_body", command.content);
        } else return WhatsAppAutomation.sendApproved(context, number, command.content);
        return open(context, intent, "কাজটি সংশ্লিষ্ট app-এ প্রস্তুত করেছি।");
    }

    private static DeviceController.Result alarm(Context context, Command command) {
        if (command.hour < 0) return new DeviceController.Result(false, "অ্যালার্মের সময় বুঝতে পারিনি।");
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, command.hour).putExtra(AlarmClock.EXTRA_MINUTES, command.minute)
                .putExtra(AlarmClock.EXTRA_MESSAGE, command.content).putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        return open(context, intent, "অ্যালার্ম প্রস্তুত করেছি।");
    }

    private static DeviceController.Result reminder(Context context, Command command) {
        if (command.hour < 0) return new DeviceController.Result(false, "রিমাইন্ডারের সময় বুঝতে পারিনি।");
        Calendar begin = Calendar.getInstance(); begin.add(Calendar.DAY_OF_YEAR, command.dayOffset);
        begin.set(Calendar.HOUR_OF_DAY, command.hour); begin.set(Calendar.MINUTE, command.minute); begin.set(Calendar.SECOND, 0);
        SecureMemoryRepository memory = new SecureMemoryRepository(context);
        SecureMemoryRepository.MemoryRecord saved = memory.rememberRecord("অসম্পূর্ণ কাজ", command.content, begin.getTimeInMillis());
        if (saved != null) ReminderScheduler.schedule(context, saved.id, saved.content, saved.remindAt);
        Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin.getTimeInMillis())
                .putExtra(CalendarContract.Events.TITLE, command.content);
        return open(context, intent, "রিমাইন্ডার প্রস্তুত করেছি।");
    }

    private static DeviceController.Result note(Context context, Command command) {
        if (command.content.isEmpty()) return new DeviceController.Result(false, "নোটের লেখা বলুন।");
        new NotesRepository(context).add(command.content);
        return new DeviceController.Result(true, "নোটটি লিখে রেখেছি।");
    }

    private static DeviceController.Result notes(Context context) {
        List<String> notes = new NotesRepository(context).list(5);
        return new DeviceController.Result(true, notes.isEmpty() ? "কোনো নোট নেই।" : String.join("। ", notes));
    }

    private static DeviceController.Result remember(Context context, Command command) {
        SecureMemoryRepository memory = new SecureMemoryRepository(context);
        if (!memory.isEnabled()) return new DeviceController.Result(false, "Personal Memory বন্ধ আছে।");
        boolean saved = memory.remember("ব্যক্তিগত তথ্য", command.content, 0);
        return new DeviceController.Result(saved, saved ? "মনে রেখেছি।" : "সংবেদনশীল তথ্য Personal Memory-তে রাখা হয় না।");
    }

    private static DeviceController.Result recall(Context context, boolean pendingOnly) {
        SecureMemoryRepository memory = new SecureMemoryRepository(context);
        List<SecureMemoryRepository.MemoryRecord> records = pendingOnly
                ? memory.pending(System.currentTimeMillis()) : memory.recent(8);
        if (records.isEmpty()) return new DeviceController.Result(true,
                pendingOnly ? "সময় পেরিয়ে যাওয়া কোনো অসম্পূর্ণ কাজ নেই।" : "এখনও কোনো স্মৃতি নেই।");
        StringBuilder out = new StringBuilder();
        for (SecureMemoryRepository.MemoryRecord record : records) {
            if (out.length() > 0) out.append("। "); out.append(record.content);
        }
        return new DeviceController.Result(true, out.toString());
    }

    private static DeviceController.Result completeMemory(Context context) {
        boolean completed = new SecureMemoryRepository(context).completeLatestTask();
        return new DeviceController.Result(completed,
                completed ? "সর্বশেষ pending কাজটি সম্পন্ন হিসেবে রেখেছি।" : "কোনো pending কাজ পাইনি।");
    }

    private static DeviceController.Result open(Context context, Intent intent, String message) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return new DeviceController.Result(true, message);
    }
}
