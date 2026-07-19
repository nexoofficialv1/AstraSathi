package com.astratechnologies.astrasathi;

import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Executes parsed life-context statements and produces transparent Bengali summaries. */
public final class LifeContextEngine {
    private LifeContextEngine() { }

    public static DeviceController.Result execute(Context context, Command command) {
        if (command.type == Command.Type.LIFE_CONTEXT_OPEN) {
            Intent open = new Intent(context, LifeContextActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(open);
            return new DeviceController.Result(true, "JARVIS Life Context খুলেছি।");
        }
        SecureMemoryRepository memory = new SecureMemoryRepository(context);
        if (!memory.isEnabled())
            return new DeviceController.Result(false, "এই কাজের জন্য Personal Memory চালু করা দরকার। তথ্য শুধু encrypted on-device storage-এ থাকবে।");
        LifeContextRepository repository = new LifeContextRepository(context);
        List<LifeContextIntent> intents = new LifeContextParser().parseAll(command.original);
        if (intents.isEmpty()) return new DeviceController.Result(false, "এই Life Context কথাটি বুঝতে পারিনি।");
        if (intents.size() == 1) return executeIntent(context, repository, memory, intents.get(0));
        StringBuilder messages = new StringBuilder();
        boolean success = true;
        for (LifeContextIntent intent : intents) {
            DeviceController.Result result = executeIntent(context, repository, memory, intent);
            if (messages.length() > 0) messages.append("\n");
            messages.append("• ").append(result.message);
            success &= result.success;
        }
        return new DeviceController.Result(success, messages.toString());
    }

    private static DeviceController.Result executeIntent(Context context, LifeContextRepository repository,
                                                         SecureMemoryRepository memory, LifeContextIntent intent) {
        long eventAt = eventTime(intent.eventDayOffset, intent.hour, intent.minute);
        return switch (intent.type) {
            case FUEL_LOG -> saveFuel(repository, intent, eventAt);
            case DISTANCE_LOG -> saveDistance(repository, intent, eventAt);
            case MILEAGE_SET -> saveMileage(repository, intent, eventAt);
            case VEHICLE_STATUS -> new DeviceController.Result(true, repository.vehicleSummary());
            case REFUEL_NEEDED -> saveRefuelTask(context, repository, memory, intent);
            case COMMITMENT_SAVE -> saveCommitment(context, repository, memory, intent);
            case COMMITMENT_LIST -> new DeviceController.Result(true, repository.commitmentSummary());
            case COMMITMENT_COMPLETE -> completeCommitment(repository, memory);
            case MEDICINE_LOG -> saveMedicine(repository, intent, eventAt);
            case SYMPTOM_LOG -> saveSymptom(repository, intent, eventAt);
            case HEALTH_STATUS -> new DeviceController.Result(true, repository.healthSummary());
            case LIFE_STATUS -> new DeviceController.Result(true, repository.lifeSummary());
            default -> new DeviceController.Result(false, "এই Life Context কথাটি বুঝতে পারিনি।");
        };
    }

    public static boolean requiresMemory(Command.Type type) {
        return switch (type) {
            case VEHICLE_FUEL_LOG, VEHICLE_DISTANCE_LOG, VEHICLE_MILEAGE_SET,
                    VEHICLE_STATUS, VEHICLE_REFUEL_NEEDED, COMMITMENT_SAVE,
                    COMMITMENT_LIST, COMMITMENT_COMPLETE, MEDICINE_LOG,
                    SYMPTOM_LOG, HEALTH_STATUS, LIFE_STATUS -> true;
            default -> false;
        };
    }

    private static DeviceController.Result saveFuel(LifeContextRepository repository,
                                                     LifeContextIntent intent, long eventAt) {
        LifeContextRepository.Event saved = repository.add(LifeContextRepository.FUEL, eventAt,
                intent.amount, intent.confidence, "গাড়ি", intent.details, "voice");
        if (saved == null) return failed();
        if (intent.amount <= 0)
            return new DeviceController.Result(true, "তেল ভরার ঘটনাটি মনে রেখেছি। কত লিটার ভরেছিলেন বললে অবশিষ্ট তেল হিসাব করতে পারব।");
        return new DeviceController.Result(true, format(intent.amount) + " লিটার তেল ভরার তথ্যটি সময়সহ মনে রেখেছি।");
    }

    private static DeviceController.Result saveDistance(LifeContextRepository repository,
                                                         LifeContextIntent intent, long eventAt) {
        if (intent.amount <= 0) return new DeviceController.Result(false, "কত কিলোমিটার চলেছেন সেটি বুঝতে পারিনি।");
        LifeContextRepository.Event saved = repository.add(LifeContextRepository.DISTANCE, eventAt,
                intent.amount, intent.confidence, "গাড়ি", intent.details, "voice");
        return saved == null ? failed() : new DeviceController.Result(true,
                format(intent.amount) + " কিলোমিটার চলার হিসাব রেখেছি। " + repository.vehicleSummary());
    }

    private static DeviceController.Result saveMileage(LifeContextRepository repository,
                                                        LifeContextIntent intent, long eventAt) {
        if (intent.amount <= 0) return new DeviceController.Result(false, "গাড়ির মাইলেজ কত সেটি বুঝতে পারিনি। যেমন বলুন—গাড়ি লিটারে ১৫ কিলোমিটার মাইলেজ দেয়।");
        LifeContextRepository.Event saved = repository.add(LifeContextRepository.MILEAGE, eventAt,
                intent.amount, intent.confidence, "গাড়ি", intent.details, "voice");
        return saved == null ? failed() : new DeviceController.Result(true,
                "গাড়ির মাইলেজ " + format(intent.amount) + " কিলোমিটার প্রতি লিটার হিসেবে রেখেছি।");
    }

    private static DeviceController.Result saveRefuelTask(Context context, LifeContextRepository repository,
                                                           SecureMemoryRepository memory, LifeContextIntent intent) {
        long when = defaultTodayReminder();
        LifeContextRepository.Event event = repository.add(LifeContextRepository.REFUEL_TASK,
                when, 0, intent.confidence, "গাড়ি", "গাড়িতে তেল ভরতে হবে", "voice");
        SecureMemoryRepository.MemoryRecord task = memory.rememberRecord("যানবাহনের কাজ",
                "গাড়িতে তেল ভরতে হবে", when);
        if (event == null || task == null) return failed();
        ReminderScheduler.schedule(context, task.id, task.content, task.remindAt);
        return new DeviceController.Result(true, "আজ গাড়িতে তেল ভরার কাজ মনে রেখেছি। "
                + timeText(when) + " আবার মনে করিয়ে দেব। অন্য সময় চাইলে সময়টি বলুন।");
    }

    private static DeviceController.Result saveCommitment(Context context, LifeContextRepository repository,
                                                           SecureMemoryRepository memory, LifeContextIntent intent) {
        String taskText = intent.details.isEmpty() ? intent.original : intent.details;
        long when = commitmentReminder(intent.reminderDayOffset, intent.hour, intent.minute);
        LifeContextRepository.Event event = repository.add(LifeContextRepository.COMMITMENT,
                when, 0, intent.confidence, intent.subject, taskText, "voice-confirmed-summary");
        String reminder = (intent.subject.isEmpty() ? "দেওয়া কথা" : intent.subject + "-কে দেওয়া কথা") + ": " + taskText;
        SecureMemoryRepository.MemoryRecord task = memory.rememberRecord("প্রতিশ্রুতি", reminder, when);
        if (event == null || task == null) return failed();
        ReminderScheduler.schedule(context, task.id, task.content, task.remindAt);
        String who = intent.subject.isEmpty() ? "কাকে বলেছেন সেটি স্পষ্ট নয়। " : intent.subject + "-কে দেওয়া কথাটি ";
        return new DeviceController.Result(true, who + "মনে রেখেছি। " + timeText(when)
                + " মনে করিয়ে দেব। ভুল বুঝে থাকলে বলুন—শেষ প্রতিশ্রুতি সম্পন্ন।");
    }

    private static DeviceController.Result completeCommitment(LifeContextRepository repository,
                                                               SecureMemoryRepository memory) {
        boolean done = repository.completeLatestCommitment();
        if (done) memory.completeLatestByType("প্রতিশ্রুতি");
        return new DeviceController.Result(done, done ? "সর্বশেষ দেওয়া কথাটি সম্পন্ন হিসেবে রেখেছি।"
                : "অসম্পূর্ণ কোনো প্রতিশ্রুতি পাইনি।");
    }

    private static DeviceController.Result saveMedicine(LifeContextRepository repository,
                                                         LifeContextIntent intent, long eventAt) {
        LifeContextRepository.Event saved = repository.add(LifeContextRepository.MEDICINE, eventAt,
                0, intent.confidence, intent.subject.isEmpty() ? "ওষুধ" : intent.subject,
                intent.details, "voice");
        return saved == null ? failed() : new DeviceController.Result(true,
                "ওষুধ খাওয়ার সময়টি লগ করেছি। ওষুধের সঠিক নাম ও ডোজ বললে আরও নির্ভুলভাবে মনে রাখতে পারব। এখন উপসর্গ কমেছে, একই আছে, নাকি বেড়েছে?");
    }

    private static DeviceController.Result saveSymptom(LifeContextRepository repository,
                                                        LifeContextIntent intent, long eventAt) {
        LifeContextRepository.Event saved = repository.add(LifeContextRepository.SYMPTOM, eventAt,
                0, intent.confidence, intent.subject, intent.details, "voice");
        return saved == null ? failed() : new DeviceController.Result(true,
                intent.subject + " উপসর্গটি সময়সহ লিখে রেখেছি। ব্যথা বা অস্বস্তি ১ থেকে ১০-এর মধ্যে কত বলুন। গুরুতর বা দ্রুত বাড়লে চিকিৎসা সহায়তা নিন।");
    }

    private static long eventTime(int dayOffset, int hour, int minute) {
        Calendar value = Calendar.getInstance();
        value.add(Calendar.DAY_OF_YEAR, dayOffset);
        if (hour >= 0) {
            value.set(Calendar.HOUR_OF_DAY, hour);
            value.set(Calendar.MINUTE, Math.max(0, minute));
            value.set(Calendar.SECOND, 0);
            value.set(Calendar.MILLISECOND, 0);
        }
        return value.getTimeInMillis();
    }

    private static long commitmentReminder(int dayOffset, int hour, int minute) {
        Calendar value = Calendar.getInstance();
        if (dayOffset > 0) value.add(Calendar.DAY_OF_YEAR, dayOffset);
        if (hour >= 0) {
            value.set(Calendar.HOUR_OF_DAY, hour);
            value.set(Calendar.MINUTE, minute);
        } else if (dayOffset > 0) {
            value.set(Calendar.HOUR_OF_DAY, 9);
            value.set(Calendar.MINUTE, 0);
        } else {
            value.add(Calendar.HOUR_OF_DAY, 2);
        }
        value.set(Calendar.SECOND, 0);
        if (value.getTimeInMillis() <= System.currentTimeMillis()) value.add(Calendar.DAY_OF_YEAR, 1);
        return value.getTimeInMillis();
    }

    private static long defaultTodayReminder() {
        Calendar value = Calendar.getInstance();
        value.set(Calendar.HOUR_OF_DAY, 18);
        value.set(Calendar.MINUTE, 0);
        value.set(Calendar.SECOND, 0);
        if (value.getTimeInMillis() <= System.currentTimeMillis() + 5 * 60_000L) {
            value = Calendar.getInstance();
            value.add(Calendar.MINUTE, 30);
        }
        return value.getTimeInMillis();
    }

    private static String timeText(long value) {
        return new SimpleDateFormat("dd MMM, hh:mm a", new Locale("bn", "IN")).format(value);
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.US, "%.1f", value);
    }

    private static DeviceController.Result failed() {
        return new DeviceController.Result(false, "তথ্যটি encrypted Life Context-এ রাখা গেল না। আবার চেষ্টা করুন।");
    }
}
