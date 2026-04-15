package com.example.agrialert.ui.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrialert.R;
import com.example.agrialert.model.Report;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private List<Report> reports;

    public interface OnReportLongClickListener {
        void onReportLongClicked(Report report, int position);
    }

    private OnReportLongClickListener longClickListener;

    public ReportAdapter(List<Report> reports) {
        this.reports = reports;
    }

    public void setOnReportLongClickListener(OnReportLongClickListener listener) {
        this.longClickListener = listener;
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
        holder.tvDescription.setText(report.getSymptoms());
        holder.tvDate.setText(report.getDateObserved());
    holder.tvStatus.setText(report.getStatus());

        // Reset image first to avoid recycled image issues
        holder.ivImage.setImageDrawable(null);
        String imagePath = report.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Uri uri = Uri.parse(imagePath);
                holder.ivImage.setImageURI(uri);
                if (holder.ivImage.getDrawable() == null) {
                    holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            } catch (Exception e) {
                holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        String status = report.getStatus();
        if ("Pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FFA000")); // Amber
        } else if ("Reviewing".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#1976D2")); // Blue
        } else if ("Visit scheduled".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#7B1FA2")); // Purple
        } else if ("Resolved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#388E3C")); // Green
        } else {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#88000000")); // Default black transparent
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), com.example.agrialert.ui.activities.ReportDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            intent.putExtra("animalType", report.getAnimalType());
            intent.putExtra("status", report.getStatus());
            intent.putExtra("date", report.getDateObserved());
            intent.putExtra("description", report.getSymptoms());
            intent.putExtra("imagePath", report.getImagePath());
            v.getContext().startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onReportLongClicked(report, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return reports == null ? 0 : reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvAnimalType, tvDescription, tvDate, tvStatus;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivReportImage);
            tvAnimalType = itemView.findViewById(R.id.tvReportAnimalType);
            tvDescription = itemView.findViewById(R.id.tvReportDescription);
            tvDate = itemView.findViewById(R.id.tvReportDate);
            tvStatus = itemView.findViewById(R.id.tvReportStatus);
        }
    }
}
