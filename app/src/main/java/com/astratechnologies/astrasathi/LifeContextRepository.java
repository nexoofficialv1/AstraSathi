package com.astratechnologies.astrasathi;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted, on-device event store used by the JARVIS Life Context Engine. */
public final class LifeContextRepository {
    public static final String FUEL = "fuel";
    public static final String DISTANCE = "distance";
    public static final String MILEAGE = "mileage";
    public static final String REFUEL_TASK = "refuel_task";
    public static final String COMMITMENT = "commitment";
    public static final String MEDICINE = "medicine";
    public static final String SYMPTOM = "symptom";

    public static final class Event {
        public final String id;
        public final long createdAt;
        public final long eventAt;
        public final boolean completed;
        public final String kind;
        public final double amount;
        public final double confidence;
        public final String subject;
        public final String details;
        public final String source;

        Event(String id, long createdAt, long eventAt, boolean completed, String kind,
              double amount, double confidence, String subject, String details, String source) {
            this.id = id;
            this.createdAt = createdAt;
            this.eventAt = eventAt;
            this.completed = completed;
            this.kind = kind;
            this.amount = amount;
            this.confidence = confidence;
            this.subject = subject;
            this.details = details;
            this.source = source;
        }
    }

    private static final String PREFS = "astra_sathi_life_context";
    private static final String DATA = "life_context_blob_v1";
    private static final String KEY_ALIAS = "astra_sathi_life_context_aes_v1";
    private static final int MAX_EVENTS = 1200;
    private final SharedPreferences preferences;

    public LifeContextRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized Event add(String kind, long eventAt, double amount, double confidence,
                                  String subject, String details, String source) {
        if (kind == null || kind.isEmpty() || details == null || details.trim().isEmpty()) return null;
        if (SensitiveDataFilter.isSensitive(details)) return null;
        List<Event> events = load();
        Event event = new Event(UUID.randomUUID().toString(), System.currentTimeMillis(),
                eventAt <= 0 ? System.currentTimeMillis() : eventAt, false, kind, amount,
                Math.max(0, Math.min(1, confidence)), safe(subject), details.trim(), safe(source));
        events.add(event);
        while (events.size() > MAX_EVENTS) events.remove(0);
        return save(events) ? event : null;
    }

