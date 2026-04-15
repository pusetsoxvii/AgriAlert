package com.example.agrialert.ui.activities;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

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