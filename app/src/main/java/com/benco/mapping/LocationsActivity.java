package com.benco.mapping;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsListAdapter;
import com.benco.mapping.domain.LocationsViewModel;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LocationsActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private Button addNewLocation;
    private Button backButton;
    LocationsViewModel locationsViewDModel;
    private ActivityResultLauncher<Intent> boundaryPickerLauncher;
    private Locations pendingBoundaryLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);
        locationsViewDModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(LocationsViewModel.class);

        boundaryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null || pendingBoundaryLocation == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importBoundaryForLocation(pendingBoundaryLocation, uri);
                    }
                    pendingBoundaryLocation = null;
                });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationsViewDModel.getAllLocationsFromVm().observe(this, locations -> {
            if (locations != null && !locations.isEmpty()) {
                LocationsListAdapter adapter = new LocationsListAdapter((ArrayList<Locations>) locations,
                        this::showDeleteLocationConfirmation,
                        this::openBoundaryPicker);
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

    private void openBoundaryPicker(Locations location) {
        pendingBoundaryLocation = location;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        boundaryPickerLauncher.launch(intent);
    }

    private void importBoundaryForLocation(Locations location, Uri uri) {
        new Thread(() -> {
            try {
                String path = copyBoundaryFile(uri, location.lid);
                JSONObject configJson;
                try {
                    configJson = new JSONObject(location.config);
                } catch (Exception ignored) {
                    configJson = new JSONObject();
                }
                configJson.put("boundaryFilePath", path);
                location.config = configJson.toString();
                locationsViewDModel.updateLocation(location);
                runOnUiThread(() -> Toast.makeText(this,
                        "Boundary uploaded for " + location.name, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Boundary import failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String copyBoundaryFile(Uri uri, int lid) throws Exception {
        File dir = new File(getFilesDir(), "boundaries");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File shpFile = new File(dir, "location_" + lid + ".shp");

        String lower = uri.toString().toLowerCase();
        if (lower.endsWith(".zip")) {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.getName().toLowerCase().endsWith(".shp")) {
                        try (FileOutputStream outputStream = new FileOutputStream(shpFile)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zipInputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, len);
                            }
                            return shpFile.getAbsolutePath();
                        }
                    }
                }
            }
            throw new Exception("No .shp file found in ZIP.");
        }

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(shpFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        }
        return shpFile.getAbsolutePath();
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
                Locations location = new Locations(name, "{}");
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
