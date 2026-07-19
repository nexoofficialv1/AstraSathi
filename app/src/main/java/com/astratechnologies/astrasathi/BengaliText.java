package com.astratechnologies.astrasathi;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BengaliText {
    private static final Map<String, Integer> NUMBER_WORDS = new LinkedHashMap<>();
    static {
        NUMBER_WORDS.put("বারো", 12); NUMBER_WORDS.put("এগারো", 11);
        NUMBER_WORDS.put("দশ", 10); NUMBER_WORDS.put("নয়", 9); NUMBER_WORDS.put("নয়", 9);
        NUMBER_WORDS.put("আট", 8); NUMBER_WORDS.put("সাত", 7); NUMBER_WORDS.put("ছয়", 6);
        NUMBER_WORDS.put("ছয়", 6); NUMBER_WORDS.put("পাঁচ", 5); NUMBER_WORDS.put("চার", 4);
        NUMBER_WORDS.put("তিন", 3); NUMBER_WORDS.put("দুই", 2); NUMBER_WORDS.put("এক", 1);
    }

    private BengaliText() {}

    public static String normalize(String value) {
        if (value == null) return "";
        String text = toAsciiDigits(value.toLowerCase(Locale.ROOT));
        return text.replace('।', ' ').replace(',', ' ').replace('?', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    public static String toAsciiDigits(String value) {
        String bn = "০১২৩৪৫৬৭৮৯";
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            int i = bn.indexOf(c);
            out.append(i >= 0 ? (char) ('0' + i) : c);
        }
        return out.toString();
    }

    public static int[] parseTime(String raw) {
        String text = normalize(raw);
        int hour = -1;
        int minute = 0;

        Matcher clock = Pattern.compile("(?:^|\\s)([01]?\\d|2[0-3])(?:[:.]([0-5]?\\d))?\\s*(?:টা|টায়|টায়|ঘটিকা)?").matcher(text);
        while (clock.find()) {
            hour = Integer.parseInt(clock.group(1));
            minute = clock.group(2) == null ? 0 : Integer.parseInt(clock.group(2));
        }

        if (hour < 0) {
            for (Map.Entry<String, Integer> entry : NUMBER_WORDS.entrySet()) {
                if (text.contains(entry.getKey())) {
                    hour = entry.getValue();
                    break;
                }
            }
        }

        if (hour >= 0) {
            if (text.contains("সাড়ে") || text.contains("সাড়ে")) minute = 30;
            if (text.contains("সোয়া") || text.contains("সোয়া")) minute = 15;
            if (text.contains("পৌনে")) {
                hour = (hour + 23) % 24;
                minute = 45;
            }
            boolean pm = text.contains("বিকাল") || text.contains("বিকেল")
                    || text.contains("সন্ধ্যা") || text.contains("রাত") || text.contains("দুপুর");
            if (pm && hour < 12) hour += 12;
            if (text.contains("সকাল") && hour == 12) hour = 0;
        }
        return new int[]{hour, minute};
    }

    public static int parseDayOffset(String raw) {
        String text = normalize(raw);
        if (text.contains("পরশু")) return 2;
        if (text.contains("আগামীকাল") || text.contains("কালকে")) return 1;
        return 0;
    }

    public static String cleanPerson(String value) {
        return normalize(value).replaceAll("^(আমার|আমাদের)\\s+", "")
                .replaceAll("\\s*(কে|নম্বর|নাম্বার)$", "").trim();
    }
}
