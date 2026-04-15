package com.example.agrialert.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrialert.R;
import com.example.agrialert.repository.AlertRepository;
import com.example.agrialert.ui.adapters.ReportAdapter;
import com.example.agrialert.ui.viewmodels.FarmerDashboardViewModel;
import com.example.agrialert.utils.SessionManager;

import java.util.ArrayList;

public class FarmerDashboardActivity extends AppCompatActivity {
    private FarmerDashboardViewModel viewModel;
    private SessionManager sessionManager;
    private TextView tvWelcomeBack, tvAvatarInitials, tvTotalReports, tvPendingReports;
    private View layoutAlertBanner;
    private Button btnNewReport, btnViewReports;
    private RecyclerView rvRecentReports;
    private ReportAdapter adapter;

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
            android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(FarmerDashboardActivity.this, v);
            popupMenu.getMenu().add("Edit Profile");
            popupMenu.getMenu().add("Sign Out");
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Sign Out")) {
                    sessionManager.logout();
                    Toast.makeText(FarmerDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, HomeActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getTitle().equals("Edit Profile")) {
                    // Placeholder for Edit Profile functionality
                    Toast.makeText(FarmerDashboardActivity.this, "Edit Profile clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        findViewById(R.id.navReports).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, ViewReportsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navAlerts).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, AlertsActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
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
        tvPendingReports = findViewById(R.id.tvPendingReports);
    layoutAlertBanner = findViewById(R.id.layoutAlertBanner);
        btnNewReport = findViewById(R.id.btnReportSick);
        btnViewReports = findViewById(R.id.btnViewReports);
        
        rvRecentReports = findViewById(R.id.rvRecentReports);
        rvRecentReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>());
        rvRecentReports.setAdapter(adapter);

        // Dynamically set user info from session
        String userName = sessionManager.getUserName();
        tvWelcomeBack.setText("Hello, " + userName);
        if (userName != null && !userName.isEmpty()) {
            tvAvatarInitials.setText(userName.substring(0, 1).toUpperCase());
        }

        // Show alert banner only if there are alerts in DB
        AlertRepository alertRepository = new AlertRepository(this);
        if (!alertRepository.getAllAlerts().isEmpty()) {
            layoutAlertBanner.setVisibility(View.VISIBLE);
            layoutAlertBanner.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(FarmerDashboardActivity.this, AlertsActivity.class);
                startActivity(intent);
            });
        } else {
            layoutAlertBanner.setVisibility(View.GONE);
        }
    }

    private void setupObservers() {
        viewModel.getTotalReports().observe(this, count -> {
            if (count != null) {
                tvTotalReports.setText(String.valueOf(count));
            }
        });

        viewModel.getPendingReports().observe(this, count -> {
            if (count != null) {
                tvPendingReports.setText(String.valueOf(count));
            }
        });

        viewModel.getRecentReports().observe(this, reports -> {
            if (reports != null) {
                adapter.setReports(reports);
            }
        });
    }
}