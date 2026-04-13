package com.example.agrialert.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrialert.R;
import com.example.agrialert.utils.SessionManager;

public class HomeActivity extends AppCompatActivity {
    private Button btnGoLogin, btnGoRegister;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        // Auto-redirect if already logged in
        if (sessionManager.getUserId() != -1) {
            startActivity(new Intent(this, FarmerDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        btnGoLogin = findViewById(R.id.btnGoLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
        });

        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RegisterActivity.class));
        });
    }
}