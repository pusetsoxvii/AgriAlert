package com.example.agrialert.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.agrialert.R;

public class ReportDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

    ImageView ivImage = findViewById(R.id.ivReportDetailImage);
    ImageView ivBack = findViewById(R.id.ivBack);
    TextView tvAnimalType = findViewById(R.id.tvReportDetailAnimalType);
    TextView tvStatus = findViewById(R.id.tvReportDetailStatus);
    TextView tvDate = findViewById(R.id.tvReportDetailDate);
    TextView tvDescription = findViewById(R.id.tvReportDetailDescription);
    Button btnEditReport = findViewById(R.id.btnEditReport);

        ivBack.setOnClickListener(v -> onBackPressed());

        Intent intent = getIntent();
    int reportId = intent.getIntExtra("reportId", -1);
    String animalType = intent.getStringExtra("animalType");
        String status = intent.getStringExtra("status");
        String date = intent.getStringExtra("date");
        String description = intent.getStringExtra("description");
        String imagePath = intent.getStringExtra("imagePath");

        if (animalType != null) tvAnimalType.setText(animalType);
        if (status != null) tvStatus.setText(status);
        if (date != null) tvDate.setText(date);
        if (description != null) tvDescription.setText(description);

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                ivImage.setImageURI(Uri.parse(imagePath));
            } catch (Exception e) {
                ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        if (status != null) {
            if ("Pending".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundColor(Color.parseColor("#FFA000"));
            } else if ("Reviewing".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundColor(Color.parseColor("#1976D2"));
            } else if ("Visit scheduled".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundColor(Color.parseColor("#7B1FA2"));
            } else if ("Resolved".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundColor(Color.parseColor("#388E3C"));
            } else {
                tvStatus.setBackgroundColor(Color.parseColor("#88000000"));
            }
        }

        if (reportId == -1) {
            btnEditReport.setEnabled(false);
            btnEditReport.setAlpha(0.5f);
        } else {
            btnEditReport.setOnClickListener(v -> {
                Intent editIntent = new Intent(this, ReportFormActivity.class);
                editIntent.putExtra("reportId", reportId);
                editIntent.putExtra("animalType", animalType);
                editIntent.putExtra("description", description);
                // For simplicity we reuse date/location/image as-is from DB when saving; here just pass basics
                startActivity(editIntent);
            });
        }
    }
}
