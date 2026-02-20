package com.benco.mapping;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.data.ApplicationsListAdapter;
import com.benco.mapping.data.Locations;
import com.benco.mapping.domain.ApplicationsDataViewModel;
import com.benco.mapping.domain.ApplicationsViewModel;
import com.benco.mapping.domain.LocationsViewModel;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipOutputStream;

public class ApplicationsActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private Button addNewApplication;
    private Button backButton;
    private ApplicationsViewModel applicationsViewDModel;
    private ApplicationsDataViewModel applicationsDataViewModel;
    private Applications pendingExportApplication;
    private ActivityResultLauncher<Intent> exportShapefileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications);

        exportShapefileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    if (uri == null || pendingExportApplication == null) {
                        return;
                    }
                    exportApplicationToShapefile(uri, pendingExportApplication);
                    pendingExportApplication = null;
                });

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
        applicationsDataViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ApplicationsDataViewModel.class);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        applicationsViewDModel.getAllApplicationsByLid(lid).observe(this, applications -> {
            if (applications != null && !applications.isEmpty()) {
                ApplicationsListAdapter adapter = new ApplicationsListAdapter((ArrayList<Applications>) applications,
                        this::showDeleteApplicationConfirmation,
                        this::openExportFilePicker);
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

    private void openExportFilePicker(Applications application) {
        pendingExportApplication = application;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "application_" + application.aid + ".zip");
        try {
            exportShapefileLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            exportApplicationToAppStorage(application);
        }
    }

    private void exportApplicationToShapefile(Uri destinationUri, Applications application) {
        new Thread(() -> {
            List<ApplicationsData> pathPoints = applicationsDataViewModel.getAllApplicationsListVm(application.aid);
            if (pathPoints == null || pathPoints.size() < 2) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Not enough GPS data to export shapefile.", Toast.LENGTH_LONG).show());
                return;
            }
            try (OutputStream outputStream = getContentResolver().openOutputStream(destinationUri);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                if (outputStream == null) {
                    throw new IOException("Unable to open export destination.");
                }
                String baseName = "application_" + application.aid;
                ShapefileExporter.exportPolylineZip(zipOutputStream, baseName, application.aid, pathPoints);
                runOnUiThread(() -> Toast.makeText(this,
                        "Exported shapefile ZIP: " + baseName + ".zip", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void exportApplicationToAppStorage(Applications application) {
        new Thread(() -> {
            List<ApplicationsData> pathPoints = applicationsDataViewModel.getAllApplicationsListVm(application.aid);
            if (pathPoints == null || pathPoints.size() < 2) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Not enough GPS data to export shapefile.", Toast.LENGTH_LONG).show());
                return;
            }

            String fileName = "application_" + application.aid + ".zip";
            File exportDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (exportDir == null) {
                exportDir = getFilesDir();
            }
            File outputFile = new File(exportDir, fileName);

            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
                String baseName = "application_" + application.aid;
                ShapefileExporter.exportPolylineZip(zipOutputStream, baseName, application.aid, pathPoints);
                File finalExportDir = exportDir;
                runOnUiThread(() -> Toast.makeText(this,
                        "No document app found. Export saved to: " + finalExportDir.getAbsolutePath() + "/" + fileName,
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
