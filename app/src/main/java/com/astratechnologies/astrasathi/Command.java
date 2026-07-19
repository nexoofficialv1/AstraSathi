package com.astratechnologies.astrasathi;

public final class Command {
    public enum Type {
        CALL, SMS, WHATSAPP, ALARM, REMINDER, NOTE_SAVE, NOTE_SHOW,
        OPEN_APP, WEB_SEARCH, NAVIGATE, WEATHER, TORCH_ON, TORCH_OFF,
        HOME, BACK, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, LOCK_SCREEN,
        SCREENSHOT, CLICK_TEXT, CLICK_NUMBER, ACTIONS_LIST, TYPE_TEXT, READ_SCREEN, SCROLL_UP, SCROLL_DOWN,
        SWIPE_LEFT, SWIPE_RIGHT, VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE,
        MEDIA_PLAY_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS,
        WIFI_CONTROL, BLUETOOTH_CONTROL, LOCATION_SETTINGS, SECURITY_SETTINGS,
        PRIVACY_SETTINGS, DISPLAY_SETTINGS, SOUND_SETTINGS, APP_INFO, UNINSTALL_APP,
        BRIGHTNESS, SCREEN_TIMEOUT, AUTO_ROTATE_ON, AUTO_ROTATE_OFF,
        RINGER_NORMAL, RINGER_VIBRATE, RINGER_SILENT,
        NOTIFICATION_READ, NOTIFICATION_OPEN, NOTIFICATION_DISMISS, NOTIFICATION_REPLY,
        APP_NOTIFICATION_READ, APP_NOTIFICATION_OPEN, APP_NOTIFICATION_REPLY,
        CALL_ANSWER, CALL_END, CALL_WHO, CALL_SPEAKER_ON, CALL_SPEAKER_OFF,
        CALL_MUTE, CALL_UNMUTE,
        VEHICLE_FUEL_LOG, VEHICLE_DISTANCE_LOG, VEHICLE_MILEAGE_SET,
        VEHICLE_STATUS, VEHICLE_REFUEL_NEEDED,
        COMMITMENT_SAVE, COMMITMENT_LIST, COMMITMENT_COMPLETE,
        MEDICINE_LOG, SYMPTOM_LOG, HEALTH_STATUS, LIFE_STATUS, LIFE_CONTEXT_OPEN,
        MEMORY_SAVE, MEMORY_RECALL, MEMORY_PENDING, MEMORY_COMPLETE, MEMORY_FORGET_ALL,
        WORKFLOW, ROUTINE_SAVE, ROUTINE_RUN, ROUTINE_LIST,
        MACRO_START, MACRO_STOP, MACRO_CANCEL, TRADE_ORDER,
        FULL_ACCESS, HELP, UNKNOWN
    }

    public final Type type;
    public final String original;
    public final String target;
    public final String content;
    public final int hour;
    public final int minute;
    public final int dayOffset;

    private Command(Type type, String original, String target, String content,
                    int hour, int minute, int dayOffset) {
        this.type = type;
        this.original = original == null ? "" : original.trim();
        this.target = target == null ? "" : target.trim();
        this.content = content == null ? "" : content.trim();
        this.hour = hour;
        this.minute = minute;
        this.dayOffset = dayOffset;
    }

    public static Command of(Type type, String original, String target, String content) {
        return new Command(type, original, target, content, -1, -1, 0);
    }

    public static Command timed(Type type, String original, String content,
                                int hour, int minute, int dayOffset) {
        return new Command(type, original, "", content, hour, minute, dayOffset);
    }

    public boolean needsConfirmation() {
        return type == Type.CALL || type == Type.SMS || type == Type.WHATSAPP
                || type == Type.APP_NOTIFICATION_REPLY
                || type == Type.ALARM || type == Type.REMINDER || type == Type.TRADE_ORDER;
    }

    public boolean isProtectedUiAction() {
        if (type == Type.TYPE_TEXT || type == Type.CLICK_NUMBER
                || type == Type.NOTIFICATION_REPLY || type == Type.LOCK_SCREEN) return true;
        if (type != Type.CLICK_TEXT) return false;
        if (SensitiveDataFilter.isProtectedAction(target)) return true;
        String value = BengaliText.normalize(target);
        String[] protectedWords = {"পেমেন্ট", "পে", "টাকা", "ট্রান্সফার", "পাঠান", "পাঠাও",
                "মুছ", "ডিলিট", "আনইনস্টল", "ইনস্টল", "অনুমতি", "অ্যালাউ", "ওটিপি",
                "পাসওয়ার্ড", "পাসওয়ার্ড", "কনফার্ম", "নিশ্চিত"};
        for (String word : protectedWords) if (value.contains(word)) return true;
        return false;
    }
}
