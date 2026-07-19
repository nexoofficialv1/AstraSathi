package com.astratechnologies.astrasathi;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.Locale;

public class WakeWordService extends Service implements RecognitionListener {
    private static final String CHANNEL = "astra_sathi_voice_control";
    private static final int NOTIFICATION_ID = 7001;
    private static final String ACTION_STOP = "com.astratechnologies.astrasathi.STOP_WAKE";
    private static volatile boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CommandRouter router = new CommandRouter();
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private PowerManager.WakeLock wakeLock;
    private Command pendingConfirmation;
    private boolean armed;
    private boolean speaking;
    private boolean destroyed;

    public static boolean isRunning() { return running; }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("‘হ্যালো সাথী’ বলুন"));
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AstraSathi:VoiceControl");
        wakeLock.acquire();
        setupTts();
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
            restartSoon(500);
        } else speak("এই ফোনে voice recognition service পাওয়া যায়নি।");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        return START_STICKY;
    }

    private void setupTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.forLanguageTag("bn-IN"));
                tts.setSpeechRate(0.92f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { speaking = true; }
                    @Override public void onDone(String utteranceId) { speaking = false; restartSoon(450); }
                    @Override public void onError(String utteranceId) { speaking = false; restartSoon(700); }
                });
            }
        });
    }

    private void startListening() {
        if (destroyed || speaking || recognizer == null
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try { recognizer.startListening(intent); }
        catch (Exception e) { restartSoon(1500); }
    }

    private void handleSpeech(String raw) {
        String text = BengaliText.normalize(raw);
        if (pendingConfirmation != null) {
            if (isYes(text)) {
                Command approved = pendingConfirmation; pendingConfirmation = null;
                executeApproved(approved);
            } else if (isNo(text)) {
                pendingConfirmation = null; speak("কাজটি বাতিল করেছি।");
            } else speak("অনুগ্রহ করে হ্যাঁ অথবা না বলুন।");
            return;
        }
        boolean hasWake = text.contains("হ্যালো সাথী") || text.contains("হেলো সাথী") || text.contains("অ্যাস্ট্রা সাথী");
        if (!hasWake && !armed) { restartSoon(400); return; }
        String commandText = text.replaceFirst(".*?(হ্যালো সাথী|হেলো সাথী|অ্যাস্ট্রা সাথী)", "").trim();
        if (commandText.isEmpty() && !armed) { armed = true; speak("বলুন, কী করতে হবে?"); return; }
        if (armed && !hasWake) commandText = text;
        armed = false;
        Command command = router.parse(commandText);
        if (command.type == Command.Type.UNKNOWN) { speak("কমান্ডটি বুঝতে পারিনি। আবার বলুন।"); return; }
        new SecureMemoryRepository(this).rememberCommand(command.original);
        boolean workflowRisk = false;
        if (command.type == Command.Type.WORKFLOW) {
            AutomationWorkflow workflow = new WorkflowPlanner().plan(command.content);
            if (!workflow.isValid()) { speak(workflow.error); return; }
            workflowRisk = workflow.requiresConfirmation();
        }
        if (command.type == Command.Type.ROUTINE_RUN) {
            RoutineRepository.Routine routine = new RoutineRepository(this).find(command.target);
            if (routine != null) {
                AutomationWorkflow workflow = new WorkflowPlanner().plan(routine.command);
                workflowRisk = workflow.isValid() && workflow.requiresConfirmation();
            }
        }
        if (command.needsConfirmation() || command.isProtectedUiAction() || workflowRisk
                || command.type == Command.Type.UNINSTALL_APP || command.type == Command.Type.SECURITY_SETTINGS) {
            pendingConfirmation = command;
            speak(VoiceActionExecutor.describe(command) + "। আপনি কি নিশ্চিত? হ্যাঁ অথবা না বলুন।");
        } else executeApproved(command);
    }

    private void executeApproved(Command command) {
        if (command.type == Command.Type.WORKFLOW) {
            AutomationWorkflow workflow = new WorkflowPlanner().plan(command.content);
            if (!workflow.isValid()) { speak(workflow.error); return; }
            WorkflowRunner.get().run(this, workflow, new WorkflowRunner.Callback() {
                @Override public void onProgress(int current, int total, String message) {
                    updateNotification("Workflow " + current + "/" + total + ": " + message);
                }
                @Override public void onComplete(boolean success, String message) { speak(message); }
            });
            return;
        }
        if (command.type == Command.Type.ROUTINE_SAVE) {
            String source = WorkflowRunner.getLastSuccessfulSource();
            boolean saved = !source.isEmpty() && new RoutineRepository(this).save(command.target, source);
            speak(saved ? command.target + " Routine হিসেবে মনে রেখেছি।"
                    : "Routine সংরক্ষণ করা যায়নি। Personal Memory ও আগের workflow পরীক্ষা করুন।");
            return;
        }
        if (command.type == Command.Type.ROUTINE_RUN) {
            RoutineRepository.Routine routine = new RoutineRepository(this).find(command.target);
            if (routine == null) { speak("এই নামে Routine পাইনি।"); return; }
            executeApproved(Command.of(Command.Type.WORKFLOW, routine.command, "", routine.command));
            return;
        }
        if (command.type == Command.Type.ROUTINE_LIST) {
            java.util.List<RoutineRepository.Routine> routines = new RoutineRepository(this).list();
            if (routines.isEmpty()) { speak("কোনো সংরক্ষিত Routine নেই।"); return; }
            StringBuilder names = new StringBuilder("সংরক্ষিত Routine হলো ");
            for (RoutineRepository.Routine routine : routines) names.append(routine.name).append("। ");
            speak(names.toString());
            return;
        }
        DeviceController.Result result = VoiceActionExecutor.execute(this, command);
        speak(result.message);
    }

    private boolean isYes(String text) {
        return text.equals("হ্যাঁ") || text.contains("হ্যাঁ করো") || text.contains("নিশ্চিত") || text.contains("এগিয়ে যাও");
    }

    private boolean isNo(String text) {
        return text.equals("না") || text.contains("বাতিল") || text.contains("করো না");
    }

    private void speak(String message) {
        if (recognizer != null) { try { recognizer.cancel(); } catch (Exception ignored) { } }
        speaking = true;
        updateNotification(message);
        if (tts != null) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "wake-result-" + System.currentTimeMillis());
        else { speaking = false; restartSoon(1000); }
    }

    private void restartSoon(long delay) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::startListening, delay);
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                "Astra Sathi voice control", NotificationManager.IMPORTANCE_LOW));
    }

    private android.app.Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, WakeWordService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new android.app.Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher).setContentTitle("Astra Sathi শুনছে")
                .setContentText(text).setContentIntent(content).setOngoing(true)
                .addAction(new android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_launcher),
                        "বন্ধ করুন", stopPending).build()).build();
    }

    private void updateNotification(String text) {
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification(text));
    }

    @Override public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) restartSoon(500); else handleSpeech(matches.get(0));
    }
    @Override public void onError(int error) { restartSoon(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1800 : 800); }
    @Override public void onReadyForSpeech(Bundle params) { }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }
    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType, Bundle params) { }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        destroyed = true; running = false; handler.removeCallbacksAndMessages(null);
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }
}
