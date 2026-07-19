package com.astratechnologies.astrasathi;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bengali rule parser for vehicle, commitment and health context. */
public final class LifeContextParser {
    private static final Map<String, Integer> NUMBERS = new LinkedHashMap<>();
    static {
        NUMBERS.put("বিশ", 20); NUMBERS.put("উনিশ", 19); NUMBERS.put("আঠারো", 18);
        NUMBERS.put("সতেরো", 17); NUMBERS.put("ষোলো", 16); NUMBERS.put("পনেরো", 15);
        NUMBERS.put("চৌদ্দ", 14); NUMBERS.put("তেরো", 13); NUMBERS.put("বারো", 12);
        NUMBERS.put("এগারো", 11); NUMBERS.put("দশ", 10); NUMBERS.put("নয়", 9);
        NUMBERS.put("নয়", 9); NUMBERS.put("আট", 8); NUMBERS.put("সাত", 7);
        NUMBERS.put("ছয়", 6); NUMBERS.put("ছয়", 6); NUMBERS.put("পাঁচ", 5);
        NUMBERS.put("চার", 4); NUMBERS.put("তিন", 3); NUMBERS.put("দুই", 2);
        NUMBERS.put("এক", 1);
    }

    public LifeContextIntent parse(String raw) {
        String text = BengaliText.normalize(raw);
        if (text.isEmpty()) return LifeContextIntent.none(raw);

        if (containsAny(text, "লাইফ কনটেক্সট খোলো", "জীবনের হিসাব খোলো", "জার্ভিস ড্যাশবোর্ড খোলো"))
            return intent(LifeContextIntent.Type.OPEN_DASHBOARD, raw, "", text, 0, 0, 0, -1, 0, 1);
        if (containsAny(text, "আমার বর্তমান পরিস্থিতি কেমন", "আমার বর্তমান অবস্থা কেমন",
                "আজ আমার কী কী অবস্থা", "জীবনের বর্তমান হিসাব", "আমার আজকের সারাংশ"))
            return intent(LifeContextIntent.Type.LIFE_STATUS, raw, "", text, 0, 0, 0, -1, 0, 1);

        if (containsAny(text, "শরীরের অবস্থা", "স্বাস্থ্যের অবস্থা", "শরীর এখন কেমন",
                "স্বাস্থ্য এখন কেমন", "ওষুধের হিসাব", "কী ওষুধ খেয়েছি", "কি ওষুধ খেয়েছি"))
            return intent(LifeContextIntent.Type.HEALTH_STATUS, raw, "", text, 0, 0, 0, -1, 0, 1);

        if (containsAny(text, "শেষ প্রতিশ্রুতি সম্পন্ন", "শেষ দেওয়া কথা রেখেছি", "শেষ দেওয়া কথা রেখেছি",
                "শেষ কথার কাজ হয়ে গেছে", "শেষ কথার কাজ হয়ে গেছে"))
            return intent(LifeContextIntent.Type.COMMITMENT_COMPLETE, raw, "", text, 0, 0, 0, -1, 0, 1);
        if (containsAny(text, "কী কথা দিয়েছি", "কি কথা দিয়েছি", "প্রতিশ্রুতি দেখাও",
                "কাকে কী বলেছি", "কাকে কি বলেছি", "দেওয়া কথাগুলো", "দেওয়া কথাগুলো"))
            return intent(LifeContextIntent.Type.COMMITMENT_LIST, raw, "", text, 0, 0, 0, -1, 0, 1);

        LifeContextIntent commitment = parseCommitment(raw, text);
        if (commitment.type != LifeContextIntent.Type.NONE) return commitment;

        if (isMedicineLog(text)) {
            String medicine = cleanMedicine(text);
            int[] time = BengaliText.parseTime(text);
            return intent(LifeContextIntent.Type.MEDICINE_LOG, raw, medicine, text,
                    0, pastDayOffset(text), 0, time[0], time[1], medicine.isEmpty() ? 0.65 : 0.88);
        }
        if (isSymptomLog(text))
            return intent(LifeContextIntent.Type.SYMPTOM_LOG, raw, symptomName(text), text,
                    0, pastDayOffset(text), 0, -1, 0, 0.85);

        if (isVehicleText(text)) {
            if (containsAny(text, "তেল ভরতে হবে", "পেট্রোল ভরতে হবে", "ডিজেল ভরতে হবে",
                    "তেল নিতে হবে", "পেট্রোল নিতে হবে"))
                return intent(LifeContextIntent.Type.REFUEL_NEEDED, raw, "গাড়ি", text,
                        0, 0, 0, -1, 0, 1);
            if (containsAny(text, "তেলের অবস্থা", "কত তেল আছে", "তেল কত আছে", "তেলের হিসাব",
                    "গাড়ির হিসাব", "গাড়ির হিসাব", "কখন তেল ভরতে হবে", "আর কত চলবে"))
                return intent(LifeContextIntent.Type.VEHICLE_STATUS, raw, "গাড়ি", text,
                        0, 0, 0, -1, 0, 1);
            if (text.contains("মাইলেজ") && containsAny(text, "দেয়", "দেয়", "দিচ্ছে", "হয়", "হয়", "সেট"))
                return intent(LifeContextIntent.Type.MILEAGE_SET, raw, "গাড়ি", text,
                        numberNearUnit(text, "মাইলেজ"), pastDayOffset(text), 0, -1, 0, 0.9);
            if (containsAny(text, "কিলোমিটার", "কিমি", "km")
                    && containsAny(text, "রান করেছি", "চালিয়েছি", "চালিয়েছি", "চলেছি", "গিয়েছি", "গিয়েছি"))
                return intent(LifeContextIntent.Type.DISTANCE_LOG, raw, "গাড়ি", text,
                        numberBeforeAny(text, "কিলোমিটার", "কিমি", "km"), pastDayOffset(text), 0, -1, 0, 0.94);
            if (containsAny(text, "তেল", "পেট্রোল", "ডিজেল")
                    && containsAny(text, "ভরেছি", "ভরলাম", "নিয়েছি", "নিয়েছি", "দিয়েছি", "দিয়েছি"))
                return intent(LifeContextIntent.Type.FUEL_LOG, raw, "গাড়ি", text,
                        numberBeforeAny(text, "লিটার", "litre", "liter"), pastDayOffset(text), 0, -1, 0, 0.92);
        }
        return LifeContextIntent.none(raw);
    }

