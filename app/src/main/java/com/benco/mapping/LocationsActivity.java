package com.benco.mapping;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsListAdapter;
import com.benco.mapping.domain.LocationsViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class LocationsActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private Button addNewLocation;
    private Button backButton;
    LocationsViewModel locationsViewDModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);
        locationsViewDModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(LocationsViewModel.class);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationsViewDModel.getAllLocationsFromVm().observe(this, locations ->
        {
            if (locations != null && !locations.isEmpty()) {
                LocationsListAdapter adapter = new LocationsListAdapter((ArrayList<Locations>) locations);
                recyclerView.setAdapter(adapter);
            }
        });
        Button addNewLocationOrig;
        addNewLocation = findViewById(R.id.newLocationButton);
        addNewLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showInputDialog();
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
    private void showInputDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.locations_dialog);
        EditText nameInput = dialog.findViewById(R.id.nameInput);
        Button submitButton = dialog.findViewById(R.id.submitButton);

        submitButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString();
            if (!name.isEmpty()) {
                Locations location = new Locations(name, "config");
                new Thread(() -> {
                    long id = 0;
                    try {
                        id = locationsViewDModel.insertLocations(location);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    final long newId = id;
                    runOnUiThread(() -> {
                        // Use the ID as needed
                        System.out.println("Inserted ID: " + newId);
                        dialog.dismiss(); // Close the dialog
                    });
                }).start();
            }
        });

        dialog.show();
    }

}
