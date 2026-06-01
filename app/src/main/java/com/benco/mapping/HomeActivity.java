package com.benco.mapping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsRoomDatabase;

import java.util.Date;

public class HomeActivity extends BaseActivity {
    private static final String DEFAULT_LOCATION_NAME = "Default Location";

    Button quickSprayBtn;
    Button settingsButton;
    Button locationsButton;
    Button threeDeeButtton;
    Button resumeLastApplicationButton;
    Button newApplicationButton;
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

        resumeLastApplicationButton = findViewById(R.id.resumeLastApplicationBtn);
        newApplicationButton = findViewById(R.id.newApplicationBtn);

        configureHomeMode();

    }

    private void configureHomeMode() {
        if (USE_SIMPLIFIED_HOME) {
            quickSprayBtn.setVisibility(View.GONE);
            locationsButton.setVisibility(View.GONE);
            threeDeeButtton.setVisibility(View.GONE);

            resumeLastApplicationButton.setVisibility(View.VISIBLE);
            newApplicationButton.setVisibility(View.VISIBLE);
            centerSimplifiedHomeButtons();

            resumeLastApplicationButton.setOnClickListener(v -> resumeLastApplication());
            newApplicationButton.setOnClickListener(v -> createNewApplicationOnDefaultLocation());
        } else {
            resumeLastApplicationButton.setVisibility(View.GONE);
            newApplicationButton.setVisibility(View.GONE);
        }
    }

    private void centerSimplifiedHomeButtons() {
        ConstraintLayout homeRoot = findViewById(R.id.homeRoot);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(homeRoot);
        constraintSet.clear(R.id.settingsBtn, ConstraintSet.TOP);
        constraintSet.connect(R.id.settingsBtn, ConstraintSet.TOP, R.id.newApplicationBtn, ConstraintSet.BOTTOM);
        constraintSet.setVerticalChainStyle(R.id.resumeLastApplicationBtn, ConstraintSet.CHAIN_PACKED);
        constraintSet.applyTo(homeRoot);
    }

    private void resumeLastApplication() {
        new Thread(() -> {
            try {
                LocationsRoomDatabase db = LocationsRoomDatabase.getDatabase(this);
                int defaultLid = getOrCreateDefaultLocationId(db);
                Applications lastApplication = db.applicationsDao().getLastApplicationByLidSync(defaultLid);
                runOnUiThread(() -> {
                    if (lastApplication == null) {
                        Toast.makeText(this, "No previous application found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    startMainActivity(defaultLid, lastApplication.aid);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Unable to resume: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void createNewApplicationOnDefaultLocation() {
        new Thread(() -> {
            try {
                LocationsRoomDatabase db = LocationsRoomDatabase.getDatabase(this);
                int defaultLid = getOrCreateDefaultLocationId(db);
                String configJSON = "{}";
                long newAid = db.applicationsDao().insert(new Applications(defaultLid, new Date(), "", configJSON));
                runOnUiThread(() -> startMainActivity(defaultLid, (int) newAid));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Unable to create application: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private int getOrCreateDefaultLocationId(LocationsRoomDatabase db) {
        Locations location = db.locationsDao().getLocationByNameSync(DEFAULT_LOCATION_NAME);
        if (location != null) {
            return location.lid;
        }
        long newLid = db.locationsDao().insert(new Locations(DEFAULT_LOCATION_NAME, "{}"));
        return (int) newLid;
    }

    private void startMainActivity(int locationId, int applicationId) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra("lid", String.valueOf(locationId));
        intent.putExtra("aid", String.valueOf(applicationId));
        startActivity(intent);
    
    }
}
