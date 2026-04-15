package com.example.agrialert.ui.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.agrialert.model.Alert;
import com.example.agrialert.repository.AlertRepository;

import java.util.List;

public class AlertsViewModel extends AndroidViewModel {
    private AlertRepository repository;
    private MutableLiveData<List<Alert>> _alerts = new MutableLiveData<>();
    public LiveData<List<Alert>> getAlerts() { return _alerts; }

    public AlertsViewModel(Application application) {
        super(application);
        repository = new AlertRepository(application);
    }

    public void loadAlerts() {
        _alerts.setValue(repository.getAllAlerts());
    }
}