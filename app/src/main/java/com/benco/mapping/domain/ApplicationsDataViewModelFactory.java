package com.benco.mapping.domain;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ApplicationsDataViewModelFactory  implements ViewModelProvider.Factory {
    private final Application application;

    public ApplicationsDataViewModelFactory(Application myApplication) {
        application = myApplication;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ApplicationsDataViewModel(application);
    }
}