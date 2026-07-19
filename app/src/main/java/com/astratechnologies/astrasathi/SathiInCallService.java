package com.astratechnologies.astrasathi;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.telecom.VideoProfile;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class SathiInCallService extends InCallService {
    public static final String ACTION_ANSWER = "com.astratechnologies.astrasathi.CALL_ANSWER";
    public static final String ACTION_END = "com.astratechnologies.astrasathi.CALL_END";
    private static final String CHANNEL = "astra_sathi_phone_calls";
    private static final int NOTIFICATION_ID = 7003;
    private static volatile SathiInCallService instance;

    private Call currentCall;
    private final List<Call> calls = new ArrayList<>();
    private final List<Call> connectedCalls = new ArrayList<>();
    private TextToSpeech tts;
    private String pendingAnnouncement = "";
    private String callerDescription = "অজানা নম্বর";

    public static SathiInCallService get() { return instance; }

    private final Call.Callback callback = new Call.Callback() {
        @Override public void onStateChanged(Call call, int state) {
            if (state == Call.STATE_ACTIVE && !connectedCalls.contains(call)) connectedCalls.add(call);
            selectCurrentCall();
            updateCallNotification(currentCall != null && currentCall.getState() == Call.STATE_RINGING);
            if (state == Call.STATE_DISCONNECTED) clearCall(call);
        }
        @Override public void onDetailsChanged(Call call, Call.Details details) {
            if (call == currentCall) {
                callerDescription = identify(call);
                updateCallNotification(call.getState() == Call.STATE_RINGING);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        createChannel();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.forLanguageTag("bn-IN"));
                tts.setSpeechRate(0.92f);
                if (!pendingAnnouncement.isEmpty()) {
                    String message = pendingAnnouncement; pendingAnnouncement = "";
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "incoming-caller");
                }
            }
        });
    }

    @Override public void onCallAdded(Call call) {
        super.onCallAdded(call);
        calls.add(call);
        if (call.getState() == Call.STATE_ACTIVE && !connectedCalls.contains(call)) connectedCalls.add(call);
        call.registerCallback(callback);
        selectCurrentCall();
        callerDescription = identify(call);
        boolean ringing = call.getState() == Call.STATE_RINGING;
        updateCallNotification(ringing);
        if (ringing) announce(callerDescription + " কল করছেন।");
    }

    @Override public void onCallRemoved(Call call) {
        clearCall(call);
        super.onCallRemoved(call);
    }

    public boolean hasCurrentCall() { return currentCall != null; }
    public String getCurrentCallerDescription() { return callerDescription; }
    public int getCurrentState() { return currentCall == null ? Call.STATE_DISCONNECTED : currentCall.getState(); }

    public boolean answerCurrent() {
        Call call = currentCall;
        if (call == null || call.getState() != Call.STATE_RINGING) return false;
        try { call.answer(VideoProfile.STATE_AUDIO_ONLY); return true; }
        catch (Exception e) { return false; }
    }

    public boolean endCurrent() {
        Call call = currentCall;
        if (call == null) return false;
        try {
            if (call.getState() == Call.STATE_RINGING) call.reject(false, null); else call.disconnect();
            return true;
        } catch (Exception e) { return false; }
    }

    public boolean setSpeaker(boolean enabled) {
        if (currentCall == null) return false;
        try { setAudioRoute(enabled ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_EARPIECE); return true; }
        catch (Exception e) { return false; }
    }

    public boolean setCallMuted(boolean muted) {
        if (currentCall == null) return false;
        try { setMuted(muted); return true; }
        catch (Exception e) { return false; }
    }

    private String identify(Call call) {
        try {
            Uri handle = call.getDetails().getHandle();
            String number = handle == null ? "" : handle.getSchemeSpecificPart();
            if (number.isEmpty()) return "অজানা নম্বর";
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                String name = new ContactResolver(this).resolveDisplayName(number);
                if (!name.isEmpty()) return name;
            }
            return number;
        } catch (Exception e) { return "অজানা নম্বর"; }
    }

    private void announce(String message) {
        if (tts == null) { pendingAnnouncement = message; return; }
        int result = tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "incoming-caller");
        if (result == TextToSpeech.ERROR) pendingAnnouncement = message;
    }

    private void updateCallNotification(boolean ringing) {
        if (currentCall == null) return;
        Intent open = new Intent(this, InCallActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent content = PendingIntent.getActivity(this, 31, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent answer = PendingIntent.getBroadcast(this, 32,
                new Intent(this, CallActionReceiver.class).setAction(ACTION_ANSWER),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent end = PendingIntent.getBroadcast(this, 33,
                new Intent(this, CallActionReceiver.class).setAction(ACTION_END),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(ringing ? "Incoming call" : "Call চলছে")
                .setContentText(callerDescription)
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true).setContentIntent(content)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_launcher),
                        ringing ? "কল ধরুন" : "Call screen", ringing ? answer : content).build())
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_launcher),
                        ringing ? "বাতিল" : "কল শেষ", end).build());
        if (ringing) builder.setFullScreenIntent(content, true);
        try { getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, builder.build()); }
        catch (Exception ignored) { }
    }

    private void clearCall(Call call) {
        String endedCaller = call == null ? callerDescription : identify(call);
        boolean hadConversation = connectedCalls.remove(call);
        if (call != null) call.unregisterCallback(callback);
        calls.remove(call);
        selectCurrentCall();
        if (currentCall == null) getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
        else updateCallNotification(currentCall.getState() == Call.STATE_RINGING);
        if (hadConversation) PostCallFollowUp.post(this, endedCaller);
    }

    private void selectCurrentCall() {
        Call selected = null;
        for (Call call : calls) if (call.getState() == Call.STATE_RINGING) { selected = call; break; }
        if (selected == null) for (Call call : calls) if (call.getState() == Call.STATE_ACTIVE) { selected = call; break; }
        if (selected == null && !calls.isEmpty()) selected = calls.get(calls.size() - 1);
        currentCall = selected;
        if (selected != null) callerDescription = identify(selected);
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Astra Sathi phone calls", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Incoming caller announcement and call controls");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public void onDestroy() {
        for (Call call : new ArrayList<>(calls)) call.unregisterCallback(callback);
        calls.clear();
        connectedCalls.clear();
        currentCall = null;
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
