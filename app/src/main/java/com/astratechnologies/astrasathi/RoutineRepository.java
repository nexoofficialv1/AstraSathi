package com.astratechnologies.astrasathi;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class RoutineRepository {
    public static final class Routine {
        public final String name;
        public final String command;
        Routine(String name, String command) { this.name = name; this.command = command; }
    }

    private static final String TYPE = "রুটিন";
    private static final String SEPARATOR = "\u001D";
    private final SecureMemoryRepository memory;

    public RoutineRepository(Context context) { memory = new SecureMemoryRepository(context); }

    public boolean save(String name, String command) {
        if (!memory.isEnabled() || name == null || name.trim().isEmpty()
                || command == null || command.trim().isEmpty()) return false;
        return memory.remember(TYPE, name.trim() + SEPARATOR + command.trim(), 0);
    }

    public Routine find(String name) {
        String wanted = BengaliText.normalize(name);
        for (SecureMemoryRepository.MemoryRecord record : memory.byType(TYPE, 100)) {
            Routine routine = decode(record.content);
            if (routine != null && (BengaliText.normalize(routine.name).equals(wanted)
                    || BengaliText.normalize(routine.name).contains(wanted)
                    || wanted.contains(BengaliText.normalize(routine.name)))) return routine;
        }
        return null;
    }

    public List<Routine> list() {
        List<Routine> result = new ArrayList<>();
        for (SecureMemoryRepository.MemoryRecord record : memory.byType(TYPE, 30)) {
            Routine routine = decode(record.content); if (routine != null) result.add(routine);
        }
        return result;
    }

    private Routine decode(String value) {
        String[] pair = value.split(SEPARATOR, 2);
        return pair.length == 2 ? new Routine(pair[0], pair[1]) : null;
    }
}
