package com.astratechnologies.astrasathi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CallController {
    private CallController() { }

    public static DeviceController.Result execute(Context context, Command.Type type) {
        SathiInCallService service = SathiInCallService.get();
        if (type == Command.Type.CALL_WHO)
            return new DeviceController.Result(true, describeCaller(context));
        if (service == null || !service.hasCurrentCall())
            return new DeviceController.Result(false,
                    "কোনো active call নেই। Call control-এর জন্য Astra Sathi-কে Default Phone app করুন।");
        return switch (type) {
            case CALL_ANSWER -> result(service.answerCurrent(), "কলটি ধরেছি।", "কলটি এখন ধরা গেল না।");
            case CALL_END -> result(service.endCurrent(), "কলটি শেষ করেছি।", "কলটি শেষ করা গেল না।");
            case CALL_SPEAKER_ON -> result(service.setSpeaker(true), "Call speaker চালু করেছি।", "Speaker চালু করা গেল না।");
            case CALL_SPEAKER_OFF -> result(service.setSpeaker(false), "Call speaker বন্ধ করেছি।", "Speaker বন্ধ করা গেল না।");
            case CALL_MUTE -> result(service.setCallMuted(true), "Call microphone mute করেছি।", "Mute করা গেল না।");
            case CALL_UNMUTE -> result(service.setCallMuted(false), "Call microphone চালু করেছি।", "Unmute করা গেল না।");
            default -> new DeviceController.Result(false, "Call command পাওয়া যায়নি।");
        };
    }

    public static String describeCaller(Context context) {
        SathiInCallService service = SathiInCallService.get();
        if (service != null && service.hasCurrentCall()) return service.getCurrentCallerDescription() + " কল করছেন।";
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            return "এখন কোনো active call নেই। সর্বশেষ caller জানতে Call Log permission দিন।";
        try (Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE},
                null, null, CallLog.Calls.DATE + " DESC LIMIT 1")) {
            if (cursor != null && cursor.moveToFirst()) {
                String number = clean(cursor.getString(0));
                String name = clean(cursor.getString(1));
                int type = cursor.getInt(2);
                String direction = type == CallLog.Calls.MISSED_TYPE ? "মিসড কল করেছিলেন"
                        : type == CallLog.Calls.OUTGOING_TYPE ? "-কে সর্বশেষ কল করা হয়েছিল" : "সর্বশেষ কল করেছিলেন";
                return (name.isEmpty() ? mask(number) : name) + " " + direction + "।";
            }
        } catch (Exception ignored) { }
        return "সাম্প্রতিক caller-এর তথ্য পাওয়া যায়নি।";
    }

    public static String recentCalls(Context context, int limit) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            return "Recent call দেখতে Call Log permission দিন।";
        StringBuilder out = new StringBuilder();
        try (Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE},
                null, null, CallLog.Calls.DATE + " DESC LIMIT " + Math.max(1, Math.min(10, limit)))) {
            while (cursor != null && cursor.moveToNext()) {
                String number = clean(cursor.getString(0));
                String name = clean(cursor.getString(1));
                int type = cursor.getInt(2);
                String kind = type == CallLog.Calls.MISSED_TYPE ? "Missed"
                        : type == CallLog.Calls.OUTGOING_TYPE ? "Outgoing" : "Incoming";
                String time = new SimpleDateFormat("dd MMM, hh:mm a", new Locale("bn", "IN"))
                        .format(new Date(cursor.getLong(3)));
                if (out.length() > 0) out.append('\n');
                out.append(kind).append(" • ").append(name.isEmpty() ? mask(number) : name).append(" • ").append(time);
            }
        } catch (Exception ignored) { }
        return out.length() == 0 ? "কোনো recent call পাওয়া যায়নি।" : out.toString();
    }

    private static DeviceController.Result result(boolean success, String yes, String no) {
        return new DeviceController.Result(success, success ? yes : no);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }

    private static String mask(String number) {
        if (number == null || number.length() <= 4) return number == null ? "অজানা নম্বর" : number;
        return "শেষ " + number.substring(number.length() - 4) + " নম্বর";
    }
}
