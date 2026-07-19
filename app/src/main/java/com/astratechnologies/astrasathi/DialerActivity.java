package com.astratechnologies.astrasathi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DialerActivity extends Activity {
    private static final int REQUEST_CALL = 410;
    private EditText numberInput;
    private TextView recentCalls;
    private String pendingNumber = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);
        numberInput = findViewById(R.id.dialerNumber);
        recentCalls = findViewById(R.id.recentCallsText);
        Uri data = getIntent() == null ? null : getIntent().getData();
        if (data != null && "tel".equals(data.getScheme())) numberInput.setText(data.getSchemeSpecificPart());
        findViewById(R.id.placeCallButton).setOnClickListener(v -> placeCall(numberInput.getText().toString()));
        findViewById(R.id.dialerBackButton).setOnClickListener(v -> finish());
    }

    private void placeCall(String raw) {
        String number = BengaliText.toAsciiDigits(raw).replaceAll("[^0-9+*#]", "");
        if (number.isEmpty()) { Toast.makeText(this, "ফোন নম্বর লিখুন।", Toast.LENGTH_SHORT).show(); return; }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingNumber = number;
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
            return;
        }
        try {
            TelecomManager telecom = getSystemService(TelecomManager.class);
            telecom.placeCall(Uri.parse("tel:" + Uri.encode(number)), new Bundle());
        } catch (Exception e) {
            Toast.makeText(this, "Call শুরু করা গেল না। Default Phone role পরীক্ষা করুন।", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        recentCalls.setText(CallController.recentCalls(this, 5));
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && !pendingNumber.isEmpty()) {
            String number = pendingNumber; pendingNumber = ""; placeCall(number);
        }
    }
}
