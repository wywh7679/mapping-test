package com.benco.mapping.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class LocationsRepository {
    LocationsRoomDatabase locationsRoomDatabase;
    LocationsDao locationsDao;
    private LiveData<List<Locations>> listLocations;

    public LocationsRepository(Application application) {
        locationsRoomDatabase = LocationsRoomDatabase.getDatabase(application);
        locationsDao = locationsRoomDatabase.locationsDao();

    }
    public long insertLocations(Locations locations) throws ExecutionException, InterruptedException {
        return LocationsRoomDatabase.databaseWriteExecutor.submit(() -> locationsDao.insert(locations)).get();
    }

    public LiveData<List<Locations>> getAllLocations() {
        listLocations = locationsDao.getLocations();
        return listLocations;
    }

    public void deleteLocationById(int lid) {
        LocationsRoomDatabase.databaseWriteExecutor.execute(() -> {
            locationsRoomDatabase.applicationsDataDao().deleteByLid(lid);
            locationsRoomDatabase.applicationsDao().deleteApplicationsByLid(lid);
            locationsDao.deleteLocationById(lid);
        });
    }
}