    public synchronized List<Event> recent(String kind, int limit) {
        List<Event> all = load();
        List<Event> result = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && result.size() < limit; i--) {
            Event event = all.get(i);
            if (kind == null || kind.isEmpty() || kind.equals(event.kind)) result.add(event);
        }
        return result;
    }

    public synchronized boolean completeLatestCommitment() {
        return completeLatest(COMMITMENT);
    }

    public synchronized boolean completeLatestRefuelTask() {
        return completeLatest(REFUEL_TASK);
    }

    private boolean completeLatest(String kind) {
        List<Event> events = load();
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (kind.equals(event.kind) && !event.completed) {
                events.set(i, copyCompleted(event));
                return save(events);
            }
        }
        return false;
    }

    public synchronized void clearAll() { preferences.edit().remove(DATA).apply(); }

    public synchronized String vehicleSummary() {
        List<Event> events = load();
        Event latestFuel = latest(events, FUEL, false);
        Event latestMileage = latest(events, MILEAGE, false);
        Event refuel = latest(events, REFUEL_TASK, true);
        if (latestFuel == null && latestMileage == null && refuel == null)
            return "গাড়ি: এখনও তেল, চলা কিলোমিটার বা মাইলেজের তথ্য নেই।";

        StringBuilder out = new StringBuilder("গাড়ি: ");
        if (latestFuel != null) {
            long days = Math.max(0, (System.currentTimeMillis() - latestFuel.eventAt) / 86_400_000L);
            out.append(days == 0 ? "আজ" : days + " দিন আগে").append(" তেল ভরার তথ্য আছে");
            if (latestFuel.amount > 0) out.append(" (প্রায় ").append(formatNumber(latestFuel.amount)).append(" লিটার)");
            double distance = 0;
            for (Event event : events)
                if (DISTANCE.equals(event.kind) && event.eventAt >= latestFuel.eventAt) distance += event.amount;
            if (distance > 0) out.append("। এরপর ").append(formatNumber(distance)).append(" কিমি চলার হিসাব আছে");
            FuelEstimator.Estimate estimate = FuelEstimator.estimate(latestFuel.amount, distance,
                    latestMileage == null ? 0 : latestMileage.amount);
            if (estimate.available) {
                out.append("। আনুমানিক তেল ").append(formatNumber(estimate.remainingLitres)).append(" লিটার");
                if (estimate.refuelRecommended) out.append("—তেল নেওয়া উচিত");
            } else if ("fuel_quantity".equals(estimate.missing)) {
                out.append("। কত লিটার ভরেছিলেন না জানায় অবশিষ্ট তেল হিসাব করা যাচ্ছে না");
            } else {
                out.append("। মাইলেজ না জানায় অবশিষ্ট তেল আনুমানিক করা যাচ্ছে না");
            }
        } else if (latestMileage != null) {
            out.append("মাইলেজ ").append(formatNumber(latestMileage.amount)).append(" কিমি/লিটার রাখা আছে; তেল ভরার তথ্য নেই");
        }
        if (refuel != null && !refuel.completed) out.append("। তেল ভরার কাজটি বাকি হিসেবে রাখা আছে");
        return out.append('।').toString();
    }

    public synchronized String commitmentSummary() {
        List<Event> events = load();
        List<Event> active = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && active.size() < 5; i--) {
            Event event = events.get(i);
            if (COMMITMENT.equals(event.kind) && !event.completed) active.add(event);
        }
        if (active.isEmpty()) return "দেওয়া কথা: বর্তমানে কোনো অসম্পূর্ণ প্রতিশ্রুতি রাখা নেই।";
        StringBuilder out = new StringBuilder("দেওয়া কথা:");
        for (Event event : active) {
            out.append("\n• ");
            if (!event.subject.isEmpty()) out.append(event.subject).append("—");
            out.append(event.details).append(" (").append(formatDate(event.eventAt)).append(")");
            if (event.confidence < 0.75) out.append(" [আপনার নিশ্চিতকরণ দরকার]");
        }
        return out.toString();
    }

    public synchronized String healthSummary() {
        List<Event> events = load();
        Event medicine = latest(events, MEDICINE, false);
        Event symptom = latest(events, SYMPTOM, false);
        if (medicine == null && symptom == null)
            return "স্বাস্থ্য: সাম্প্রতিক ওষুধ বা উপসর্গের কোনো তথ্য নেই।";
        StringBuilder out = new StringBuilder("স্বাস্থ্য: ");
        if (symptom != null) out.append("সর্বশেষ বলা উপসর্গ—").append(symptom.subject)
                .append(" (").append(formatDate(symptom.eventAt)).append(")");
        if (medicine != null) {
            if (symptom != null) out.append("। ");
            out.append("সর্বশেষ ওষুধের লগ—").append(medicine.subject)
                    .append(" (").append(formatDate(medicine.eventAt)).append(")");
        }
        out.append("। এটি আপনার দেওয়া তথ্যের সারাংশ, চিকিৎসা-নির্ণয় নয়। এখন উপসর্গ কমেছে, একই আছে, নাকি বেড়েছে বলুন।");
        return out.toString();
    }

    public synchronized String lifeSummary() {
        return vehicleSummary() + "\n\n" + commitmentSummary() + "\n\n" + healthSummary();
    }

    private Event latest(List<Event> events, String kind, boolean incompleteOnly) {
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (kind.equals(event.kind) && (!incompleteOnly || !event.completed)) return event;
        }
        return null;
    }

    private Event copyCompleted(Event event) {
        return new Event(event.id, event.createdAt, event.eventAt, true, event.kind,
                event.amount, event.confidence, event.subject, event.details, event.source);
    }

    private String formatDate(long value) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM, hh:mm a", new Locale("bn", "IN"));
        return format.format(new Date(value));
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.US, "%.1f", value);
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }

    private List<Event> load() {
        String encrypted = preferences.getString(DATA, "");
        if (encrypted == null || encrypted.isEmpty()) return new ArrayList<>();
        try {
            String[] pair = encrypted.split(":", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(128, Base64.getDecoder().decode(pair[0])));
            String plain = new String(cipher.doFinal(Base64.getDecoder().decode(pair[1])), StandardCharsets.UTF_8);
            List<Event> events = new ArrayList<>();
            for (String line : plain.split("\\n")) {
                if (line.isEmpty()) continue;
                String[] f = line.split("\\|", 10);
                if (f.length != 10) continue;
                events.add(new Event(f[0], Long.parseLong(f[1]), Long.parseLong(f[2]), "1".equals(f[3]),
                        f[4], Double.parseDouble(f[5]), Double.parseDouble(f[6]), decode(f[7]), decode(f[8]), decode(f[9])));
            }
            return events;
        } catch (Exception ignored) { return new ArrayList<>(); }
    }

    private boolean save(List<Event> events) {
        try {
            StringBuilder plain = new StringBuilder();
            for (Event event : events) {
                if (plain.length() > 0) plain.append('\n');
                plain.append(event.id).append('|').append(event.createdAt).append('|').append(event.eventAt)
                        .append('|').append(event.completed ? '1' : '0').append('|').append(event.kind)
                        .append('|').append(event.amount).append('|').append(event.confidence)
                        .append('|').append(encode(event.subject)).append('|').append(encode(event.details))
                        .append('|').append(encode(event.source));
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            String value = Base64.getEncoder().encodeToString(cipher.getIV()) + ":"
                    + Base64.getEncoder().encodeToString(cipher.doFinal(plain.toString().getBytes(StandardCharsets.UTF_8)));
            preferences.edit().putString(DATA, value).apply();
            return true;
        } catch (Exception ignored) { return false; }
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(safe(value).getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return (SecretKey) store.getKey(KEY_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
