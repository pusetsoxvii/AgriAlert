package com.example.agrialert.ui.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.agrialert.repository.ReportRepository;

public class FarmerDashboardViewModel extends AndroidViewModel {
    private ReportRepository repository;

    private MutableLiveData<Integer> _totalReports = new MutableLiveData<>(0);
    public LiveData<Integer> getTotalReports() { return _totalReports; }

    private MutableLiveData<Integer> _pendingReports = new MutableLiveData<>(0);
    public LiveData<Integer> getPendingReports() { return _pendingReports; }

    public FarmerDashboardViewModel(Application application) {
        super(application);
        repository = new ReportRepository(application);
    }

    public void loadDashboardStats(int userId) {
        int total = repository.getTotalReportsCount(userId);
        int pending = repository.getPendingReportsCount(userId);
        
        _totalReports.setValue(total);
        _pendingReports.setValue(pending);
    }
}
