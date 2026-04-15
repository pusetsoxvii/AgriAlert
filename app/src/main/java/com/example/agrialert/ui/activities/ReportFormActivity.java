package com.example.agrialert.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.agrialert.R;
import com.example.agrialert.ui.viewmodels.ReportFormViewModel;
import com.example.agrialert.utils.SessionManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportFormActivity extends AppCompatActivity {
    private ReportFormViewModel viewModel;
    private SessionManager sessionManager;
    private LocationManager locationManager;
    private com.example.agrialert.repository.ReportRepository reportRepository;

    private EditText etAnimalType, etDescription, etNumberAffected;
    private TextView tvLocation;
    private ImageView ivPhotoPreview;
    private Button btnGetLocation, btnTakePhoto, btnUploadPhoto, btnSubmitReport;

    private double latitude = 0.0;
    private double longitude = 0.0;
    private String imagePath = "";
    private int editingReportId = -1;

    private ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            result -> {
                if (result != null) {
                    ivPhotoPreview.setImageBitmap(result);
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    imagePath = "preview_image"; 
                }
            }
    );

    private ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    ivPhotoPreview.setImageURI(uri);
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    imagePath = uri.toString();
                }
            }
    );

    private ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean locFine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean locCoarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (locFine || locCoarse) {
                    getLastLocation();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    takePictureLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_report_form);

    sessionManager = new SessionManager(this);
    viewModel = new ViewModelProvider(this).get(ReportFormViewModel.class);
    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    reportRepository = new com.example.agrialert.repository.ReportRepository(this);

    initViews();
    setupListeners();
    setupObservers();

    // Check if we are editing an existing report and, if so, load it from DB to prefill
    editingReportId = getIntent().getIntExtra("reportId", -1);
    if (editingReportId != -1) {
        com.example.agrialert.model.Report existing = reportRepository.getReportById(editingReportId);
        if (existing != null) {
            if (existing.getAnimalType() != null) {
                etAnimalType.setText(existing.getAnimalType());
            }
            if (existing.getSymptoms() != null) {
                etDescription.setText(existing.getSymptoms());
            }
            latitude = existing.getLatitude();
            longitude = existing.getLongitude();
            if (existing.getLatitude() != 0.0 || existing.getLongitude() != 0.0) {
                tvLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f\nLng: %.4f",
                        existing.getLatitude(), existing.getLongitude()));
            }

            String existingImagePath = existing.getImagePath();
            if (existingImagePath != null && !existingImagePath.isEmpty()) {
                try {
                    ivPhotoPreview.setImageURI(android.net.Uri.parse(existingImagePath));
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    imagePath = existingImagePath;
                } catch (Exception e) {
                    // ignore and leave preview hidden/empty
                }
            }
        }
    }

    View ivBack = findViewById(R.id.ivBack);
    if (ivBack != null) {
        ivBack.setOnClickListener(v -> onBackPressed());
    }
    }

    private void initViews() {
    etAnimalType = findViewById(R.id.etAnimalType);
    etDescription = findViewById(R.id.etDescription);
    etNumberAffected = findViewById(R.id.etNumberAffected);
        tvLocation = findViewById(R.id.tvLocation);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
    }

    private void setupListeners() {
        btnGetLocation.setOnClickListener(v -> checkLocationPermissionAndGet());
        btnTakePhoto.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                takePictureLauncher.launch(null);
            }
        });
        btnUploadPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSubmitReport.setOnClickListener(v -> {
            int userId = sessionManager.getUserId();
            String animalType = etAnimalType.getText().toString();
            String symptoms = etDescription.getText().toString();
            String numberStr = etNumberAffected.getText().toString().trim();
            int numberAffected = 0;
            if (!numberStr.isEmpty()) {
                try {
                    numberAffected = Integer.parseInt(numberStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Enter a valid number of animals affected", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (numberAffected <= 0) {
                Toast.makeText(this, "Number of animals affected must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            if (editingReportId == -1) {
                viewModel.submitReport(userId, animalType, symptoms, numberAffected, date, latitude, longitude, imagePath);
            } else {
                viewModel.updateReport(editingReportId, userId, animalType, symptoms, numberAffected, date, latitude, longitude, imagePath);
            }
        });
    }

    private void setupObservers() {
        viewModel.getReportSubmissionStatus().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkLocationPermissionAndGet() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        try {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location location = null;

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) { }
                });
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (isGpsEnabled && location == null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) { }
                });
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String addressName = addresses.get(0).getAddressLine(0);
                        if (addressName != null) {
                            tvLocation.setText(addressName);
                        } else {
                            tvLocation.setText(addresses.get(0).getLocality());
                        }
                    } else {
                        tvLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f\nLng: %.4f", latitude, longitude));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    tvLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f\nLng: %.4f", latitude, longitude));
                }
            } else {
                tvLocation.setText("Location not found");
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}