    public List<LifeContextIntent> parseAll(String raw) {
        List<LifeContextIntent> result = new ArrayList<>();
        if (raw != null) {
            for (String part : raw.split("[,।;]+|\\s+(?:তারপর|এরপর|তার পরে|এর পরে)\\s+")) {
                LifeContextIntent parsed = parse(part.trim());
                if (parsed.type != LifeContextIntent.Type.NONE) result.add(parsed);
            }
        }
        if (result.isEmpty()) {
            LifeContextIntent whole = parse(raw);
            if (whole.type != LifeContextIntent.Type.NONE) result.add(whole);
        }
        return result;
    }

    private LifeContextIntent parseCommitment(String raw, String text) {
        if (!text.contains("বলেছি") && !text.contains("কথা দিয়েছি") && !text.contains("কথা দিয়েছি"))
            return LifeContextIntent.none(raw);
        Matcher said = Pattern.compile("^(?:আমি\\s+)?(.+?)কে\\s+(?:বলেছি|কথা দিয়েছি|কথা দিয়েছি)\\s+(?:যে\\s+)?(.+)$").matcher(text);
        String person = "";
        String task = text;
        if (said.matches()) {
            person = BengaliText.cleanPerson(said.group(1))
                    .replaceFirst("^(?:ফোনে|কলে|কল করে)\\s+", "")
                    .replaceAll("[-–—]+$", "").trim();
            task = said.group(2).trim();
        }
        int[] time = BengaliText.parseTime(text);
        int day = futureDayOffset(text);
        double confidence = person.isEmpty() ? 0.68 : 0.9;
        return intent(LifeContextIntent.Type.COMMITMENT_SAVE, raw, person, task,
                0, 0, day, time[0], time[1], confidence);
    }

    private boolean isVehicleText(String text) {
        return containsAny(text, "গাড়ি", "গাড়ি", "বাইক", "স্কুটার", "মোটরসাইকেল")
                || text.contains("মাইলেজ")
                || (containsAny(text, "কিলোমিটার", "কিমি", "km")
                && containsAny(text, "রান করেছি", "চালিয়েছি", "চালিয়েছি", "চলেছি", "গিয়েছি", "গিয়েছি"))
                || (containsAny(text, "তেল", "পেট্রোল", "ডিজেল")
                && containsAny(text, "লিটার", "ভর", "মাইলেজ", "কিলোমিটার"));
    }

