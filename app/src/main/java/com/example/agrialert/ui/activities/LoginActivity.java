package com.example.agrialert.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.agrialert.R;
import com.example.agrialert.ui.viewmodels.AuthViewModel;
import com.example.agrialert.utils.SessionManager;
import com.example.agrialert.utils.ValidationUtil;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private SessionManager sessionManager;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Bind Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        // Observe ViewModel Data
        authViewModel.getLoginSuccess().observe(this, user -> {
            sessionManager.saveUserSession(user.getId(), user.getName(), user.getEmail(), user.getRole());
            Toast.makeText(this, "Welcome " + user.getName(), Toast.LENGTH_SHORT).show();
            navigateToDashboard(user.getRole());
        });

        authViewModel.getLoginError().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        btnLogin.setOnClickListener(v -> performLogin());

        // Assuming btnRegister or tvRegister is used to traverse to RegisterActivity based on the xml setup
        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                startActivity(new Intent(this, RegisterActivity.class));
            });
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (ValidationUtil.isEmpty(email) || ValidationUtil.isEmpty(password)) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.login(email, password);
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        switch (role) {
            case "Farmer":
                intent = new Intent(this, FarmerDashboardActivity.class);
                break;
            case "Vet":
                // intent = new Intent(this, VetDashboardActivity.class);
                intent = new Intent(this, FarmerDashboardActivity.class); // Placeholder
                break;
            case "ADMIN":
            default:
                // intent = new Intent(this, AdminDashboardActivity.class);
                intent = new Intent(this, FarmerDashboardActivity.class); // Placeholder
                break;
        }
        startActivity(intent);
        finish();
    }
}
