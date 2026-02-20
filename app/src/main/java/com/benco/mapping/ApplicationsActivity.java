package com.benco.mapping;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.data.ApplicationsListAdapter;
import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsListAdapter;
import com.benco.mapping.domain.ApplicationsViewModel;
import com.benco.mapping.domain.LocationsViewModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class ApplicationsActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private Button addNewApplication;
    private Button backButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications);

        // Get the ID passed from the intent
        lid = getIntent().getIntExtra("lid", -1);

        // Use the ID to fetch details from the database (if needed)
        // For now, just display the ID for demonstration
        TextView detailTextView = findViewById(R.id.detailTextView);
        detailTextView.setText("lid: " + lid);
        LocationsViewModel locationsViewDModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(LocationsViewModel.class);
        locationsViewDModel.getAllLocationsFromVm().observe(this, locations ->
        {
            if (locations != null && !locations.isEmpty()) {
                for(Locations location : locations) {
                    if (location.lid == lid) {
                        detailTextView.setText(location.name);
                    }
               }
            }
        });
        //getApplicationByLid(lid);
        ApplicationsViewModel applicationsViewDModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ApplicationsViewModel.class);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        applicationsViewDModel.getAllApplicationsByLid(lid).observe(this, applications ->
        {
            if (applications != null && !applications.isEmpty()) {
                ApplicationsListAdapter adapter = new ApplicationsListAdapter((ArrayList<Applications>) applications);
                recyclerView.setAdapter(adapter);
            }
        });
        addNewApplication = findViewById(R.id.newApplicationsButton);
        addNewApplication.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (lid > 0) {
                    Gson gson = new Gson(); // Basic Gson instance
                    String configJSON = gson.toJson(settings);
                    Applications application = new Applications(lid, new Date(), "", configJSON);
                    new Thread(() -> {
                        long id = 0;
                        try {
                            id = applicationsViewDModel.insertApplications(application);
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        final long newId = id;
                        runOnUiThread(() -> {
                            // Use the ID as needed
                            System.out.println("Inserted ID: " + newId);
                            aid = (int) newId;
                            Intent intent = new Intent(ApplicationsActivity.this, MainActivity.class);
                            intent.putExtra("lid", lid+"");
                            intent.putExtra("aid", aid+"");
                            intent.putExtra("configJSON", configJSON);
                            finish();
                            startActivity(intent);
                        });
                    }).start();
                }
            }
        });
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes the current activity and returns to the previous one
            }
        });
    }
}