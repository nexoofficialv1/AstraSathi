package com.astratechnologies.astrasathi;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class NotesRepository {
    private static final String PREFS = "astra_sathi_private";
    private static final String KEY = "notes_v1";
    private static final String SEP = "\u001E";
    private final SharedPreferences preferences;

    public NotesRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void add(String note) {
        String encoded = Base64.getEncoder().encodeToString(note.getBytes(StandardCharsets.UTF_8));
        String row = System.currentTimeMillis() + "\t" + encoded;
        String current = preferences.getString(KEY, "");
        preferences.edit().putString(KEY, row + (current.isEmpty() ? "" : SEP + current)).apply();
    }

    public List<String> list(int limit) {
        List<String> notes = new ArrayList<>();
        String current = preferences.getString(KEY, "");
        if (current == null || current.isEmpty()) return notes;
        for (String row : current.split(SEP)) {
            String[] parts = row.split("\\t", 2);
            if (parts.length != 2) continue;
            try {
                Date date = new Date(Long.parseLong(parts[0]));
                String time = new SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.getDefault()).format(date);
                String note = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                notes.add(time + " — " + note);
                if (notes.size() >= limit) break;
            } catch (Exception ignored) { }
        }
        return notes;
    }
}
