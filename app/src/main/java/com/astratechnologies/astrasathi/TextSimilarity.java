package com.astratechnologies.astrasathi;

import java.util.LinkedHashSet;
import java.util.Set;

/** Small deterministic matcher for Bengali/English labels whose spelling changes slightly. */
public final class TextSimilarity {
    private TextSimilarity() { }

    public static double score(String query, String candidate) {
        String a = clean(query);
        String b = clean(candidate);
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return 1;
        if (b.startsWith(a) || a.startsWith(b)) return 0.92;
        if (b.contains(a) || a.contains(b)) return 0.86;

        double tokenScore = tokenOverlap(a, b);
        double editScore = 1.0 - ((double) editDistance(a, b) / Math.max(a.length(), b.length()));
        return Math.max(tokenScore, editScore * 0.88);
    }

    public static boolean isConfident(String query, String candidate) {
        return score(query, candidate) >= 0.68;
    }

    private static String clean(String value) {
        return BengaliText.normalize(value).replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static double tokenOverlap(String a, String b) {
        Set<String> left = tokens(a);
        Set<String> right = tokens(b);
        if (left.isEmpty() || right.isEmpty()) return 0;
        int common = 0;
        for (String token : left) if (right.contains(token)) common++;
        int total = left.size() + right.size() - common;
        return total == 0 ? 0 : 0.55 + 0.35 * ((double) common / total);
    }

    private static Set<String> tokens(String value) {
        Set<String> result = new LinkedHashSet<>();
        for (String token : value.split("\\s+")) if (token.length() > 1) result.add(token);
        return result;
    }

    private static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            previous = current;
        }
        return previous[b.length()];
    }
}
