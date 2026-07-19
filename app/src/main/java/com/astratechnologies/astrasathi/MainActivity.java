package com.astratechnologies.astrasathi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements RecognitionListener {
    private static final int REQUEST_AUDIO = 100;
    private static final int REQUEST_CONTACTS = 101;
    private static final int REQUEST_CAMERA = 102;

    private TextView statusText;
    private TextView instructionText;
    private TextView resultText;
    private TextView accessStatusText;
    private TextView lifeStatusText;
    private EditText commandInput;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private final CommandRouter router = new CommandRouter();
    private NotesRepository notes;
    private SecureMemoryRepository memory;
    private ContactResolver contacts;
    private Command pendingPermissionCommand;
    private boolean isListening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        notes = new NotesRepository(this);
        memory = new SecureMemoryRepository(this);
        contacts = new ContactResolver(this);
        setupSpeech();
        setupActions();
        handleLaunchIntent(getIntent());
    }

    private void bindViews() {
        statusText = findViewById(R.id.statusText);
        instructionText = findViewById(R.id.instructionText);
        resultText = findViewById(R.id.resultText);
        accessStatusText = findViewById(R.id.accessStatusText);
        lifeStatusText = findViewById(R.id.lifeStatusText);
        commandInput = findViewById(R.id.commandInput);
    }

    private void setupSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.forLanguageTag("bn-IN"));
                if (result < 0) tts.setLanguage(new Locale("bn"));
                tts.setSpeechRate(0.92f);
            }
        });
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
        } else {
            showResult("এই ফোনে ভয়েস শনাক্তকরণ সেবা পাওয়া যায়নি। লিখে কমান্ড দিন।", true);
        }
    }

    private void setupActions() {
        findViewById(R.id.micButton).setOnClickListener(v -> toggleListening());
        findViewById(R.id.runButton).setOnClickListener(v -> runTypedCommand());
        quick(R.id.quickAlarm, "সকাল ৭টায় অ্যালার্ম দাও");
        quick(R.id.quickNote, "নোট লেখো আজকের গুরুত্বপূর্ণ কাজ");
        quick(R.id.quickWhatsApp, "রাহুলকে হোয়াটসঅ্যাপে মেসেজ পাঠাও আমি আসছি");
        quick(R.id.quickWeather, "কালনার আজকের আবহাওয়া দেখাও");
        quick(R.id.quickReminder, "আগামীকাল সকাল ১০টায় অফিসের রিমাইন্ডার দাও");
        findViewById(R.id.quickHelp).setOnClickListener(v -> execute(router.parse("কী বলতে পারি")));
        findViewById(R.id.accessCenterButton).setOnClickListener(v ->
                startActivity(new Intent(this, FullAccessActivity.class)));
        findViewById(R.id.lifeContextButton).setOnClickListener(v ->
                startActivity(new Intent(this, LifeContextActivity.class)));
    }

    private void quick(int viewId, String command) {
        findViewById(viewId).setOnClickListener(v -> {
            commandInput.setText(command);
            commandInput.setSelection(command.length());
        });
    }

    private void runTypedCommand() {
        String text = commandInput.getText().toString().trim();
        if (text.isEmpty()) {
            speakAndShow("প্রথমে একটি বাংলা কমান্ড বলুন অথবা লিখুন।", true);
            return;
        }
        hideKeyboard();
        execute(router.parse(text));
    }

    private void toggleListening() {
        if (recognizer == null) return;
        if (isListening) {
            recognizer.stopListening();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            return;
        }
        startListening();
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-IN");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizer.startListening(intent);
        isListening = true;
        setStatus("●  শুনছি…", "বাংলায় স্পষ্টভাবে কমান্ডটি বলুন", true);
    }

    private void execute(Command command) {
        setStatus("●  কমান্ডটি বুঝেছি", "প্রয়োজনীয় কাজ প্রস্তুত করছি", false);
        if (memory.isEnabled() && command.type != Command.Type.UNKNOWN
                && command.type != Command.Type.MEMORY_SAVE && command.type != Command.Type.MEMORY_FORGET_ALL)
            memory.rememberCommand(command.original);
        switch (command.type) {
            case NOTE_SAVE -> saveNote(command);
            case NOTE_SHOW -> showNotes();
            case MEMORY_SAVE -> saveMemory(command);
            case MEMORY_RECALL -> showMemory();
            case MEMORY_PENDING -> showPendingMemory();
            case MEMORY_COMPLETE -> {
                boolean completed = memory.completeLatestTask();
                speakAndShow(completed ? "সর্বশেষ অসম্পূর্ণ কাজটি সম্পন্ন হিসেবে চিহ্নিত করেছি।"
                        : "সম্পন্ন করার মতো কোনো pending কাজ পাইনি।", !completed);
            }
            case MEMORY_FORGET_ALL -> confirm("Astra Sathi-র সব Personal Memory স্থায়ীভাবে মুছে দেব?",
                    () -> {
                        memory.clearAll();
                        new LifeContextRepository(this).clearAll();
                        speakAndShow("সব Personal Memory ও Life Context মুছে দিয়েছি।", false);
                    });
            case WORKFLOW -> prepareWorkflow(command.content);
            case ROUTINE_SAVE -> saveLastWorkflowAsRoutine(command.target);
            case ROUTINE_RUN -> runRoutine(command.target);
            case ROUTINE_LIST -> listRoutines();
            case MACRO_START, MACRO_STOP, MACRO_CANCEL -> runTeachCommand(command);
            case TRADE_ORDER -> openFinancialPreview(command.original);
            case VEHICLE_FUEL_LOG, VEHICLE_DISTANCE_LOG, VEHICLE_MILEAGE_SET,
                    VEHICLE_STATUS, VEHICLE_REFUEL_NEEDED, COMMITMENT_SAVE,
                    COMMITMENT_LIST, COMMITMENT_COMPLETE, MEDICINE_LOG,
                    SYMPTOM_LOG, HEALTH_STATUS, LIFE_STATUS, LIFE_CONTEXT_OPEN -> runLifeContext(command);
            case CALL, SMS, WHATSAPP -> prepareContactAction(command);
            case ALARM -> prepareAlarm(command);
            case REMINDER -> prepareReminder(command);
            case OPEN_APP -> openApp(command.target);
            case WEB_SEARCH -> webSearch(command.content);
            case WEATHER -> webSearch((command.target.isEmpty() ? "আমার এলাকার" : command.target) + " আজকের আবহাওয়া");
            case NAVIGATE -> navigate(command.target);
            case TORCH_ON -> prepareTorch(command, true);
            case TORCH_OFF -> prepareTorch(command, false);
            case HOME, BACK, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, LOCK_SCREEN,
                    SCREENSHOT, CLICK_TEXT, CLICK_NUMBER, ACTIONS_LIST, TYPE_TEXT, READ_SCREEN, SCROLL_UP, SCROLL_DOWN,
                    SWIPE_LEFT, SWIPE_RIGHT, VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE,
                    MEDIA_PLAY_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS, NOTIFICATION_READ,
                    NOTIFICATION_OPEN, NOTIFICATION_DISMISS, NOTIFICATION_REPLY,
                    APP_NOTIFICATION_READ, APP_NOTIFICATION_OPEN, APP_NOTIFICATION_REPLY,
                    CALL_ANSWER, CALL_END, CALL_WHO, CALL_SPEAKER_ON, CALL_SPEAKER_OFF,
                    CALL_MUTE, CALL_UNMUTE,
                    WIFI_CONTROL, BLUETOOTH_CONTROL, LOCATION_SETTINGS, SECURITY_SETTINGS,
                    PRIVACY_SETTINGS, DISPLAY_SETTINGS, SOUND_SETTINGS, APP_INFO, UNINSTALL_APP,
                    BRIGHTNESS, SCREEN_TIMEOUT, AUTO_ROTATE_ON, AUTO_ROTATE_OFF,
                    RINGER_NORMAL, RINGER_VIBRATE, RINGER_SILENT -> runDeviceCommand(command);
            case FULL_ACCESS -> startActivity(new Intent(this, FullAccessActivity.class));
            case HELP -> showHelp();
            default -> speakAndShow("দুঃখিত, এই কমান্ডটি এখনও বুঝতে পারিনি। ‘কী বলতে পারি’ চাপলে উদাহরণ দেখতে পাবেন।", true);
        }
    }

    private void prepareWorkflow(String source) {
        AutomationWorkflow workflow = new WorkflowPlanner().plan(source);
        if (!workflow.isValid()) { speakAndShow(workflow.error, true); return; }
        Runnable run = () -> runWorkflow(workflow);
        if (workflow.requiresConfirmation())
            confirm("এই multi-step workflow-এ sensitive action আছে। সব ধাপ দেখুন—\n\n" + workflow.summary(), run);
        else run.run();
    }

    private void runWorkflow(AutomationWorkflow workflow) {
        WorkflowRunner.get().run(this, workflow, new WorkflowRunner.Callback() {
            @Override public void onProgress(int current, int total, String message) {
                showResult("Workflow চলছে — ধাপ " + current + "/" + total + "\n" + message, false);
            }
            @Override public void onComplete(boolean success, String message) {
                if (memory.isEnabled()) memory.remember("অটোমেশন লগ",
                        (success ? "সফল: " : "ব্যর্থ: ") + workflow.source + " — " + message, 0);
                speakAndShow(message, !success);
            }
        });
    }

    private void saveLastWorkflowAsRoutine(String name) {
        String source = WorkflowRunner.getLastSuccessfulSource();
        if (source.isEmpty()) { speakAndShow("আগে অন্তত একটি multi-step workflow সফলভাবে চালান।", true); return; }
        if (!memory.isEnabled()) {
            speakAndShow("Routine সংরক্ষণের জন্য Full Access Setup থেকে Personal Memory চালু করুন।", true); return;
        }
        boolean saved = new RoutineRepository(this).save(name, source);
        speakAndShow(saved ? "‘" + name + "’ Routine হিসেবে মনে রেখেছি।" : "Routine সংরক্ষণ করা যায়নি।", !saved);
    }

    private void runRoutine(String name) {
        RoutineRepository.Routine routine = new RoutineRepository(this).find(name);
        if (routine == null) { speakAndShow("‘" + name + "’ নামে কোনো Routine পাইনি।", true); return; }
        prepareWorkflow(routine.command);
    }

    private void listRoutines() {
        List<RoutineRepository.Routine> routines = new RoutineRepository(this).list();
        if (routines.isEmpty()) { speakAndShow("কোনো সংরক্ষিত Routine নেই।", false); return; }
        StringBuilder out = new StringBuilder("সংরক্ষিত Routine—\n\n");
        for (RoutineRepository.Routine routine : routines) out.append("• ").append(routine.name).append('\n');
        showResult(out.toString().trim(), false);
        speak(routines.size() + "টি Routine পাওয়া গেছে।");
    }

    private void runTeachCommand(Command command) {
        MacroRecorder.Result result;
        if (command.type == Command.Type.MACRO_START) {
            SathiAccessibilityService service = SathiAccessibilityService.get();
            result = MacroRecorder.get().start(this, service == null ? "" : service.getCurrentPackageName());
        } else if (command.type == Command.Type.MACRO_STOP) {
            result = MacroRecorder.get().stopAndSave(this, command.target);
        } else {
            result = MacroRecorder.get().cancel();
        }
        speakAndShow(result.message, !result.success);
    }

    private void openFinancialPreview(String raw) {
        Intent intent = new Intent(this, FinancialActionActivity.class).putExtra("trade_command", raw);
        startActivity(intent);
    }

    private void runLifeContext(Command command) {
        if (LifeContextEngine.requiresMemory(command.type) && !memory.isEnabled()) {
            new AlertDialog.Builder(this).setTitle("Life Context-এর অনুমতি")
                    .setMessage("গাড়ি, দেওয়া কথা, ওষুধ ও উপসর্গ সময়সহ মনে রাখতে encrypted Personal Memory চালু করতে হবে। তথ্য ফোনের মধ্যেই থাকবে।")
                    .setPositiveButton("চালু করুন", (dialog, which) -> {
                        memory.setEnabled(true);
                        runLifeContext(command);
                    }).setNegativeButton("এখন নয়", null).show();
            return;
        }
        DeviceController.Result result = LifeContextEngine.execute(this, command);
        speakAndShow(result.message, !result.success);
        updateLifePreview();
    }

    private void saveMemory(Command command) {
        if (!memory.isEnabled()) {
            new AlertDialog.Builder(this).setTitle("Personal Memory বন্ধ আছে")
                    .setMessage("আপনার অনুমতি দিলে বলা তথ্য encrypted private storage-এ মনে রাখা হবে। এখন চালু করবেন?")
                    .setPositiveButton("চালু করুন", (dialog, which) -> {
                        memory.setEnabled(true);
                        saveMemory(command);
                    }).setNegativeButton("না", null).show();
            return;
        }
        if (command.content.isEmpty()) {
            speakAndShow("কী মনে রাখব সেটি বলুন। যেমন—মনে রেখো, প্রতি সোমবার রিপোর্ট পাঠাতে হবে।", true);
            return;
        }
        if (memory.remember("ব্যক্তিগত তথ্য", command.content, 0))
            speakAndShow("মনে রেখেছি: “" + command.content + "”", false);
        else speakAndShow("তথ্যটি সংরক্ষণ করা যায়নি। OTP, PIN বা password Personal Memory-তে রাখা হয় না।", true);
    }

    private void showMemory() {
        if (!memory.isEnabled()) { speakAndShow("Personal Memory বন্ধ আছে। Full Access Setup থেকে চালু করতে পারবেন।", true); return; }
        List<SecureMemoryRepository.MemoryRecord> records = memory.recent(12);
        if (records.isEmpty()) { speakAndShow("এখনও কোনো Personal Memory সংরক্ষিত নেই।", false); return; }
        StringBuilder out = new StringBuilder("আমি সাম্প্রতিক যেগুলো মনে রেখেছি—\n\n");
        for (SecureMemoryRepository.MemoryRecord record : records)
            out.append("• ").append(record.content).append('\n');
        showResult(out.toString().trim(), false);
        speak("সাম্প্রতিক " + records.size() + "টি স্মৃতি পর্দায় দেখাচ্ছি।");
    }

    private void showPendingMemory() {
        if (!memory.isEnabled()) { speakAndShow("Personal Memory বন্ধ আছে।", true); return; }
        List<SecureMemoryRepository.MemoryRecord> records = memory.pending(System.currentTimeMillis());
        if (records.isEmpty()) { speakAndShow("এই মুহূর্তে সময় পেরিয়ে যাওয়া কোনো অসম্পূর্ণ কাজ পাইনি।", false); return; }
        StringBuilder out = new StringBuilder("সম্ভবত এই কাজগুলো বাকি আছে—\n\n");
        for (SecureMemoryRepository.MemoryRecord record : records)
            out.append("• ").append(record.content).append('\n');
        showResult(out.toString().trim(), false);
        speak("আপনার " + records.size() + "টি অসম্পূর্ণ কাজ মনে করিয়ে দিচ্ছি।");
    }

    private void runDeviceCommand(Command command) {
        Runnable run = () -> {
            DeviceController.Result result = DeviceController.execute(this, command);
            speakAndShow(result.message, !result.success);
        };
        if (command.isProtectedUiAction() || command.needsConfirmation())
            confirm("এই command বর্তমান screen-এ সংবেদনশীল পরিবর্তন করতে পারে। এগিয়ে যাব?\n\n" + command.original, run);
        else run.run();
    }

    private void saveNote(Command command) {
        if (command.content.isEmpty()) {
            speakAndShow("নোটে কী লিখব সেটিও বলুন। যেমন—নোট লেখো, কাল রিপোর্ট জমা দিতে হবে।", true);
            return;
        }
        notes.add(command.content);
        speakAndShow("নোটটি নিরাপদে লিখে রেখেছি: “" + command.content + "”", false);
        commandInput.setText("");
    }

    private void showNotes() {
        List<String> saved = notes.list(10);
        if (saved.isEmpty()) {
            speakAndShow("আপনার কোনো সংরক্ষিত নোট নেই।", false);
            return;
        }
        StringBuilder text = new StringBuilder("আপনার সাম্প্রতিক নোট:\n\n");
        for (int i = 0; i < saved.size(); i++) text.append(i + 1).append(". ").append(saved.get(i)).append('\n');
        showResult(text.toString().trim(), false);
        speak("আপনার " + saved.size() + "টি সাম্প্রতিক নোট দেখাচ্ছি।");
    }

    private void prepareContactAction(Command command) {
        if (command.target.isEmpty()) {
            speakAndShow("কাকে যোগাযোগ করতে হবে তার নাম অথবা নম্বর বলুন।", true);
            return;
        }
        boolean directNumber = BengaliText.toAsciiDigits(command.target).replaceAll("[^0-9]", "").length() >= 7;
        if (!directNumber && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            pendingPermissionCommand = command;
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS);
            return;
        }
        String number = contacts.resolvePhone(command.target);
        if (number.isEmpty()) {
            speakAndShow("‘" + command.target + "’ নামে কোনো ফোন নম্বর খুঁজে পাইনি। নামটি ঠিক করে বা সরাসরি নম্বর দিয়ে আবার বলুন।", true);
            return;
        }
        String label = switch (command.type) {
            case CALL -> command.target + "-কে ফোন করবেন?";
            case SMS -> command.target + "-কে SMS প্রস্তুত করবেন?\n\nবার্তা: " + emptyMessage(command.content);
            default -> command.target + "-কে WhatsApp বার্তা প্রস্তুত করবেন?\n\nবার্তা: " + emptyMessage(command.content);
        };
        confirm(label, () -> launchContactAction(command, number));
    }

    private String emptyMessage(String value) {
        return value.isEmpty() ? "(বার্তা খালি—পরের স্ক্রিনে লিখতে পারবেন)" : value;
    }

    private void launchContactAction(Command command, String rawNumber) {
        String number = rawNumber.replaceAll("[^0-9+]", "");
        Intent intent;
        if (command.type == Command.Type.CALL) {
            intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)));
        } else if (command.type == Command.Type.SMS) {
            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)));
            intent.putExtra("sms_body", command.content);
        } else {
            String digits = number.replaceAll("[^0-9]", "");
            DeviceController.Result result = WhatsAppAutomation.sendApproved(this, digits, command.content);
            speakAndShow(result.message, !result.success);
            return;
        }
        safeStart(intent, "কাজটি পরের স্ক্রিনে প্রস্তুত করা হয়েছে। চূড়ান্তভাবে পাঠানো বা কল করা আপনার নিয়ন্ত্রণে থাকবে।");
    }

    private void prepareAlarm(Command command) {
        if (command.hour < 0) {
            speakAndShow("অ্যালার্মের সময়টি বুঝতে পারিনি। যেমন বলুন—সকাল ৭টায় অ্যালার্ম দাও।", true);
            return;
        }
        String time = formatTime(command.hour, command.minute);
        confirm(time + "-এ অ্যালার্ম প্রস্তুত করব?", () -> {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, command.hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, command.minute)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, command.content)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false);
            safeStart(intent, time + "-এর অ্যালার্ম প্রস্তুত হয়েছে।");
        });
    }

    private void prepareReminder(Command command) {
        if (command.hour < 0) {
            speakAndShow("রিমাইন্ডারের সময়টি বুঝতে পারিনি। তারিখ ও সময়সহ আবার বলুন।", true);
            return;
        }
        Calendar begin = Calendar.getInstance();
        begin.add(Calendar.DAY_OF_YEAR, command.dayOffset);
        begin.set(Calendar.HOUR_OF_DAY, command.hour);
        begin.set(Calendar.MINUTE, command.minute);
        begin.set(Calendar.SECOND, 0);
        String summary = (command.dayOffset == 1 ? "আগামীকাল " : command.dayOffset == 2 ? "পরশু " : "আজ ")
                + formatTime(command.hour, command.minute) + "-এ রিমাইন্ডার তৈরি করব?";
        confirm(summary, () -> {
            SecureMemoryRepository.MemoryRecord saved = memory.rememberRecord(
                    "অসম্পূর্ণ কাজ", command.content, begin.getTimeInMillis());
            if (saved != null) ReminderScheduler.schedule(this, saved.id, saved.content, saved.remindAt);
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin.getTimeInMillis())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin.getTimeInMillis() + 30 * 60 * 1000)
                    .putExtra(CalendarContract.Events.TITLE, command.content)
                    .putExtra(CalendarContract.Events.DESCRIPTION, "Astra Sathi দ্বারা প্রস্তুত");
            safeStart(intent, "রিমাইন্ডারটি ক্যালেন্ডারে প্রস্তুত করা হয়েছে। সংরক্ষণ নিশ্চিত করুন।");
        });
    }

    private void openApp(String target) {
        DeviceController.Result result = DeviceController.openApp(this, target);
        speakAndShow(result.message, !result.success);
    }

    private void webSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            speakAndShow("কী খুঁজব সেটিও বলুন।", true);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query);
        try {
            startActivity(intent);
            speakAndShow("‘" + query + "’ খুঁজছি।", false);
        } catch (ActivityNotFoundException e) {
            safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))), "তথ্য খোঁজা হচ্ছে।");
        }
    }

    private void navigate(String place) {
        if (place.isEmpty()) {
            speakAndShow("কোন জায়গার রাস্তা দেখাব সেটি বলুন।", true);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(place)));
        intent.setPackage("com.google.android.apps.maps");
        safeStart(intent, place + "-এর দিকনির্দেশ খোলা হয়েছে।");
    }

    private void prepareTorch(Command command, boolean enable) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingPermissionCommand = command;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }
        DeviceController.Result result = DeviceController.execute(this, command);
        speakAndShow(result.message, !result.success);
    }

    private void showHelp() {
        String help = "আপনি এভাবে বলতে পারেন—\n\n"
                + "• ‘মাকে ফোন করো’\n"
                + "• ‘রাহুলকে মেসেজ পাঠাও আমি আসছি’\n"
                + "• ‘সকাল ৭টায় অ্যালার্ম দাও’\n"
                + "• ‘আগামীকাল সকাল ১০টায় অফিসের রিমাইন্ডার দাও’\n"
                + "• ‘নোট লেখো কাল রিপোর্ট জমা দিতে হবে’\n"
                + "• ‘আমার নোট দেখাও’\n"
                + "• ‘হোয়াটসঅ্যাপ খোলো’\n"
                + "• ‘হোয়াটসঅ্যাপের শেষ মেসেজ পড়ে শোনাও’\n"
                + "• ‘হোয়াটসঅ্যাপের মেসেজের উত্তর দাও আমি আসছি’\n"
                + "• ‘ফেসবুক খোলো’ বা ‘ফেসবুকের নোটিফিকেশন দেখাও’\n"
                + "• ‘কে কল করছে’, ‘কল ধরো’, ‘কল কেটে দাও’\n"
                + "• ‘কালনা থানার রাস্তা দেখাও’\n"
                + "• ‘কালনার আবহাওয়া দেখাও’\n"
                + "• ‘টর্চ জ্বালাও’\n"
                + "• ‘স্ক্রিনের লেখা পড়ো’\n"
                + "• ‘নিচে স্ক্রল করো’\n"
                + "• ‘পাঠান বোতামে চাপ দাও’\n"
                + "• ‘স্ক্রিনের বোতামগুলো দেখাও’, তারপর ‘৩ নম্বরে চাপ দাও’\n"
                + "• ‘এখানে লিখো আগামীকাল আসব’\n"
                + "• ‘মনে রেখো প্রতি সোমবার রিপোর্ট দিতে হবে’\n"
                + "• ‘আমি কী ভুলে গেছি’\n"
                + "• ‘হোয়াটসঅ্যাপ খোলো, তারপর সার্চে চাপ দাও, তারপর লিখো রাহুল’\n"
                + "• ‘এই কাজটা সকাল রুটিন হিসেবে মনে রাখো’\n"
                + "• ‘সকাল রুটিন চালাও’\n"
                + "• ‘রেকর্ড শুরু করো’—কাজটি দেখিয়ে বলুন ‘রেকর্ড বন্ধ করো সকাল নামে’\n"
                + "• ‘তিন দিন আগে গাড়িতে ৫ লিটার তেল ভরেছি’\n"
                + "• ‘গাড়ি ১৭৮ কিলোমিটার চালিয়েছি’ বা ‘গাড়ির তেলের অবস্থা বলো’\n"
                + "• ‘আমি রাহুলকে বলেছি কাল রিপোর্ট দিয়ে দেবো’\n"
                + "• ‘আজ দুপুর ২টায় পেট ব্যথার ওষুধ খেয়েছি’\n"
                + "• ‘আমার বর্তমান পরিস্থিতি কেমন’\n"
                + "• ‘TCS-এর ১০টা শেয়ার মার্কেট প্রাইসে কিনো’—শুধু Financial Preview";
        showResult(help, false);
        speak("কমান্ডের উদাহরণগুলো পর্দায় দেখাচ্ছি।");
    }

    private void confirm(String message, Runnable accepted) {
        new AlertDialog.Builder(this)
                .setTitle("আপনার অনুমতি প্রয়োজন")
                .setMessage(message)
                .setPositiveButton("হ্যাঁ, এগিয়ে যাও", (dialog, which) -> accepted.run())
                .setNegativeButton("বাতিল", (dialog, which) -> speakAndShow("কাজটি বাতিল করা হয়েছে।", false))
                .setCancelable(true)
                .show();
    }

    private void safeStart(Intent intent, String successMessage) {
        try {
            startActivity(intent);
            speakAndShow(successMessage, false);
        } catch (ActivityNotFoundException e) {
            speakAndShow("এই কাজটি করার উপযুক্ত অ্যাপ ফোনে পাওয়া যায়নি।", true);
        } catch (Exception e) {
            speakAndShow("কাজটি এখন সম্পন্ন করা গেল না। আবার চেষ্টা করুন।", true);
        }
    }

    private void setStatus(String status, String instruction, boolean listening) {
        statusText.setText(status);
        instructionText.setText(instruction);
        statusText.setTextColor(Color.parseColor(listening ? "#FFE08A" : "#9BE7C8"));
    }

    private void showResult(String text, boolean error) {
        resultText.setText(text);
        resultText.setTextColor(getColor(error ? R.color.danger : R.color.ink));
        setStatus("●  আমি প্রস্তুত", getString(R.string.tap_to_speak), false);
    }

    private void speakAndShow(String text, boolean error) {
        showResult(text, error);
        speak(text);
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "astra-sathi-result");
    }

    private String formatTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        return new java.text.SimpleDateFormat("hh:mm a", new Locale("bn", "IN")).format(calendar.getTime());
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void handleLaunchIntent(Intent intent) {
        if (intent == null) return;
        String command = intent.getStringExtra("voice_command");
        if (command != null && !command.trim().isEmpty()) {
            commandInput.setText(command);
            execute(router.parse(command));
        } else if (intent.getBooleanExtra("show_pending_memory", false)) showPendingMemory();
        else if (intent.getBooleanExtra("post_call_follow_up", false)) {
            String caller = intent.getStringExtra("caller");
            commandInput.setText("আমি " + (caller == null || caller.isEmpty() ? "ওই ব্যক্তিকে" : caller + "-কে")
                    + " বলেছি আগামীকাল ");
            commandInput.setSelection(commandInput.length());
            speakAndShow("কথোপকথনে কোনো কাজের প্রতিশ্রুতি দিয়ে থাকলে বাকিটা বলুন বা লিখুন।", false);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        if (accessStatusText != null) {
            boolean screen = SathiAccessibilityService.isConnected();
            boolean notification = SathiNotificationService.isConnected();
            accessStatusText.setText(screen && notification
                    ? "● Full Access সক্রিয় — Screen ও Notification control প্রস্তুত"
                    : "○ Full Access অসম্পূর্ণ — Setup খুলে permissions পরীক্ষা করুন");
            accessStatusText.setTextColor(getColor(screen && notification ? R.color.forest : R.color.danger));
        }
        updateLifePreview();
    }

    private void updateLifePreview() {
        if (lifeStatusText == null) return;
        if (!memory.isEnabled()) {
            lifeStatusText.setText("○ Personal Memory বন্ধ—Life Context সংরক্ষণ হচ্ছে না");
            lifeStatusText.setTextColor(getColor(R.color.danger));
            return;
        }
        lifeStatusText.setText("● গাড়ি, দেওয়া কথা ও স্বাস্থ্য-লগ encryptedভাবে প্রস্তুত\nবলুন—‘আমার বর্তমান পরিস্থিতি কেমন’");
        lifeStatusText.setTextColor(getColor(R.color.forest));
    }

    @Override public void onReadyForSpeech(Bundle params) { setStatus("●  শুনছি…", "এখন বলুন", true); }
    @Override public void onBeginningOfSpeech() { instructionText.setText("আপনার কথা শুনছি…"); }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { isListening = false; setStatus("●  বুঝছি…", "একটু অপেক্ষা করুন", false); }
    @Override public void onError(int error) {
        isListening = false;
        String message = switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH -> "কথাটি বুঝতে পারিনি। আবার একটু স্পষ্ট করে বলুন।";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শুনতে পাইনি। আবার মাইক্রোফোন চাপুন।";
            case SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ভয়েস সেবার নেটওয়ার্ক সমস্যা হয়েছে। লিখেও কমান্ড দিতে পারেন।";
            default -> "ভয়েস শনাক্ত করা যায়নি। আবার চেষ্টা করুন।";
        };
        showResult(message, true);
    }
    @Override public void onResults(Bundle results) {
        isListening = false;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) { showResult("কথাটি বুঝতে পারিনি।", true); return; }
        String command = matches.get(0);
        commandInput.setText(command);
        commandInput.setSelection(command.length());
        execute(router.parse(command));
    }
    @Override public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) commandInput.setText(matches.get(0));
    }
    @Override public void onEvent(int eventType, Bundle params) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == REQUEST_AUDIO) {
            if (granted) startListening();
            else speakAndShow("ভয়েস কমান্ডের জন্য মাইক্রোফোন অনুমতি প্রয়োজন। লিখে কমান্ড দেওয়া যাবে।", true);
            return;
        }
        Command pending = pendingPermissionCommand;
        pendingPermissionCommand = null;
        if (granted && pending != null) execute(pending);
        else if (requestCode == REQUEST_CONTACTS)
            speakAndShow("নাম থেকে নম্বর খুঁজতে Contacts অনুমতি প্রয়োজন। সরাসরি নম্বর বললেও কাজ হবে।", true);
        else if (requestCode == REQUEST_CAMERA)
            speakAndShow("টর্চ নিয়ন্ত্রণের জন্য Camera অনুমতি প্রয়োজন।", true);
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
