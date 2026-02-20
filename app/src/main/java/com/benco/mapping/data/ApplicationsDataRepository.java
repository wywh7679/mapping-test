package com.benco.mapping.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ApplicationsDataRepository {
    LocationsRoomDatabase locationsRoomDatabase;
    ApplicationsDataDao applicationsDataDao;
    private LiveData<List<ApplicationsData>> listApplications;
    private LiveData<List<ApplicationsData>> listAllApplications;

    public ApplicationsDataRepository(Application application) {
        locationsRoomDatabase = LocationsRoomDatabase.getDatabase(application);
        applicationsDataDao = locationsRoomDatabase.applicationsDataDao();
    }

    public void insertApplications(ApplicationsData applicationsData) {
        LocationsRoomDatabase.databaseWriteExecutor.execute(() -> applicationsDataDao.insert(applicationsData));
    }
    public List<ApplicationsData> getAllApplicationsList(int aid) {
        return applicationsDataDao.getAllApplicationsDataList(aid);
    }
    public LiveData<List<ApplicationsData>> getApplications(int aid) {
        return listApplications = applicationsDataDao.getApplicationsData(aid);
    }
    public int reloadAllApplicationsData(int aid) {
        return applicationsDataDao.reloadAllApplicationsData(aid);
    }
    public LiveData<List<ApplicationsData>> getAllApplications(int aid) {
        listAllApplications = applicationsDataDao.getAllApplicationsData(aid);
        return listAllApplications;
    }
}