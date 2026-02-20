package com.benco.mapping.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.benco.mapping.converters.Converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Locations.class,Applications.class,ApplicationsData.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class LocationsRoomDatabase extends RoomDatabase {
    public abstract LocationsDao locationsDao();
    public abstract ApplicationsDao applicationsDao();
    public abstract ApplicationsDataDao applicationsDataDao();

    private static volatile LocationsRoomDatabase locationsRoomDatabase;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    @Override
    public void init(@NonNull DatabaseConfiguration configuration) {
        //importExistingDatabase(configuration.context, true); //<<<<<<<<<< Invokes the Import of the Exisiting Database.
        super.init(configuration);
    }
    private void importExistingDatabase(Context context, boolean throw_exception) {
        int buffer_size = 32768;
        File dbpath = context.getDatabasePath("locations");
        if (dbpath.exists()) {
            return; // Database already exists
        }
        // Just in case make the directories
        File dirs = new File(dbpath.getParent());
        dirs.mkdirs();
        int stage = 0;
        byte[] buffer = new byte[buffer_size];
        long total_bytes_read = 0;
        long total_bytes_written = 0;
        int bytes_read = 0;
        try {
            File dbImportPath = context.getDatabasePath("locations_import");
            InputStream assetdb = new FileInputStream(dbImportPath);
            stage++;
            dbpath.createNewFile();
            stage++;
            OutputStream realdb = new FileOutputStream(dbpath);
            stage++;
            while((bytes_read = assetdb.read(buffer)) > 0) {
                total_bytes_read = total_bytes_read + bytes_read;
                realdb.write(buffer,0,bytes_read);
                total_bytes_written = total_bytes_written + bytes_read;
            }
            stage++;
            realdb.flush();
            stage++;
            assetdb.close();
            stage++;
            realdb.close();
            stage++;
        } catch (IOException e) {
            String failed_at = "";
            switch  (stage) {
                case 0:
                    failed_at = "Opening Asset locations";
                    break;
                case 1:
                    failed_at = "Creating Output Database " + dbpath.getAbsolutePath();
                    break;
                case 2:
                    failed_at = "Genreating Database OutputStream " + dbpath.getAbsolutePath();
                    break;
                case 3:
                    failed_at = "Copying Data from Asset Database to Output Database. " +
                            " Bytes read=" + String.valueOf(total_bytes_read) +
                            " Bytes written=" + String.valueOf(total_bytes_written);
                    break;
                case 4:
                    failed_at = "Flushing Written Data (" +
                            String.valueOf(total_bytes_written) +
                            " bytes written)";
                    break;
                case 5:
                    failed_at = "Closing Asset Database File.";
                    break;
                case 6:
                    failed_at = "Closing Created Database File.";
            }
            String msg = "An error was encountered copying the Database " +
                    "from the asset file to New Database. " +
                    "The error was encountered whilst :-\n\t" + failed_at;
            Log.e("IMPORTDATABASE",msg);
            e.printStackTrace();
            if (throw_exception) {
                throw new RuntimeException(msg);
            }
        }
    }
    public static LocationsRoomDatabase getDatabase(final Context context) {
        if (locationsRoomDatabase == null) {
            synchronized (LocationsRoomDatabase.class) {
                if (locationsRoomDatabase == null) {
                    locationsRoomDatabase = Room.databaseBuilder(context.getApplicationContext(),
                                    LocationsRoomDatabase.class, "locations")
                            //.addMigrations(new MigrationTo2())
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return locationsRoomDatabase;
    }
}
