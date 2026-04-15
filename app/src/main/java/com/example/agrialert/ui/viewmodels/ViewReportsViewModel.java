package com.example.agrialert.ui.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.agrialert.model.Report;
import com.example.agrialert.repository.ReportRepository;

import java.util.List;

public class ViewReportsViewModel extends AndroidViewModel {
    private ReportRepository repository;
    private MutableLiveData<List<Report>> _reports = new MutableLiveData<>();
    public LiveData<List<Report>> getReports() { return _reports; }

    public ViewReportsViewModel(Application application) {
        super(application);
        repository = new ReportRepository(application);
    }

    public void loadUserReports(int userId) {
        _reports.setValue(repository.getReportsForUser(userId));
    }

    public boolean deleteReport(int reportId) {
        return repository.deleteReport(reportId);
    }
}