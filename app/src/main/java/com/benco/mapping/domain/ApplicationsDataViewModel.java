package com.benco.mapping.domain;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.data.ApplicationsDataRepository;

import java.util.List;

public class ApplicationsDataViewModel  extends AndroidViewModel {
    private ApplicationsDataRepository applicationsData;
    private LiveData<List<ApplicationsData>> listLiveData;
    private LiveData<List<ApplicationsData>> listAllLiveData;

    public ApplicationsDataViewModel(Application application) {
        super(application);
        applicationsData = new ApplicationsDataRepository(application);
    }
    public List<ApplicationsData> getAllApplicationsListVm(int aid) {
       return applicationsData.getAllApplicationsList(aid);
    }
    public LiveData<List<ApplicationsData>> getApplicationsFromVm(int aid) {
        listLiveData = applicationsData.getApplications(aid);
        return listLiveData;
    }
    public LiveData<List<ApplicationsData>> getAllApplicationsFromVm(int aid) {
        listAllLiveData = applicationsData.getAllApplications(aid);
        return listAllLiveData;
    }
    public int reloadAllApplicationsData(int aid) {
        return applicationsData.reloadAllApplicationsData(aid);
    }

    public void insertApplications(ApplicationsData application) {
        applicationsData.insertApplications(application);
    }
}