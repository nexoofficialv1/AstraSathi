package com.astratechnologies.astrasathi;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.KeyEvent;

import java.util.List;

public final class DeviceController {
    public static final class Result {
        public final boolean success;
        public final String message;
        Result(boolean success, String message) { this.success = success; this.message = message; }
    }

    private DeviceController() { }

    public static Result execute(Context context, Command command) {
        SathiAccessibilityService service = SathiAccessibilityService.get();
        switch (command.type) {
            case VOLUME_UP -> { return volume(context, AudioManager.ADJUST_RAISE, "ভলিউম বাড়িয়েছি।"); }
            case VOLUME_DOWN -> { return volume(context, AudioManager.ADJUST_LOWER, "ভলিউম কমিয়েছি।"); }
            case VOLUME_MUTE -> { return volume(context, AudioManager.ADJUST_TOGGLE_MUTE, "ভলিউম mute পরিবর্তন করেছি।"); }
            case MEDIA_PLAY_PAUSE -> { return media(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Play/Pause করা হয়েছে।"); }
            case MEDIA_NEXT -> { return media(context, KeyEvent.KEYCODE_MEDIA_NEXT, "পরের media চালু করেছি।"); }
            case MEDIA_PREVIOUS -> { return media(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS, "আগের media চালু করেছি।"); }
            case RINGER_NORMAL -> { return ringer(context, AudioManager.RINGER_MODE_NORMAL, "Ring mode চালু করেছি।"); }
            case RINGER_VIBRATE -> { return ringer(context, AudioManager.RINGER_MODE_VIBRATE, "Vibrate mode চালু করেছি।"); }
            case RINGER_SILENT -> { return ringer(context, AudioManager.RINGER_MODE_SILENT, "Silent mode চালু করেছি।"); }
            case WIFI_CONTROL -> { return openSetting(context, new Intent(Settings.Panel.ACTION_WIFI), "Wi-Fi control panel খুলেছি।"); }
            case BLUETOOTH_CONTROL -> { return openSetting(context, new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth settings খুলেছি।"); }
            case LOCATION_SETTINGS -> { return openSetting(context, new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), "Location settings খুলেছি।"); }
            case SECURITY_SETTINGS -> { return openSetting(context, new Intent(Settings.ACTION_SECURITY_SETTINGS), "Security settings খুলেছি। Android চাইলে বর্তমান PIN/biometric দিন।"); }
            case PRIVACY_SETTINGS -> { return openSetting(context, new Intent(Settings.ACTION_PRIVACY_SETTINGS), "Privacy settings খুলেছি।"); }
            case DISPLAY_SETTINGS -> { return openSetting(context, new Intent(Settings.ACTION_DISPLAY_SETTINGS), "Display settings খুলেছি।"); }
            case SOUND_SETTINGS -> { return openSetting(context, new Intent(Settings.ACTION_SOUND_SETTINGS), "Sound settings খুলেছি।"); }
            case BRIGHTNESS -> { return brightness(context, command.target); }
            case SCREEN_TIMEOUT -> { return screenTimeout(context, command.target, command.content); }
            case AUTO_ROTATE_ON -> { return autoRotate(context, true); }
            case AUTO_ROTATE_OFF -> { return autoRotate(context, false); }
            case APP_INFO -> { return appSettings(context, command.target, false); }
            case UNINSTALL_APP -> { return appSettings(context, command.target, true); }
            case NOTIFICATION_READ, NOTIFICATION_OPEN, NOTIFICATION_DISMISS, NOTIFICATION_REPLY,
                    APP_NOTIFICATION_READ, APP_NOTIFICATION_OPEN, APP_NOTIFICATION_REPLY -> {
                return notification(command);
            }
            case CALL_ANSWER, CALL_END, CALL_WHO, CALL_SPEAKER_ON, CALL_SPEAKER_OFF,
                    CALL_MUTE, CALL_UNMUTE -> { return CallController.execute(context, command.type); }
            case OPEN_APP -> { return openApp(context, command.target); }
            case TORCH_ON -> { return torch(context, true); }
            case TORCH_OFF -> { return torch(context, false); }
            default -> { }
        }
        if (service == null) return new Result(false, "Screen Control access চালু নেই। Full Access Setup থেকে Accessibility চালু করুন।");
        boolean ok;
        String message;
        switch (command.type) {
            case HOME -> { ok = service.global(AccessibilityService.GLOBAL_ACTION_HOME); message = "Home screen-এ যাচ্ছি।"; }
            case BACK -> { ok = service.global(AccessibilityService.GLOBAL_ACTION_BACK); message = "পেছনের screen-এ যাচ্ছি।"; }
            case RECENTS -> { ok = service.global(AccessibilityService.GLOBAL_ACTION_RECENTS); message = "সাম্প্রতিক apps দেখাচ্ছি।"; }
            case NOTIFICATIONS -> { ok = service.global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS); message = "Notification panel খুলেছি।"; }
            case QUICK_SETTINGS -> { ok = service.global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS); message = "Quick Settings খুলেছি।"; }
            case LOCK_SCREEN -> { ok = service.global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN); message = "ফোন lock করেছি।"; }
            case SCREENSHOT -> { ok = service.global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT); message = "Screenshot নেওয়ার অনুরোধ করেছি।"; }
            case CLICK_TEXT -> { ok = service.clickByText(command.target); message = ok ? "‘" + command.target + "’-এ চাপ দিয়েছি।" : "স্ক্রিনে ‘" + command.target + "’ খুঁজে পাইনি।"; }
            case CLICK_NUMBER -> {
                try {
                    int number = Integer.parseInt(command.target);
                    ok = service.clickActionNumber(number);
                    message = ok ? number + " নম্বর control-এ চাপ দিয়েছি।"
                            : "নম্বরটি বর্তমান screen snapshot-এর সঙ্গে মিলছে না। আবার ‘স্ক্রিনের বোতামগুলো দেখাও’ বলুন।";
                } catch (NumberFormatException e) { ok = false; message = "Control-এর নম্বরটি বুঝতে পারিনি।"; }
            }
            case ACTIONS_LIST -> { return new Result(true, service.describeActionableElements()); }
            case TYPE_TEXT -> { ok = service.typeIntoFocusedField(command.content); message = ok ? "লেখাটি টাইপ করেছি।" : "লেখার উপযুক্ত ঘর খুঁজে পাইনি।"; }
            case READ_SCREEN -> { return new Result(true, service.readVisibleText()); }
            case SCROLL_DOWN -> { ok = service.scroll(true); message = "নিচে scroll করেছি।"; }
            case SCROLL_UP -> { ok = service.scroll(false); message = "উপরে scroll করেছি।"; }
            case SWIPE_LEFT -> { ok = service.swipe(2); message = "বামে swipe করেছি।"; }
            case SWIPE_RIGHT -> { ok = service.swipe(3); message = "ডানে swipe করেছি।"; }
            default -> { return new Result(false, "এই device command-টি এখনও যুক্ত হয়নি।"); }
        }
        if (ok) return new Result(true, message);
        if (command.type == Command.Type.CLICK_TEXT || command.type == Command.Type.CLICK_NUMBER
                || command.type == Command.Type.TYPE_TEXT) return new Result(false, message);
        return new Result(false, "কাজটি করা যায়নি। বর্তমান screen সম্ভবত action-টি অনুমতি দেয়নি।");
    }

    private static Result volume(Context context, int direction, String message) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
        return new Result(true, message);
    }

    private static Result media(Context context, int keyCode, String message) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        return new Result(true, message);
    }

    private static Result ringer(Context context, int mode, String message) {
        try {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audio.setRingerMode(mode);
            return new Result(true, message);
        } catch (SecurityException e) {
            return new Result(false, "Do Not Disturb access প্রয়োজন। Full Access Setup থেকে চালু করুন।");
        }
    }

    private static Result brightness(Context context, String rawPercent) {
        if (!Settings.System.canWrite(context)) return new Result(false, "Modify system settings access চালু নেই।");
        try {
            int percent = Math.max(1, Math.min(100, Integer.parseInt(rawPercent)));
            int value = Math.round(255f * percent / 100f);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
            return new Result(true, "Brightness " + percent + " শতাংশ করেছি।");
        } catch (Exception e) { return new Result(false, "Brightness-এর মানটি বুঝতে পারিনি।"); }
    }

    private static Result screenTimeout(Context context, String amount, String unit) {
        if (!Settings.System.canWrite(context)) return new Result(false, "Modify system settings access চালু নেই।");
        try {
            long value = Long.parseLong(amount);
            long millis = "সেকেন্ড".equals(unit) ? value * 1000 : value * 60_000;
            millis = Math.max(15_000, Math.min(30 * 60_000, millis));
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, (int) millis);
            return new Result(true, "Screen timeout পরিবর্তন করেছি।");
        } catch (Exception e) { return new Result(false, "Screen timeout-এর সময়টি বুঝতে পারিনি।"); }
    }

    private static Result autoRotate(Context context, boolean enabled) {
        if (!Settings.System.canWrite(context)) return new Result(false, "Modify system settings access চালু নেই।");
        boolean ok = Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
        return new Result(ok, ok ? (enabled ? "Auto rotate চালু করেছি।" : "Auto rotate বন্ধ করেছি।") : "Auto rotate পরিবর্তন করা গেল না।");
    }

    private static Result openSetting(Context context, Intent intent, String message) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return new Result(true, message);
        } catch (Exception e) { return new Result(false, "এই settings screen ফোনে খোলা গেল না।"); }
    }

    private static Result appSettings(Context context, String target, boolean uninstall) {
        String packageName = findPackage(context, target);
        if (packageName == null) return new Result(false, "‘" + target + "’ app খুঁজে পাইনি।");
        Intent intent = new Intent(uninstall ? Intent.ACTION_DELETE : Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return openSetting(context, intent, uninstall
                ? "Android uninstall confirmation খুলেছি। নিশ্চিত করলে app মুছে যাবে।"
                : target + " app-এর settings খুলেছি।");
    }

    private static String findPackage(Context context, String target) {
        String known = knownSocialPackage(context, target);
        if (known != null) return known;
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        String wanted = BengaliText.normalize(target);
        for (ResolveInfo app : pm.queryIntentActivities(query, 0)) {
            String label = BengaliText.normalize(app.loadLabel(pm).toString());
            if (label.equals(wanted) || label.contains(wanted) || wanted.contains(label))
                return app.activityInfo.packageName;
        }
        return null;
    }

    private static Result notification(Command command) {
        SathiNotificationService service = SathiNotificationService.get();
        if (service == null) return new Result(false, "Notification access চালু নেই।");
        return switch (command.type) {
            case NOTIFICATION_READ -> new Result(true, service.describeLatest());
            case NOTIFICATION_OPEN -> result(service.openLatest(), "সর্বশেষ notification খুলেছি।", "Notification-টি খোলা গেল না।");
            case NOTIFICATION_DISMISS -> result(service.dismissLatest(), "Notification সরিয়েছি।", "Notification-টি সরানো গেল না।");
            case NOTIFICATION_REPLY -> result(service.replyLatest(command.content), "উত্তর পাঠানো হয়েছে।", "এই notification-এ quick reply নেই।");
            case APP_NOTIFICATION_READ -> new Result(true,
                    service.describeRecentFromPackages(5, socialPackages(command.target)));
            case APP_NOTIFICATION_OPEN -> result(service.openLatestFromPackages(socialPackages(command.target)),
                    socialLabel(command.target) + "-এর সর্বশেষ notification খুলেছি।",
                    socialLabel(command.target) + "-এর notification খোলা গেল না।");
            case APP_NOTIFICATION_REPLY -> result(service.replyLatestFromPackages(command.content,
                            socialPackages(command.target)),
                    socialLabel(command.target) + " message-এর উত্তর পাঠিয়েছি।",
                    "এই " + socialLabel(command.target) + " notification-এ quick reply পাওয়া যায়নি।");
            default -> new Result(false, "Notification command পাওয়া যায়নি।");
        };
    }

    private static String[] socialPackages(String target) {
        return switch (BengaliText.normalize(target)) {
            case "whatsapp" -> new String[]{"com.whatsapp", "com.whatsapp.w4b"};
            case "messenger" -> new String[]{"com.facebook.orca", "com.facebook.mlite"};
            default -> new String[]{"com.facebook.katana", "com.facebook.lite"};
        };
    }

    private static String socialLabel(String target) {
        return switch (BengaliText.normalize(target)) {
            case "whatsapp" -> "WhatsApp";
            case "messenger" -> "Messenger";
            default -> "Facebook";
        };
    }

    public static Result openApp(Context context, String target) {
        PackageManager pm = context.getPackageManager();
        String known = knownSocialPackage(context, target);
        if (known != null) {
            Intent launch = pm.getLaunchIntentForPackage(known);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(launch);
                return new Result(true, appLabel(context, known) + " খুলেছি।");
            }
        }
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        String wanted = BengaliText.normalize(target);
        for (ResolveInfo app : apps) {
            String label = BengaliText.normalize(app.loadLabel(pm).toString());
            if (label.equals(wanted) || label.contains(wanted) || wanted.contains(label)) {
                Intent launch = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
                if (launch == null) continue;
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launch);
                return new Result(true, app.loadLabel(pm) + " খুলেছি।");
            }
        }
        return new Result(false, "‘" + target + "’ নামে installed app খুঁজে পাইনি।");
    }

    private static String knownSocialPackage(Context context, String target) {
        String wanted = BengaliText.normalize(target);
        String[] candidates = null;
        if (wanted.contains("হোয়াট") || wanted.contains("হোয়াট") || wanted.contains("whatsapp"))
            candidates = new String[]{"com.whatsapp", "com.whatsapp.w4b"};
        else if (wanted.contains("ফেসবুক") || wanted.contains("facebook"))
            candidates = new String[]{"com.facebook.katana", "com.facebook.lite"};
        else if (wanted.contains("মেসেঞ্জার") || wanted.contains("messenger"))
            candidates = new String[]{"com.facebook.orca", "com.facebook.mlite"};
        if (candidates == null) return null;
        PackageManager pm = context.getPackageManager();
        for (String candidate : candidates) if (pm.getLaunchIntentForPackage(candidate) != null) return candidate;
        return null;
    }

    private static String appLabel(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) { return packageName; }
    }

    private static Result torch(Context context, boolean enabled) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                Boolean flash = manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) {
                    manager.setTorchMode(id, enabled);
                    return new Result(true, enabled ? "টর্চ চালু করেছি।" : "টর্চ বন্ধ করেছি।");
                }
            }
        } catch (CameraAccessException | SecurityException ignored) { }
        return new Result(false, "টর্চ নিয়ন্ত্রণ করা গেল না। Camera permission পরীক্ষা করুন।");
    }

    private static Result result(boolean ok, String success, String failure) {
        return new Result(ok, ok ? success : failure);
    }
}
