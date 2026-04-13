package com.example.agrialert.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrialert.R;
import com.example.agrialert.model.Report;
import com.example.agrialert.ui.viewmodels.ViewReportsViewModel;
import com.example.agrialert.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

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

    private static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
        private List<Report> reports;

        public ReportAdapter(List<Report> reports) {
            this.reports = reports;
        }

        public void setReports(List<Report> reports) {
            this.reports = reports;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            Report report = reports.get(position);
            holder.tvAnimalType.setText(report.getAnimalType());
            holder.tvDate.setText(report.getDateObserved());
            holder.tvStatus.setText(report.getStatus());
        }

        @Override
        public int getItemCount() {
            return reports == null ? 0 : reports.size();
        }

        static class ReportViewHolder extends RecyclerView.ViewHolder {
            TextView tvAnimalType, tvDate, tvStatus;

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAnimalType = itemView.findViewById(R.id.tvReportAnimalType);
                tvDate = itemView.findViewById(R.id.tvReportDate);
                tvStatus = itemView.findViewById(R.id.tvReportStatus);
            }
        }
    }
}