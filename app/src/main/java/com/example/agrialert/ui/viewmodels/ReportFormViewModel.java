package com.example.agrialert.ui.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.agrialert.repository.ReportRepository;

public class ReportFormViewModel extends AndroidViewModel {
    private ReportRepository repository;
    private MutableLiveData<Boolean> _reportSubmissionStatus = new MutableLiveData<>();
    public LiveData<Boolean> getReportSubmissionStatus() { return _reportSubmissionStatus; }
    private MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public ReportFormViewModel(Application application) {
        super(application);
        repository = new ReportRepository(application);
    }

    public void submitReport(int userId, String animalType, String symptoms, String dateObserved, double lat, double lng, String imagePath) {
        if (animalType == null || animalType.trim().isEmpty()) {
            _errorMessage.setValue("Animal type is required.");
            return;
        }
        if (symptoms == null || symptoms.trim().isEmpty()) {
            _errorMessage.setValue("Symptoms are required.");
            return;
        }
        boolean success = repository.insertReport(userId, animalType, symptoms, 1, dateObserved, lat, lng, imagePath);
        if (success) {
            _reportSubmissionStatus.setValue(true);
        } else {
            _errorMessage.setValue("Failed to submit report. Try again.");
        }
    }
}