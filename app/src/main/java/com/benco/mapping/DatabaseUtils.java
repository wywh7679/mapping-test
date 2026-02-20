package com.benco.mapping;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DatabaseUtils {

    public static void exportDatabase(Context context) {
        try {
            // Get the database path
            String currentDBPath = context.getDatabasePath("locations").getAbsolutePath();
            File dbFile = new File(currentDBPath);
            FileInputStream fis = new FileInputStream(dbFile);

            // Specify the export file location
            File exportDir = new File(Environment.getExternalStorageDirectory(), "ExportedDB");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File exportFile = new File(exportDir, "locations.db");
            FileOutputStream fos = new FileOutputStream(exportFile);

            // Copy the database file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.flush();
            fos.close();
            fis.close();
            System.out.println("Database exported to: " + exportFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void importDatabase(Context context) {
        try {
            // Get the database path
            String currentDBPath = context.getDatabasePath("locations").getAbsolutePath();
            File dbFile = new File(currentDBPath);
            if (dbFile.exists()) {
                dbFile.delete(); // Delete the existing database
            }

            // Specify the import file location
            File importFile = new File(Environment.getExternalStorageDirectory(), "ExportedDB/locations.db");
            FileInputStream fis = new FileInputStream(importFile);
            FileOutputStream fos = new FileOutputStream(dbFile);

            // Copy the database file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.flush();
            fos.close();
            fis.close();
            System.out.println("Database imported from: " + importFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
