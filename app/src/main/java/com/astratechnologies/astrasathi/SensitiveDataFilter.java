package com.astratechnologies.astrasathi;

import java.util.Locale;

public final class SensitiveDataFilter {
    private SensitiveDataFilter() { }

    public static boolean isSensitive(String value) { return isSensitive(value, ""); }

    public static boolean isSensitive(String value, String fieldHint) {
        String text = BengaliText.normalize((value == null ? "" : value) + " "
                + (fieldHint == null ? "" : fieldHint)).toLowerCase(Locale.ROOT);
        String[] blocked = {"otp", "ওটিপি", "one time password", "pin", "পিন", "password",
                "পাসওয়ার্ড", "পাসওয়ার্ড", "passcode", "cvv", "cvc", "card number",
                "কার্ড নম্বর", "upi pin", "ইউপিআই পিন", "security code", "সিকিউরিটি কোড"};
        for (String word : blocked) if (text.contains(word)) return true;
        return text.matches(".*(?:^|\\D)\\d{12,19}(?:\\D|$).*");
    }

    public static boolean isProtectedAction(String value) {
        String text = BengaliText.normalize(value).toLowerCase(Locale.ROOT);
        String[] blocked = {"পেমেন্ট", "টাকা পাঠ", "ট্রান্সফার", "send money", "pay now",
                "শেয়ার কিন", "শেয়ার কিন", "buy stock", "sell stock", "place order",
                "অর্ডার কনফার্ম", "confirm order", "আনইনস্টল", "delete account"};
        for (String word : blocked) if (text.contains(word)) return true;
        return false;
    }
}
