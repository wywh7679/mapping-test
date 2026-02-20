package com.benco.mapping.domain;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ApplicationsViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public ApplicationsViewModelFactory(Application myApplication) {
        application = myApplication;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ApplicationsViewModel(application);
    }
}