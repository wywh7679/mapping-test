package com.benco.mapping.domain;
import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.ApplicationsRepository;
import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ApplicationsViewModel extends AndroidViewModel {
    private ApplicationsRepository applications;
    private final LiveData<List<Applications>> listLiveData;
    private LiveData<List<Applications>> listApplicationByLid;

    public ApplicationsViewModel(Application application) {
        super(application);
        applications = new ApplicationsRepository(application);
        listLiveData = applications.getAllApplications();
    }

    public LiveData<List<Applications>> getAllApplicationsByLid(int lid) {
        return listApplicationByLid = applications.getApplicationByLid(lid);
    }
    public LiveData<List<Applications>> getApplication(int aid) {
        return listApplicationByLid = applications.getApplication(aid);
    }

    public LiveData<List<Applications>> getAllApplicationsFromVm() {
        return listLiveData;
    }

    public long insertApplications(Applications application) throws ExecutionException, InterruptedException {
        return applications.insertApplications(application);
    }
}