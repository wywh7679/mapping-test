package com.benco.mapping.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ApplicationsRepository {
    LocationsRoomDatabase locationsRoomDatabase;
    ApplicationsDao applicationsDao;
    private LiveData<List<Applications>> listApplications;
    private LiveData<List<Applications>> listApplicationsById;

    public ApplicationsRepository(Application application) {
        locationsRoomDatabase = LocationsRoomDatabase.getDatabase(application);
        applicationsDao = locationsRoomDatabase.applicationsDao();
        listApplications = applicationsDao.getApplications();
    }

    public LiveData<List<Applications>> getApplicationByLid(int lid) {
        return listApplicationsById = applicationsDao.getApplicationByLid(lid);
    }
    public LiveData<List<Applications>> getApplication(int aid) {
        return listApplicationsById = applicationsDao.getApplication(aid);
    }

    public long insertApplications(Applications application) throws ExecutionException, InterruptedException {
        return LocationsRoomDatabase.databaseWriteExecutor.submit(() -> applicationsDao.insert(application)).get();
    }

    public LiveData<List<Applications>> getAllApplications() {
        return listApplications;
    }
}
