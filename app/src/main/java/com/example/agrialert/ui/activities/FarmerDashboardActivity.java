package com.example.agrialert.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.agrialert.R;
import com.example.agrialert.ui.viewmodels.FarmerDashboardViewModel;
import com.example.agrialert.utils.SessionManager;

public class FarmerDashboardActivity extends AppCompatActivity {
    private FarmerDashboardViewModel viewModel;
    private SessionManager sessionManager;
    private TextView tvWelcomeBack, tvAvatarInitials, tvTotalReports, tvPendingReports;
    private Button btnNewReport, btnViewReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(FarmerDashboardViewModel.class);

        initViews();
        setupObservers();

        int farmerId = sessionManager.getUserId();
        if (farmerId != -1) {
            viewModel.loadDashboardStats(farmerId);
        } else {
            Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnNewReport.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, ReportFormActivity.class);
            startActivity(intent);
        });

        btnViewReports.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, ViewReportsActivity.class);
            startActivity(intent);
        });

        tvAvatarInitials.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(FarmerDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, HomeActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int farmerId = sessionManager.getUserId();
        if (farmerId != -1) {
            viewModel.loadDashboardStats(farmerId);
        }
    }

    private void initViews() {
        tvWelcomeBack = findViewById(R.id.tvWelcomeBack);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        
        // Dynamically set user info from session
        String userName = sessionManager.getUserName();
        tvWelcomeBack.setText("Good morning, " + userName);
        if (userName != null && !userName.isEmpty()) {
            tvAvatarInitials.setText(userName.substring(0, 1).toUpperCase());
        }
        tvPendingReports = findViewById(R.id.tvPendingReports);
        btnNewReport = findViewById(R.id.btnReportSick);
        btnViewReports = findViewById(R.id.btnViewReports);
    }

    private void setupObservers() {
        viewModel.getTotalReports().observe(this, total -> {
            if (total != null) {
                tvTotalReports.setText(String.valueOf(total));
            }
        });

        viewModel.getPendingReports().observe(this, pending -> {
            if (pending != null) {
                tvPendingReports.setText(String.valueOf(pending));
            }
        });
    }
}