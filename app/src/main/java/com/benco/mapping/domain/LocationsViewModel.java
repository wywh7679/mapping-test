package com.benco.mapping.domain;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class LocationsViewModel extends AndroidViewModel {
    private LocationsRepository locationsRepository;
    private final LiveData<List<Locations>> listLiveData;

    public LocationsViewModel(Application application) {
        super(application);
        locationsRepository = new LocationsRepository(application);
        listLiveData = locationsRepository.getAllLocations();
    }

    public LiveData<List<Locations>> getAllLocationsFromVm() {

        return listLiveData;
    }

    public long insertLocations(Locations locations) throws ExecutionException, InterruptedException {
        return locationsRepository.insertLocations(locations);
    }

    public void deleteLocationById(int lid) {
        locationsRepository.deleteLocationById(lid);
    }
}
