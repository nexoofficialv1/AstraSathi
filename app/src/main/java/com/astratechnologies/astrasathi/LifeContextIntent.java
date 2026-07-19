package com.astratechnologies.astrasathi;

/** Pure-Java representation of a Bengali life-context statement. */
public final class LifeContextIntent {
    public enum Type {
        NONE, FUEL_LOG, DISTANCE_LOG, MILEAGE_SET, VEHICLE_STATUS, REFUEL_NEEDED,
        COMMITMENT_SAVE, COMMITMENT_LIST, COMMITMENT_COMPLETE,
        MEDICINE_LOG, SYMPTOM_LOG, HEALTH_STATUS, LIFE_STATUS, OPEN_DASHBOARD
    }

    public final Type type;
    public final String original;
    public final String subject;
    public final String details;
    public final double amount;
    public final int eventDayOffset;
    public final int reminderDayOffset;
    public final int hour;
    public final int minute;
    public final double confidence;

    LifeContextIntent(Type type, String original, String subject, String details,
                      double amount, int eventDayOffset, int reminderDayOffset,
                      int hour, int minute, double confidence) {
        this.type = type;
        this.original = original == null ? "" : original.trim();
        this.subject = subject == null ? "" : subject.trim();
        this.details = details == null ? "" : details.trim();
        this.amount = amount;
        this.eventDayOffset = eventDayOffset;
        this.reminderDayOffset = reminderDayOffset;
        this.hour = hour;
        this.minute = minute;
        this.confidence = confidence;
    }

    static LifeContextIntent none(String original) {
        return new LifeContextIntent(Type.NONE, original, "", "", 0,
                0, 0, -1, 0, 0);
    }
}
