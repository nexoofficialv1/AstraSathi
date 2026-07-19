package com.astratechnologies.astrasathi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class LifeContextActivity extends Activity {
    private TextView vehicle;
    private TextView commitments;
    private TextView health;
    private TextView permission;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_life_context);
        vehicle = findViewById(R.id.vehicleContextText);
        commitments = findViewById(R.id.commitmentContextText);
        health = findViewById(R.id.healthContextText);
        permission = findViewById(R.id.lifePermissionText);
        findViewById(R.id.refreshLifeContextButton).setOnClickListener(v -> refresh());
        findViewById(R.id.speakLifeCommandButton).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                    .putExtra("voice_command", "আমার বর্তমান পরিস্থিতি কেমন"));
            finish();
        });
        findViewById(R.id.lifeContextBackButton).setOnClickListener(v -> finish());
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        SecureMemoryRepository memory = new SecureMemoryRepository(this);
        if (!memory.isEnabled()) {
            permission.setText("○ Personal Memory বন্ধ আছে। Full Access Setup থেকে চালু করুন।");
            permission.setTextColor(getColor(R.color.danger));
            vehicle.setText("তথ্য সংরক্ষণ বন্ধ");
            commitments.setText("তথ্য সংরক্ষণ বন্ধ");
            health.setText("তথ্য সংরক্ষণ বন্ধ");
            return;
        }
        permission.setText("● Encrypted on-device Life Context সক্রিয়");
        permission.setTextColor(getColor(R.color.forest));
        LifeContextRepository repository = new LifeContextRepository(this);
        vehicle.setText(repository.vehicleSummary());
        commitments.setText(repository.commitmentSummary());
        health.setText(repository.healthSummary());
    }
}
