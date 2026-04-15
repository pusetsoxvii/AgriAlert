package com.example.agrialert.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrialert.R;
import com.example.agrialert.model.Alert;
import com.example.agrialert.ui.viewmodels.AlertsViewModel;

import java.util.List;

public class AlertsActivity extends AppCompatActivity {

    private AlertsViewModel viewModel;
    private RecyclerView rvAlerts;
    private AlertsAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        ImageView ivBack = findViewById(R.id.ivBack);
    rvAlerts = findViewById(R.id.rvAlerts);
    tvEmpty = findViewById(R.id.tvEmptyAlerts);

        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlertsAdapter();
        rvAlerts.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AlertsViewModel.class);
        viewModel.getAlerts().observe(this, alerts -> {
            if (alerts == null || alerts.isEmpty()) {
                rvAlerts.setVisibility(View.GONE);
                if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            } else {
                rvAlerts.setVisibility(View.VISIBLE);
                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                adapter.setAlerts(alerts);
            }
        });

        viewModel.loadAlerts();

        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private static class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

        private List<Alert> alerts;

        public void setAlerts(List<Alert> alerts) {
            this.alerts = alerts;
            notifyDataSetChanged();
        }

        @Override
        public AlertViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
            return new AlertViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AlertViewHolder holder, int position) {
            Alert alert = alerts.get(position);
            holder.tvTitle.setText(alert.getTitle());
            holder.tvMessage.setText(alert.getMessage());
            holder.tvDate.setText(alert.getDateCreated());
        }

        @Override
        public int getItemCount() {
            return alerts == null ? 0 : alerts.size();
        }

        static class AlertViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvDate;

            public AlertViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvAlertTitle);
                tvMessage = itemView.findViewById(R.id.tvAlertMessage);
                tvDate = itemView.findViewById(R.id.tvAlertDate);
            }
        }
    }
}