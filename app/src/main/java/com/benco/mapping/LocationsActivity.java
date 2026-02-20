package com.benco.mapping;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsListAdapter;
import com.benco.mapping.domain.LocationsViewModel;

import java.util.ArrayList;
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
        locationsViewDModel.getAllLocationsFromVm().observe(this, locations -> {
            if (locations != null && !locations.isEmpty()) {
                LocationsListAdapter adapter = new LocationsListAdapter((ArrayList<Locations>) locations,
                        this::showDeleteLocationConfirmation);
                recyclerView.setAdapter(adapter);
            } else {
                recyclerView.setAdapter(null);
            }
        });

        addNewLocation = findViewById(R.id.newLocationButton);
        addNewLocation.setOnClickListener(v -> showInputDialog());

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void showDeleteLocationConfirmation(Locations location) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Location")
                .setMessage("Delete location '" + location.name + "' and all applications for it?")
                .setPositiveButton("Delete", (dialog, which) -> locationsViewDModel.deleteLocationById(location.lid))
                .setNegativeButton("Cancel", null)
                .show();
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
                    long id;
                    try {
                        id = locationsViewDModel.insertLocations(location);
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    final long newId = id;
                    runOnUiThread(() -> {
                        System.out.println("Inserted ID: " + newId);
                        dialog.dismiss();
                    });
                }).start();
            }
        });

        dialog.show();
    }
}
