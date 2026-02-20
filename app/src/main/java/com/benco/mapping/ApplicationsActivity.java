package com.benco.mapping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.ApplicationsListAdapter;
import com.benco.mapping.data.Locations;
import com.benco.mapping.domain.ApplicationsViewModel;
import com.benco.mapping.domain.LocationsViewModel;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class ApplicationsActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private Button addNewApplication;
    private Button backButton;
    private ApplicationsViewModel applicationsViewDModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications);

        lid = getIntent().getIntExtra("lid", -1);

        TextView detailTextView = findViewById(R.id.detailTextView);
        detailTextView.setText("lid: " + lid);
        LocationsViewModel locationsViewDModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(LocationsViewModel.class);
        locationsViewDModel.getAllLocationsFromVm().observe(this, locations -> {
            if (locations != null && !locations.isEmpty()) {
                for (Locations location : locations) {
                    if (location.lid == lid) {
                        detailTextView.setText(location.name);
                    }
                }
            }
        });

        applicationsViewDModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ApplicationsViewModel.class);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        applicationsViewDModel.getAllApplicationsByLid(lid).observe(this, applications -> {
            if (applications != null && !applications.isEmpty()) {
                ApplicationsListAdapter adapter = new ApplicationsListAdapter((ArrayList<Applications>) applications,
                        this::showDeleteApplicationConfirmation);
                recyclerView.setAdapter(adapter);
            } else {
                recyclerView.setAdapter(null);
            }
        });

        addNewApplication = findViewById(R.id.newApplicationsButton);
        addNewApplication.setOnClickListener(v -> {
            if (lid > 0) {
                Gson gson = new Gson();
                String configJSON = gson.toJson(settings);
                Applications application = new Applications(lid, new Date(), "", configJSON);
                new Thread(() -> {
                    long id;
                    try {
                        id = applicationsViewDModel.insertApplications(application);
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    final long newId = id;
                    runOnUiThread(() -> {
                        aid = (int) newId;
                        Intent intent = new Intent(ApplicationsActivity.this, MainActivity.class);
                        intent.putExtra("lid", lid + "");
                        intent.putExtra("aid", aid + "");
                        intent.putExtra("configJSON", configJSON);
                        finish();
                        startActivity(intent);
                    });
                }).start();
            }
        });

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void showDeleteApplicationConfirmation(Applications application) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Application")
                .setMessage("Delete this application and recorded path data?")
                .setPositiveButton("Delete", (dialog, which) -> applicationsViewDModel.deleteApplicationById(application.aid))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
