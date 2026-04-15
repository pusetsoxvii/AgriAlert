package com.example.agrialert.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.agrialert.R;
import com.example.agrialert.ui.viewmodels.AuthViewModel;
import com.example.agrialert.utils.ValidationUtil;

public class RegisterActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;

    private EditText etName, etEmail, etPassword;
    private Spinner spinRole;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private ImageView ivToggleRegPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

    etName = findViewById(R.id.etName);
    etEmail = findViewById(R.id.etRegEmail);
    etPassword = findViewById(R.id.etRegPassword);
    ivToggleRegPassword = findViewById(R.id.ivToggleRegPassword);
        spinRole = findViewById(R.id.spinRole);
        btnRegister = findViewById(R.id.btnDoRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        authViewModel.getRegisterSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                finish(); // Returns to Login Activity
            }
        });

        authViewModel.getRegisterError().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

    btnRegister.setOnClickListener(v -> attemptRegistration());

    setupPasswordToggle(etPassword, ivToggleRegPassword);

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = spinRole.getSelectedItem().toString();

        if (ValidationUtil.isEmpty(name) || ValidationUtil.isEmpty(email) || ValidationUtil.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.register(name, email, password, role);
    }

    private void setupPasswordToggle(EditText editText, ImageView toggleView) {
        if (editText == null || toggleView == null) return;

        toggleView.setOnClickListener(v -> {
            int inputType = editText.getInputType();
            boolean isPasswordVisible = (inputType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

            if (isPasswordVisible) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }

            editText.setSelection(editText.getText().length());
        });
    }
}
