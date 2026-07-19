package com.astratechnologies.astrasathi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandRouter {
    public Command parse(String raw) {
        String text = BengaliText.normalize(raw);
        if (text.isEmpty()) return Command.of(Command.Type.UNKNOWN, raw, "", "");
        text = stripWakePhrase(text);

        if (containsAny(text, "স্ক্রিনের বোতামগুলো দেখাও", "স্ক্রিনের বাটনগুলো দেখাও",
                "কী কী চাপা যাবে", "কি কি চাপা যাবে", "চাপার অপশন দেখাও", "অ্যাকশনগুলো দেখাও"))
            return Command.of(Command.Type.ACTIONS_LIST, raw, "", "");

        Matcher numberedClick = Pattern.compile("^(\\d{1,2})\\s*(?:নম্বর|নাম্বার|নং)(?:ে|তে)?(?:\\s+বোতাম(?:ে|তে)?)?\\s+(?:চাপ দাও|ক্লিক করো|ট্যাপ করো)$").matcher(text);
        if (numberedClick.matches())
            return Command.of(Command.Type.CLICK_NUMBER, raw, numberedClick.group(1), "");

        if (containsAny(text, "রেকর্ড বাতিল করো", "রেকর্ডিং বাতিল করো", "শেখানো বাতিল করো"))
            return Command.of(Command.Type.MACRO_CANCEL, raw, "", "");
        Matcher stopRecordPrefix = Pattern.compile("^(?:রেকর্ড|রেকর্ডিং)\\s+(?:বন্ধ|শেষ)\\s+করো(?:\\s+(.+?)(?:\\s+নামে|\\s+রুটিন হিসেবে)?)?$").matcher(text);
        if (stopRecordPrefix.matches())
            return Command.of(Command.Type.MACRO_STOP, raw, cleanRoutineName(stopRecordPrefix.group(1)), "");
        Matcher stopRecordSuffix = Pattern.compile("^(.+?)(?:\\s+নামে|\\s+রুটিন হিসেবে)\\s+(?:রেকর্ড|রেকর্ডিং)\\s+(?:বন্ধ|শেষ)\\s+করো$").matcher(text);
        if (stopRecordSuffix.matches())
            return Command.of(Command.Type.MACRO_STOP, raw, cleanRoutineName(stopRecordSuffix.group(1)), "");
        if (containsAny(text, "রেকর্ড শুরু করো", "রেকর্ডিং শুরু করো", "কাজ শেখা শুরু করো", "আমার কাজ রেকর্ড করো"))
            return Command.of(Command.Type.MACRO_START, raw, "", "");

        Command lifeContext = parseLifeContext(raw);
        if (lifeContext != null) return lifeContext;

        if (text.contains("তারপর") || text.contains("এরপর") || text.contains("তার পরে") || text.contains("এর পরে"))
            return Command.of(Command.Type.WORKFLOW, raw, "", text);

        if (containsAny(text, "রুটিনগুলো দেখাও", "রুটিন দেখাও", "আমার রুটিন"))
            return Command.of(Command.Type.ROUTINE_LIST, raw, "", "");
        Matcher routineSave = Pattern.compile("(?:এই কাজটা|এই কাজটি)\\s+(.+?)\\s+(?:নামে |হিসেবে )?রুটিন\\s+(?:হিসেবে )?(?:মনে রাখো|সেভ করো)").matcher(text);
        if (routineSave.find())
            return Command.of(Command.Type.ROUTINE_SAVE, raw, routineSave.group(1).trim(), "");
        Matcher routineRun = Pattern.compile("^(.+?)\\s+রুটিন\\s+(?:চালাও|করো|শুরু করো)$").matcher(text);
        if (routineRun.matches())
            return Command.of(Command.Type.ROUTINE_RUN, raw, routineRun.group(1).trim(), "");

        if ((text.contains("শেয়ার") || text.contains("শেয়ার") || text.contains("স্টক"))
                && containsAny(text, "কিনো", "কিনে দাও", "বেচো", "বিক্রি করো", "buy", "sell"))
            return Command.of(Command.Type.TRADE_ORDER, raw, "", text);

        if (containsAny(text, "কে কল করছে", "কে ফোন করছে", "কে কল করেছে", "কে ফোন করেছে"))
            return Command.of(Command.Type.CALL_WHO, raw, "", "");
        if (containsAny(text, "কল ধরো", "ফোন ধরো", "কল রিসিভ করো", "ফোন রিসিভ করো", "কলের উত্তর দাও"))
            return Command.of(Command.Type.CALL_ANSWER, raw, "", "");
        if (containsAny(text, "কল কেটে দাও", "ফোন কেটে দাও", "কল শেষ করো"))
            return Command.of(Command.Type.CALL_END, raw, "", "");
        if (containsAny(text, "কলের স্পিকার চালু", "ফোনের স্পিকার চালু", "স্পিকারফোন চালু"))
            return Command.of(Command.Type.CALL_SPEAKER_ON, raw, "", "");
        if (containsAny(text, "কলের স্পিকার বন্ধ", "ফোনের স্পিকার বন্ধ", "স্পিকারফোন বন্ধ"))
            return Command.of(Command.Type.CALL_SPEAKER_OFF, raw, "", "");
        if (containsAny(text, "কল মিউট করো", "মাইক মিউট করো"))
            return Command.of(Command.Type.CALL_MUTE, raw, "", "");
        if (containsAny(text, "কল আনমিউট করো", "মাইক চালু করো"))
            return Command.of(Command.Type.CALL_UNMUTE, raw, "", "");

        Matcher appReply = Pattern.compile("^(?:শেষ )?(হোয়াটসঅ্যাপ|হোয়াটসঅ্যাপ|whatsapp|ফেসবুক|facebook|মেসেঞ্জার|messenger)(?:ের| এর|ে| এ)?(?:\\s+(?:শেষ )?(?:মেসেজ|বার্তা|নোটিফিকেশন)(?:টির|ের)?)?\\s+(?:উত্তর দাও|রিপ্লাই দাও)\\s+(.+)$").matcher(text);
        if (appReply.matches())
            return Command.of(Command.Type.APP_NOTIFICATION_REPLY, raw,
                    socialTarget(appReply.group(1)), appReply.group(2));
        Matcher appNotification = Pattern.compile("^(হোয়াটসঅ্যাপ|হোয়াটসঅ্যাপ|whatsapp|ফেসবুক|facebook|মেসেঞ্জার|messenger)(?:ের| এর|ে| এ)?\\s+(?:শেষ )?(?:মেসেজ|বার্তা|নোটিফিকেশন)(?:গুলো|টি)?\\s+(পড়ো|পড়ো|পড়ে শোনাও|পড়ে শোনাও|দেখাও|খোলো)$").matcher(text);
        if (appNotification.matches()) {
            boolean open = "খোলো".equals(appNotification.group(2));
            return Command.of(open ? Command.Type.APP_NOTIFICATION_OPEN : Command.Type.APP_NOTIFICATION_READ,
                    raw, socialTarget(appNotification.group(1)), "");
        }

        if (containsAny(text, "কী বলতে পারি", "কি বলতে পারি", "সাহায্য", "হেল্প", "কমান্ড দেখাও"))
            return Command.of(Command.Type.HELP, raw, "", "");
        if (containsAny(text, "নোটগুলো দেখাও", "নোট দেখাও", "আমার নোট", "নোট পড়ো", "নোট পড়ো"))
            return Command.of(Command.Type.NOTE_SHOW, raw, "", "");
        if (containsAny(text, "কী মনে রেখেছ", "কি মনে রেখেছ", "আমার সম্পর্কে কী জানো", "আমার সম্পর্কে কি জানো", "স্মৃতি দেখাও"))
            return Command.of(Command.Type.MEMORY_RECALL, raw, "", "");
        if (containsAny(text, "আমি কী ভুলে গেছি", "আমি কি ভুলে গেছি", "অসম্পূর্ণ কাজ দেখাও", "বাকি কাজ দেখাও"))
            return Command.of(Command.Type.MEMORY_PENDING, raw, "", "");
        if (containsAny(text, "কাজটি হয়ে গেছে", "কাজটা হয়ে গেছে", "কাজটি সম্পন্ন", "শেষ কাজ সম্পন্ন", "রিমাইন্ডারটি শেষ"))
            return Command.of(Command.Type.MEMORY_COMPLETE, raw, "", "");
        if (containsAny(text, "সব স্মৃতি মুছে দাও", "সব কিছু ভুলে যাও", "সব পার্সোনাল মেমোরি মুছ"))
            return Command.of(Command.Type.MEMORY_FORGET_ALL, raw, "", "");
        if (containsAny(text, "টর্চ বন্ধ", "ফ্ল্যাশ বন্ধ"))
            return Command.of(Command.Type.TORCH_OFF, raw, "", "");
        if (containsAny(text, "টর্চ জ্বালাও", "টর্চ চালু", "ফ্ল্যাশ জ্বালাও", "ফ্ল্যাশ চালু"))
            return Command.of(Command.Type.TORCH_ON, raw, "", "");

        if (containsAny(text, "ফুল অ্যাক্সেস", "ফুল একসেস", "অ্যাক্সেস সেটআপ", "একসেস সেটআপ"))
            return Command.of(Command.Type.FULL_ACCESS, raw, "", "");
        if (containsAny(text, "হোম স্ক্রিনে যাও", "হোমে যাও", "হোম চাপো"))
            return Command.of(Command.Type.HOME, raw, "", "");
        if (containsAny(text, "পেছনে যাও", "পিছনে যাও", "ব্যাক করো", "ব্যাক চাপো"))
            return Command.of(Command.Type.BACK, raw, "", "");
        if (containsAny(text, "রিসেন্ট অ্যাপ", "সাম্প্রতিক অ্যাপ", "রিসেন্টস দেখাও"))
            return Command.of(Command.Type.RECENTS, raw, "", "");
        if (containsAny(text, "কুইক সেটিংস", "কন্ট্রোল সেন্টার"))
            return Command.of(Command.Type.QUICK_SETTINGS, raw, "", "");
        if (containsAny(text, "নোটিফিকেশন প্যানেল", "নোটিফিকেশন খোলো", "নোটিফিকেশন বার খোলো"))
            return Command.of(Command.Type.NOTIFICATIONS, raw, "", "");
        if (containsAny(text, "স্ক্রিন লক করো", "ফোন লক করো"))
            return Command.of(Command.Type.LOCK_SCREEN, raw, "", "");
        if (containsAny(text, "স্ক্রিনশট নাও", "স্ক্রিন শট নাও"))
            return Command.of(Command.Type.SCREENSHOT, raw, "", "");
        if (containsAny(text, "স্ক্রিন পড়ো", "স্ক্রিন পড়ো", "স্ক্রিনের লেখা পড়ো", "স্ক্রিনের লেখা পড়ো", "পর্দার লেখা পড়ো", "পর্দার লেখা পড়ো", "সব লেখা পড়ো", "সব লেখা পড়ো"))
            return Command.of(Command.Type.READ_SCREEN, raw, "", "");
        if (containsAny(text, "নিচে স্ক্রল", "স্ক্রল ডাউন", "নিচের দিকে যাও"))
            return Command.of(Command.Type.SCROLL_DOWN, raw, "", "");
        if (containsAny(text, "উপরে স্ক্রল", "স্ক্রল আপ", "উপরের দিকে যাও"))
            return Command.of(Command.Type.SCROLL_UP, raw, "", "");
        if (containsAny(text, "বামে সোয়াইপ", "বামে সোয়াইপ"))
            return Command.of(Command.Type.SWIPE_LEFT, raw, "", "");
        if (containsAny(text, "ডানে সোয়াইপ", "ডানে সোয়াইপ"))
            return Command.of(Command.Type.SWIPE_RIGHT, raw, "", "");
        if (containsAny(text, "ভলিউম বাড়াও", "ভলিউম বাড়াও", "আওয়াজ বাড়াও", "আওয়াজ বাড়াও"))
            return Command.of(Command.Type.VOLUME_UP, raw, "", "");
        if (containsAny(text, "ভলিউম কমাও", "আওয়াজ কমাও", "আওয়াজ কমাও"))
            return Command.of(Command.Type.VOLUME_DOWN, raw, "", "");
        if (containsAny(text, "ভলিউম মিউট", "আওয়াজ বন্ধ", "আওয়াজ বন্ধ"))
            return Command.of(Command.Type.VOLUME_MUTE, raw, "", "");
        if (containsAny(text, "পরের গান", "নেক্সট গান"))
            return Command.of(Command.Type.MEDIA_NEXT, raw, "", "");
        if (containsAny(text, "আগের গান", "প্রিভিয়াস গান", "প্রিভিয়াস গান"))
            return Command.of(Command.Type.MEDIA_PREVIOUS, raw, "", "");
        if (containsAny(text, "গান থামাও", "গান চালাও", "প্লে পজ", "প্লে করো", "পজ করো"))
            return Command.of(Command.Type.MEDIA_PLAY_PAUSE, raw, "", "");

        if (containsAny(text, "সাইলেন্ট মোড", "ফোন সাইলেন্ট"))
            return Command.of(Command.Type.RINGER_SILENT, raw, "", "");
        if (containsAny(text, "ভাইব্রেট মোড", "ফোন ভাইব্রেট"))
            return Command.of(Command.Type.RINGER_VIBRATE, raw, "", "");
        if (containsAny(text, "রিং মোড", "সাউন্ড মোড", "সাইলেন্ট বন্ধ"))
            return Command.of(Command.Type.RINGER_NORMAL, raw, "", "");

        Matcher brightness = Pattern.compile("(?:ব্রাইটনেস|উজ্জ্বলতা)\\s*(?:করো|রাখো|সেট করো)?\\s*(\\d{1,3})").matcher(text);
        if (brightness.find())
            return Command.of(Command.Type.BRIGHTNESS, raw, brightness.group(1), "");
        Matcher timeout = Pattern.compile("স্ক্রিন\\s*(?:টাইমআউট|বন্ধ হওয়ার সময়|বন্ধ হওয়ার সময়)\\s*(\\d+)\\s*(মিনিট|সেকেন্ড)?").matcher(text);
        if (timeout.find())
            return Command.of(Command.Type.SCREEN_TIMEOUT, raw, timeout.group(1), timeout.group(2));
        if (containsAny(text, "অটো রোটেট চালু", "স্ক্রিন রোটেশন চালু"))
            return Command.of(Command.Type.AUTO_ROTATE_ON, raw, "", "");
        if (containsAny(text, "অটো রোটেট বন্ধ", "স্ক্রিন রোটেশন বন্ধ"))
            return Command.of(Command.Type.AUTO_ROTATE_OFF, raw, "", "");
        if (text.contains("ওয়াইফাই") || text.contains("ওয়াইফাই") || text.contains("wi-fi"))
            return Command.of(Command.Type.WIFI_CONTROL, raw, text.contains("বন্ধ") ? "বন্ধ" : text.contains("চালু") ? "চালু" : "", "");
        if (text.contains("ব্লুটুথ"))
            return Command.of(Command.Type.BLUETOOTH_CONTROL, raw, text.contains("বন্ধ") ? "বন্ধ" : text.contains("চালু") ? "চালু" : "", "");
        if (containsAny(text, "লোকেশন সেটিং", "জিপিএস সেটিং", "লোকেশন চালু", "লোকেশন বন্ধ"))
            return Command.of(Command.Type.LOCATION_SETTINGS, raw, "", "");
        if (containsAny(text, "সিকিউরিটি সেটিং", "লক সেটিং", "পিন পরিবর্তন", "পাসওয়ার্ড পরিবর্তন", "পাসওয়ার্ড পরিবর্তন", "ফিঙ্গারপ্রিন্ট সেটিং"))
            return Command.of(Command.Type.SECURITY_SETTINGS, raw, "", "");
        if (containsAny(text, "প্রাইভেসি সেটিং", "গোপনীয়তা সেটিং", "গোপনীয়তা সেটিং"))
            return Command.of(Command.Type.PRIVACY_SETTINGS, raw, "", "");
        if (containsAny(text, "ডিসপ্লে সেটিং", "স্ক্রিন সেটিং"))
            return Command.of(Command.Type.DISPLAY_SETTINGS, raw, "", "");
        if (containsAny(text, "সাউন্ড সেটিং", "শব্দের সেটিং"))
            return Command.of(Command.Type.SOUND_SETTINGS, raw, "", "");
        Matcher uninstall = Pattern.compile("^(.+?)\\s+(?:অ্যাপ )?(?:আনইনস্টল করো|মুছে দাও)$").matcher(text);
        if (uninstall.matches())
            return Command.of(Command.Type.UNINSTALL_APP, raw, uninstall.group(1).trim(), "");
        Matcher appInfo = Pattern.compile("^(.+?)\\s+(?:অ্যাপের |অ্যাপ )?(?:সেটিংস|অ্যাপ ইনফো) খোলো$").matcher(text);
        if (appInfo.matches())
            return Command.of(Command.Type.APP_INFO, raw, appInfo.group(1).trim(), "");

        if (containsAny(text, "শেষ নোটিফিকেশন পড়ো", "শেষ নোটিফিকেশন পড়ো", "নোটিফিকেশন পড়ে শোনাও", "নোটিফিকেশন পড়ে শোনাও"))
            return Command.of(Command.Type.NOTIFICATION_READ, raw, "", "");
        if (containsAny(text, "শেষ নোটিফিকেশন খোলো", "নোটিফিকেশনটি খোলো"))
            return Command.of(Command.Type.NOTIFICATION_OPEN, raw, "", "");
        if (containsAny(text, "শেষ নোটিফিকেশন সরাও", "নোটিফিকেশনটি সরাও", "নোটিফিকেশন dismiss"))
            return Command.of(Command.Type.NOTIFICATION_DISMISS, raw, "", "");
        Matcher reply = Pattern.compile("(?:শেষ )?(?:মেসেজ|নোটিফিকেশন)(?:টির|ে)?\\s+(?:উত্তর দাও|রিপ্লাই দাও)\\s*(.*)").matcher(text);
        if (reply.find())
            return Command.of(Command.Type.NOTIFICATION_REPLY, raw, "", reply.group(1));

        Matcher click = Pattern.compile("^(.+?)(?:\\s+বোতাম)?(?:ে|তে)?\\s+(?:চাপ দাও|ক্লিক করো|ট্যাপ করো)$").matcher(text);
        if (click.matches())
            return Command.of(Command.Type.CLICK_TEXT, raw,
                    click.group(1).replaceAll("\\s+বোতাম$", "").trim(), "");
        Matcher type = Pattern.compile("^(?:এখানে\\s+)?(?:লিখো|টাইপ করো)\\s+(.+)$").matcher(text);
        if (type.matches())
            return Command.of(Command.Type.TYPE_TEXT, raw, "", type.group(1));

        if (text.contains("অ্যালার্ম") || text.contains("এলার্ম")) {
            int[] time = BengaliText.parseTime(text);
            return Command.timed(Command.Type.ALARM, raw, "অ্যাস্ট্রা সাথী অ্যালার্ম", time[0], time[1], 0);
        }
        if (text.contains("রিমাইন্ডার") || text.contains("মনে করিয়ে") || text.contains("মনে করিয়ে")) {
            int[] time = BengaliText.parseTime(text);
            String title = text.replaceAll("রিমাইন্ডার( দাও| করো| সেট করো)?", "")
                    .replaceAll("আমাকে\\s+", "").replaceAll("মনে করিয়ে দিও|মনে করিয়ে দিও", "").trim();
            return Command.timed(Command.Type.REMINDER, raw,
                    title.isEmpty() ? "ব্যক্তিগত রিমাইন্ডার" : title,
                    time[0], time[1], BengaliText.parseDayOffset(text));
        }

        Command message = parseMessage(raw, text);
        if (message != null) return message;

        if (containsAny(text, "মনে রেখো", "মনে রাখো", "এটা মনে রেখো")) {
            String content = afterAny(text, "এটা মনে রেখো", "মনে রেখো", "মনে রাখো");
            return Command.of(Command.Type.MEMORY_SAVE, raw, "", content);
        }

        if (containsAny(text, "নোট লেখো", "নোট করো", "লিখে রাখো")) {
            String content = afterAny(text, "নোট লেখো", "নোট করো", "লিখে রাখো");
            return Command.of(Command.Type.NOTE_SAVE, raw, "", content);
        }

        if (text.contains("ফোন করো") || text.contains("কল করো") || text.contains("ফোন লাগাও")) {
            String target = text.replaceAll("(কে)?\\s*(ফোন করো|কল করো|ফোন লাগাও).*", "").trim();
            return Command.of(Command.Type.CALL, raw, BengaliText.cleanPerson(target), "");
        }

        if (text.contains("আবহাওয়া") || text.contains("আবহাওয়া") || text.contains("ওয়েদার")) {
            String place = text.replaceAll("আজকের|কালকের|আবহাওয়া|আবহাওয়া|ওয়েদার|কেমন|দেখাও|বল", " ")
                    .replaceAll("\\s+", " ").trim();
            return Command.of(Command.Type.WEATHER, raw, place, "");
        }

        if (containsAny(text, "রাস্তা দেখাও", "দিকনির্দেশ", "ম্যাপে দেখাও", "নেভিগেশন")) {
            String place = text.replaceAll("(এর|র)?\\s*(রাস্তা দেখাও|দিকনির্দেশ দাও|দিকনির্দেশ|ম্যাপে দেখাও|নেভিগেশন চালু করো)", "")
                    .replaceAll("কীভাবে যাব|কিভাবে যাব", "").trim();
            return Command.of(Command.Type.NAVIGATE, raw, place, "");
        }

        if (containsAny(text, "খোলো", "চালু করো", "চালাও")) {
            String app = text.replaceAll("\\s*(খোলো|চালু করো|চালাও).*$", "").trim();
            return Command.of(Command.Type.OPEN_APP, raw, app, "");
        }

        if (containsAny(text, "খুঁজে দেখাও", "সার্চ করো", "খোঁজ করো")) {
            String query = text.replaceAll("\\s*(খুঁজে দেখাও|সার্চ করো|খোঁজ করো).*$", "").trim();
            return Command.of(Command.Type.WEB_SEARCH, raw, "", query);
        }
        return Command.of(Command.Type.UNKNOWN, raw, "", text);
    }

    private Command parseMessage(String raw, String text) {
        Pattern p = Pattern.compile("^(.+?)কে\\s+(?:(হোয়াটসঅ্যাপ|হোয়াটসঅ্যাপ)(?:ে)?\\s+)?(?:মেসেজ|বার্তা|এসএমএস)\\s*(?:পাঠাও|দাও|করো)?\\s*[:：-]?\\s*(.*)$");
        Matcher m = p.matcher(text);
        if (m.matches()) {
            boolean whatsapp = m.group(2) != null;
            return Command.of(whatsapp ? Command.Type.WHATSAPP : Command.Type.SMS,
                    raw, BengaliText.cleanPerson(m.group(1)), m.group(3));
        }
        Pattern wp = Pattern.compile("^(.+?)কে\\s+(?:হোয়াটসঅ্যাপ|হোয়াটসঅ্যাপ)\\s*(?:করো|পাঠাও)?\\s*[:：-]?\\s*(.*)$");
        Matcher wm = wp.matcher(text);
        if (wm.matches()) {
            return Command.of(Command.Type.WHATSAPP, raw,
                    BengaliText.cleanPerson(wm.group(1)), wm.group(2));
        }
        return null;
    }

    private Command parseLifeContext(String raw) {
        LifeContextIntent intent = new LifeContextParser().parse(raw);
        Command.Type type = switch (intent.type) {
            case FUEL_LOG -> Command.Type.VEHICLE_FUEL_LOG;
            case DISTANCE_LOG -> Command.Type.VEHICLE_DISTANCE_LOG;
            case MILEAGE_SET -> Command.Type.VEHICLE_MILEAGE_SET;
            case VEHICLE_STATUS -> Command.Type.VEHICLE_STATUS;
            case REFUEL_NEEDED -> Command.Type.VEHICLE_REFUEL_NEEDED;
            case COMMITMENT_SAVE -> Command.Type.COMMITMENT_SAVE;
            case COMMITMENT_LIST -> Command.Type.COMMITMENT_LIST;
            case COMMITMENT_COMPLETE -> Command.Type.COMMITMENT_COMPLETE;
            case MEDICINE_LOG -> Command.Type.MEDICINE_LOG;
            case SYMPTOM_LOG -> Command.Type.SYMPTOM_LOG;
            case HEALTH_STATUS -> Command.Type.HEALTH_STATUS;
            case LIFE_STATUS -> Command.Type.LIFE_STATUS;
            case OPEN_DASHBOARD -> Command.Type.LIFE_CONTEXT_OPEN;
            default -> null;
        };
        return type == null ? null : Command.of(type, raw, intent.subject, intent.details);
    }

    private String stripWakePhrase(String text) {
        return text.replaceFirst("^(হ্যালো|হেলো)\\s+(সাথী|অ্যাস্ট্রা সাথী)\\s*", "").trim();
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private String afterAny(String text, String... values) {
        int best = -1;
        String selected = "";
        for (String value : values) {
            int index = text.indexOf(value);
            if (index >= 0 && (best < 0 || index < best)) { best = index; selected = value; }
        }
        return best < 0 ? "" : text.substring(best + selected.length()).trim();
    }

    private String cleanRoutineName(String value) {
        if (value == null) return "";
        return value.replaceAll("\\s+(?:নামে|রুটিন হিসেবে)$", "").trim();
    }

    private String socialTarget(String value) {
        String text = BengaliText.normalize(value);
        if (text.contains("হোয়াট") || text.contains("হোয়াট") || text.contains("whatsapp")) return "whatsapp";
        if (text.contains("মেসেঞ্জার") || text.contains("messenger")) return "messenger";
        return "facebook";
    }
}
