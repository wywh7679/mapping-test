package com.benco.mapping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends BaseActivity {
    Button quickSprayBtn;
    Button settingsButton;
    Button locationsButton;
    Button threeDeeButtton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        quickSprayBtn = findViewById(R.id.quickSprayBtn);
        quickSprayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SettingsActivity
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra("aid", "-1"); // Pass the ID or any other data
                startActivity(intent);
            }
        });
        settingsButton = findViewById(R.id.settingsBtn);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SettingsActivity
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        locationsButton = findViewById(R.id.locationsBtn);
        locationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start LocationsActivity
                Intent intent = new Intent(HomeActivity.this, LocationsActivity.class);
                startActivity(intent);
            }
        });
        threeDeeButtton = findViewById(R.id.threeDeeBtn);
        threeDeeButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GLMapActivity.class);
                startActivity(intent);
            }
        });

    }
}
