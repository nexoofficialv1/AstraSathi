package com.astratechnologies.astrasathi;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class InCallActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView callerText;
    private TextView callStatus;
    private Button answerButton;
    private Button rejectButton;
    private Button endButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_in_call);
        callerText = findViewById(R.id.inCallCaller);
        callStatus = findViewById(R.id.inCallStatus);
        answerButton = findViewById(R.id.answerCallButton);
        rejectButton = findViewById(R.id.rejectCallButton);
        endButton = findViewById(R.id.endCallButton);
        answerButton.setOnClickListener(v -> {
            SathiInCallService service = SathiInCallService.get();
            if (service != null) service.answerCurrent();
            refresh();
        });
        rejectButton.setOnClickListener(v -> {
            SathiInCallService service = SathiInCallService.get();
            if (service != null) service.endCurrent();
        });
        endButton.setOnClickListener(v -> {
            SathiInCallService service = SathiInCallService.get();
            if (service != null) service.endCurrent();
        });
        findViewById(R.id.speakerCallButton).setOnClickListener(v -> {
            SathiInCallService service = SathiInCallService.get();
            if (service != null) service.setSpeaker(true);
        });
        findViewById(R.id.earpieceCallButton).setOnClickListener(v -> {
            SathiInCallService service = SathiInCallService.get();
            if (service != null) service.setSpeaker(false);
        });
        findViewById(R.id.muteCallButton).setOnClickListener(v -> {
            SathiInCallService service = SathiInCallService.get();
            if (service != null) service.setCallMuted(true);
        });
        findViewById(R.id.unmuteCallButton).setOnClickListener(v -> {
            SathiInCallService service = SathiInCallService.get();
            if (service != null) service.setCallMuted(false);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        handler.removeCallbacksAndMessages(null);
        SathiInCallService service = SathiInCallService.get();
        if (service == null || !service.hasCurrentCall()) {
            callStatus.setText("Call শেষ হয়েছে");
            answerButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
            endButton.setVisibility(View.GONE);
            handler.postDelayed(this::finish, 900);
            return;
        }
        callerText.setText(service.getCurrentCallerDescription());
        int state = service.getCurrentState();
        boolean ringing = state == Call.STATE_RINGING;
        callStatus.setText(ringing ? "Incoming call" : state == Call.STATE_DIALING
                ? "কল করা হচ্ছে…" : state == Call.STATE_ACTIVE ? "Call চলছে" : "Connecting…");
        answerButton.setVisibility(ringing ? View.VISIBLE : View.GONE);
        rejectButton.setVisibility(ringing ? View.VISIBLE : View.GONE);
        endButton.setVisibility(ringing ? View.GONE : View.VISIBLE);
        handler.postDelayed(this::refresh, 500);
    }

    @Override protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }
}
