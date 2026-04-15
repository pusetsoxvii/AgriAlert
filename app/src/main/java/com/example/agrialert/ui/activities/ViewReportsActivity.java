package com.example.agrialert.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrialert.R;
import com.example.agrialert.ui.adapters.ReportAdapter;
import com.example.agrialert.model.Report;
import com.example.agrialert.ui.viewmodels.ViewReportsViewModel;
import com.example.agrialert.utils.SessionManager;

import java.util.ArrayList;

public class ViewReportsActivity extends AppCompatActivity {
    private ViewReportsViewModel viewModel;
    private SessionManager sessionManager;
    private RecyclerView recyclerView;
    private ReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reports);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ViewReportsViewModel.class);

    recyclerView = findViewById(R.id.recyclerViewReports);
    View ivBack = findViewById(R.id.ivBack);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter = new ReportAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnReportLongClickListener((report, position) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete report")
                    .setMessage("Are you sure you want to delete this report?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        boolean deleted = viewModel.deleteReport(report.getId());
                        if (deleted) {
                            Toast.makeText(this, "Report deleted", Toast.LENGTH_SHORT).show();
                            int userId1 = sessionManager.getUserId();
                            if (userId1 != -1) {
                                viewModel.loadUserReports(userId1);
                            }
                        } else {
                            Toast.makeText(this, "Failed to delete report", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        ivBack.setOnClickListener(v -> onBackPressed());

        viewModel.getReports().observe(this, reports -> {
            if (reports != null) {
                adapter.setReports(reports);
            }
        });

        int userId = sessionManager.getUserId();
        if (userId != -1) {
            viewModel.loadUserReports(userId);
        }
    }
}