    private boolean isMedicineLog(String text) {
        return containsAny(text, "ওষুধ", "মেডিসিন", "ট্যাবলেট")
                && containsAny(text, "খেয়েছি", "খেয়েছি", "নিয়েছি", "নিয়েছি", "খেলাম");
    }

    private boolean isSymptomLog(String text) {
        return containsAny(text, "ব্যথা", "জ্বর", "বমি", "মাথা ঘোরা", "দুর্বল লাগছে", "শ্বাসকষ্ট")
                && containsAny(text, "হচ্ছে", "আছে", "লাগছে", "শুরু হয়েছে", "শুরু হয়েছে", "বেড়েছে", "বেড়েছে");
    }

    private String symptomName(String text) {
        String[] names = {"পেট ব্যথা", "পেটে ব্যথা", "মাথা ব্যথা", "বুক ব্যথা", "জ্বর",
                "বমি", "মাথা ঘোরা", "দুর্বলতা", "শ্বাসকষ্ট"};
        for (String name : names) if (text.contains(name)) return name;
        return "উপসর্গ";
    }

    private String cleanMedicine(String text) {
        String cleaned = text.replaceAll("আজকে|আজ|গতকাল|পরশু|\\d+\\s*দিন আগে", " ")
                .replaceAll("সকাল|দুপুর|বিকাল|বিকেল|সন্ধ্যা|রাত", " ")
                .replaceAll("\\d{1,2}(?::\\d{1,2})?\\s*(?:টায়|টায়|টা)?", " ")
                .replaceAll("খেয়েছি|খেয়েছি|নিয়েছি|নিয়েছি|খেলাম", " ")
                .replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? "ওষুধ" : cleaned;
    }

    private int pastDayOffset(String text) {
        if (text.contains("গতকাল")) return -1;
        Matcher days = Pattern.compile("(\\d+|এক|দুই|তিন|চার|পাঁচ|ছয়|ছয়|সাত)\\s*দিন আগে").matcher(text);
        if (days.find()) return -Math.max(1, parseNumber(days.group(1)));
        return 0;
    }

    private int futureDayOffset(String text) {
        if (text.contains("পরশু")) return 2;
        if (text.contains("আগামীকাল") || text.contains("কালকে") || text.matches(".*(?:^|\\s)কাল(?:\\s|$).*$")) return 1;
        return 0;
    }

    private double numberNearUnit(String text, String marker) {
        Matcher after = Pattern.compile(Pattern.quote(marker) + "\\s*(?:হলো|হয়|হয়|দেয়|দেয়|দিচ্ছে|সেট)?\\s*(\\d+(?:\\.\\d+)?)").matcher(text);
        if (after.find()) return Double.parseDouble(after.group(1));
        return firstNumber(text);
    }

    private double numberBeforeAny(String text, String... units) {
        for (String unit : units) {
            Matcher digits = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*" + Pattern.quote(unit)).matcher(text);
            if (digits.find()) return Double.parseDouble(digits.group(1));
            for (Map.Entry<String, Integer> number : NUMBERS.entrySet())
                if (text.matches(".*(?:^|\\s)" + number.getKey() + "\\s*" + Pattern.quote(unit) + ".*"))
                    return number.getValue();
        }
        return 0;
    }

    private double firstNumber(String text) {
        Matcher matcher = Pattern.compile("(?:^|\\s)(\\d+(?:\\.\\d+)?)(?:\\s|$)").matcher(text);
        if (matcher.find()) return Double.parseDouble(matcher.group(1));
        for (Map.Entry<String, Integer> entry : NUMBERS.entrySet()) if (text.contains(entry.getKey())) return entry.getValue();
        return 0;
    }

    private int parseNumber(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return NUMBERS.getOrDefault(value, 0); }
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private LifeContextIntent intent(LifeContextIntent.Type type, String original, String subject,
                                     String details, double amount, int eventDayOffset,
                                     int reminderDayOffset, int hour, int minute, double confidence) {
        return new LifeContextIntent(type, original, subject, details, amount, eventDayOffset,
                reminderDayOffset, hour, minute, confidence);
    }
}
