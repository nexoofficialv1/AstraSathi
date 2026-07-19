import com.astratechnologies.astrasathi.Command;
import com.astratechnologies.astrasathi.CommandRouter;

public class CommandRouterTest {
    private static int passed = 0;

    public static void main(String[] args) {
        CommandRouter router = new CommandRouter();
        expect(router, "হ্যালো সাথী সকাল ৭টায় অ্যালার্ম দাও", Command.Type.ALARM, "", 7, 0);
        expect(router, "রাত ৯:৩০ টায় অ্যালার্ম দাও", Command.Type.ALARM, "", 21, 30);
        expect(router, "সাড়ে সাতটায় অ্যালার্ম দাও", Command.Type.ALARM, "", 7, 30);
        expect(router, "মাকে ফোন করো", Command.Type.CALL, "মা", -1, -1);
        expect(router, "রাহুলকে মেসেজ পাঠাও আমি আসছি", Command.Type.SMS, "রাহুল", -1, -1);
        expect(router, "রাহুলকে হোয়াটসঅ্যাপে মেসেজ পাঠাও আমি আসছি", Command.Type.WHATSAPP, "রাহুল", -1, -1);
        expect(router, "নোট লেখো কাল রিপোর্ট জমা দিতে হবে", Command.Type.NOTE_SAVE, "", -1, -1);
        expect(router, "আমার নোট দেখাও", Command.Type.NOTE_SHOW, "", -1, -1);
        expect(router, "হোয়াটসঅ্যাপ খোলো", Command.Type.OPEN_APP, "হোয়াটসঅ্যাপ", -1, -1);
        expect(router, "কালনার আবহাওয়া দেখাও", Command.Type.WEATHER, "কালনার", -1, -1);
        expect(router, "টর্চ জ্বালাও", Command.Type.TORCH_ON, "", -1, -1);
        expect(router, "স্ক্রিনের লেখা পড়ো", Command.Type.READ_SCREEN, "", -1, -1);
        expect(router, "নিচে স্ক্রল করো", Command.Type.SCROLL_DOWN, "", -1, -1);
        expect(router, "পাঠান বোতামে চাপ দাও", Command.Type.CLICK_TEXT, "পাঠান", -1, -1);
        expect(router, "স্ক্রিনের বোতামগুলো দেখাও", Command.Type.ACTIONS_LIST, "", -1, -1);
        expect(router, "৩ নম্বরে চাপ দাও", Command.Type.CLICK_NUMBER, "3", -1, -1);
        expect(router, "রেকর্ড শুরু করো", Command.Type.MACRO_START, "", -1, -1);
        expect(router, "রেকর্ড বন্ধ করো সকাল নামে", Command.Type.MACRO_STOP, "সকাল", -1, -1);
        expect(router, "রেকর্ড বাতিল করো", Command.Type.MACRO_CANCEL, "", -1, -1);
        expect(router, "এখানে লিখো আমি আগামীকাল আসব", Command.Type.TYPE_TEXT, "", -1, -1);
        expect(router, "মনে রেখো প্রতি সোমবার রিপোর্ট দিতে হবে", Command.Type.MEMORY_SAVE, "", -1, -1);
        expect(router, "আমি কী ভুলে গেছি", Command.Type.MEMORY_PENDING, "", -1, -1);
        expect(router, "কাজটা হয়ে গেছে", Command.Type.MEMORY_COMPLETE, "", -1, -1);
        expect(router, "ব্রাইটনেস ৫০ শতাংশ করো", Command.Type.BRIGHTNESS, "50", -1, -1);
        expect(router, "অটো রোটেট বন্ধ করো", Command.Type.AUTO_ROTATE_OFF, "", -1, -1);
        expect(router, "সিকিউরিটি সেটিংস খোলো", Command.Type.SECURITY_SETTINGS, "", -1, -1);
        expect(router, "হোয়াটসঅ্যাপ খোলো তারপর সার্চে চাপ দাও", Command.Type.WORKFLOW, "", -1, -1);
        expect(router, "সকাল রুটিন চালাও", Command.Type.ROUTINE_RUN, "সকাল", -1, -1);
        expect(router, "TCS-এর ১০টা শেয়ার মার্কেট প্রাইসে কিনো", Command.Type.TRADE_ORDER, "", -1, -1);
        expect(router, "হোয়াটসঅ্যাপের শেষ মেসেজ পড়ে শোনাও", Command.Type.APP_NOTIFICATION_READ, "whatsapp", -1, -1);
        expect(router, "হোয়াটসঅ্যাপের মেসেজের উত্তর দাও আমি আসছি", Command.Type.APP_NOTIFICATION_REPLY, "whatsapp", -1, -1);
        expect(router, "ফেসবুকের নোটিফিকেশন খোলো", Command.Type.APP_NOTIFICATION_OPEN, "facebook", -1, -1);
        expect(router, "কে কল করছে", Command.Type.CALL_WHO, "", -1, -1);
        expect(router, "কল ধরো", Command.Type.CALL_ANSWER, "", -1, -1);
        expect(router, "কল কেটে দাও", Command.Type.CALL_END, "", -1, -1);
        expect(router, "কলের স্পিকার চালু", Command.Type.CALL_SPEAKER_ON, "", -1, -1);
        expect(router, "WhatsApp এ মেসেজ দেখাও", Command.Type.APP_NOTIFICATION_READ, "whatsapp", -1, -1);
        expect(router, "Facebook চালাও", Command.Type.OPEN_APP, "facebook", -1, -1);
        expect(router, "তিন দিন আগে গাড়িতে ৫ লিটার তেল ভরেছি", Command.Type.VEHICLE_FUEL_LOG, "গাড়ি", -1, -1);
        expect(router, "এরপর গাড়ি ১৭৮ কিলোমিটার চালিয়েছি", Command.Type.VEHICLE_DISTANCE_LOG, "গাড়ি", -1, -1);
        expect(router, "আজকে গাড়িতে তেল ভরতে হবে", Command.Type.VEHICLE_REFUEL_NEEDED, "গাড়ি", -1, -1);
        expect(router, "আমি রাহুলকে বলেছি কালকে রিপোর্ট দিয়ে দেবো", Command.Type.COMMITMENT_SAVE, "রাহুল", -1, -1);
        expect(router, "আজকে দুপুর ২টায় পেট ব্যথার ওষুধ খেয়েছি", Command.Type.MEDICINE_LOG, "পেট ব্যথার ওষুধ", -1, -1);
        expect(router, "আমার বর্তমান পরিস্থিতি কেমন", Command.Type.LIFE_STATUS, "", -1, -1);
        System.out.println("সব " + passed + "টি CommandRouter পরীক্ষা সফল হয়েছে।");
    }

    private static void expect(CommandRouter router, String input, Command.Type type,
                               String target, int hour, int minute) {
        Command result = router.parse(input);
        if (result.type != type) fail(input + " -> type " + result.type + ", expected " + type);
        if (!target.isEmpty() && !result.target.equals(target)) fail(input + " -> target “" + result.target + "”");
        if (hour >= 0 && result.hour != hour) fail(input + " -> hour " + result.hour);
        if (minute >= 0 && result.minute != minute) fail(input + " -> minute " + result.minute);
        passed++;
    }

    private static void fail(String message) { throw new AssertionError(message); }
}
