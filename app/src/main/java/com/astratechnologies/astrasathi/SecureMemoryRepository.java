package com.astratechnologies.astrasathi;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureMemoryRepository {
    public static final class MemoryRecord {
        public final String id;
        public final long createdAt;
        public final long remindAt;
        public final boolean completed;
        public final String type;
        public final String content;

        MemoryRecord(String id, long createdAt, long remindAt, boolean completed, String type, String content) {
            this.id = id; this.createdAt = createdAt; this.remindAt = remindAt;
            this.completed = completed; this.type = type; this.content = content;
        }
    }

    private static final String PREFS = "astra_sathi_secure_memory";
    private static final String ENABLED = "memory_enabled";
    private static final String DATA = "memory_blob_v1";
    private static final String KEY_ALIAS = "astra_sathi_memory_aes_v1";
    private static final int MAX_RECORDS = 1000;
    private final SharedPreferences preferences;

    public SecureMemoryRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() { return preferences.getBoolean(ENABLED, false); }
    public void setEnabled(boolean enabled) { preferences.edit().putBoolean(ENABLED, enabled).apply(); }

    public synchronized boolean remember(String type, String content, long remindAt) {
        return rememberRecord(type, content, remindAt) != null;
    }

    public synchronized MemoryRecord rememberRecord(String type, String content, long remindAt) {
        if (!isEnabled() || content == null || content.trim().isEmpty() || SensitiveDataFilter.isSensitive(content)) return null;
        List<MemoryRecord> records = load();
        MemoryRecord created = new MemoryRecord(UUID.randomUUID().toString(), System.currentTimeMillis(),
                remindAt, false, type == null ? "তথ্য" : type, content.trim());
        records.add(created);
        while (records.size() > MAX_RECORDS) records.remove(0);
        return save(records) ? created : null;
    }

    public boolean rememberCommand(String command) {
        return remember("কমান্ড", command, 0);
    }

    public synchronized List<MemoryRecord> recent(int limit) {
        List<MemoryRecord> records = load();
        Collections.reverse(records);
        if (records.size() > limit) return new ArrayList<>(records.subList(0, limit));
        return records;
    }

    public synchronized List<MemoryRecord> pending(long now) {
        List<MemoryRecord> result = new ArrayList<>();
        for (MemoryRecord record : load()) {
            if (!record.completed && record.remindAt > 0 && record.remindAt <= now) result.add(record);
        }
        return result;
    }

    public synchronized List<MemoryRecord> scheduled() {
        List<MemoryRecord> result = new ArrayList<>();
        for (MemoryRecord record : load()) if (!record.completed && record.remindAt > 0) result.add(record);
        return result;
    }

    public synchronized List<MemoryRecord> byType(String type, int limit) {
        List<MemoryRecord> records = load();
        List<MemoryRecord> result = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0 && result.size() < limit; i--) {
            MemoryRecord record = records.get(i);
            if (record.type.equals(type)) result.add(record);
        }
        return result;
    }

    public synchronized void markCompleted(String id) {
        List<MemoryRecord> changed = new ArrayList<>();
        for (MemoryRecord record : load()) changed.add(record.id.equals(id)
                ? new MemoryRecord(record.id, record.createdAt, record.remindAt, true, record.type, record.content)
                : record);
        save(changed);
    }

    public synchronized boolean completeLatestTask() {
        List<MemoryRecord> records = load();
        for (int i = records.size() - 1; i >= 0; i--) {
            MemoryRecord record = records.get(i);
            if (!record.completed && record.remindAt > 0) {
                records.set(i, new MemoryRecord(record.id, record.createdAt, record.remindAt,
                        true, record.type, record.content));
                return save(records);
            }
        }
        return false;
    }

    public synchronized boolean completeLatestByType(String type) {
        if (type == null || type.isEmpty()) return false;
        List<MemoryRecord> records = load();
        for (int i = records.size() - 1; i >= 0; i--) {
            MemoryRecord record = records.get(i);
            if (type.equals(record.type) && !record.completed) {
                records.set(i, new MemoryRecord(record.id, record.createdAt, record.remindAt,
                        true, record.type, record.content));
                return save(records);
            }
        }
        return false;
    }

    public synchronized MemoryRecord find(String id) {
        if (id == null) return null;
        for (MemoryRecord record : load()) if (record.id.equals(id)) return record;
        return null;
    }

    public synchronized void clearAll() { preferences.edit().remove(DATA).apply(); }

    private List<MemoryRecord> load() {
        String encrypted = preferences.getString(DATA, "");
        if (encrypted == null || encrypted.isEmpty()) return new ArrayList<>();
        try {
            String[] pair = encrypted.split(":", 2);
            byte[] iv = Base64.getDecoder().decode(pair[0]);
            byte[] cipherText = Base64.getDecoder().decode(pair[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            String plain = new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
            List<MemoryRecord> records = new ArrayList<>();
            for (String line : plain.split("\\n")) {
                if (line.isEmpty()) continue;
                String[] fields = line.split("\\|", 6);
                if (fields.length != 6) continue;
                records.add(new MemoryRecord(fields[0], Long.parseLong(fields[1]), Long.parseLong(fields[2]),
                        "1".equals(fields[3]), fields[4],
                        new String(Base64.getDecoder().decode(fields[5]), StandardCharsets.UTF_8)));
            }
            return records;
        } catch (Exception ignored) { return new ArrayList<>(); }
    }

    private boolean save(List<MemoryRecord> records) {
        try {
            StringBuilder plain = new StringBuilder();
            for (MemoryRecord record : records) {
                if (plain.length() > 0) plain.append('\n');
                plain.append(record.id).append('|').append(record.createdAt).append('|').append(record.remindAt)
                        .append('|').append(record.completed ? '1' : '0').append('|').append(record.type).append('|')
                        .append(Base64.getEncoder().encodeToString(record.content.getBytes(StandardCharsets.UTF_8)));
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            String value = Base64.getEncoder().encodeToString(cipher.getIV()) + ":"
                    + Base64.getEncoder().encodeToString(cipher.doFinal(plain.toString().getBytes(StandardCharsets.UTF_8)));
            preferences.edit().putString(DATA, value).apply();
            return true;
        } catch (Exception ignored) { return false; }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return (SecretKey) store.getKey(KEY_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

}
