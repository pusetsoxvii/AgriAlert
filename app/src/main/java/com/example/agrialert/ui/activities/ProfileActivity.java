package com.example.agrialert.ui.activities;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.agrialert.R;
import com.example.agrialert.repository.UserRepository;
import com.example.agrialert.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private UserRepository userRepository;
    private EditText etName, etEmail, etOldPassword, etNewPassword, etConfirmPassword;
    private ImageView ivToggleOldPassword, ivToggleNewPassword, ivToggleConfirmPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

    sessionManager = new SessionManager(this);
    userRepository = new UserRepository(this);

        ImageView ivBack = findViewById(R.id.ivBack);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivToggleOldPassword = findViewById(R.id.ivToggleOldPassword);
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);

        etName.setText(sessionManager.getUserName());
        etEmail.setText(sessionManager.getUserEmail());

        ivBack.setOnClickListener(v -> onBackPressed());

    setupPasswordToggle(etOldPassword, ivToggleOldPassword);
    setupPasswordToggle(etNewPassword, ivToggleNewPassword);
    setupPasswordToggle(etConfirmPassword, ivToggleConfirmPassword);

        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Name is required");
                return;
            }

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email");
                return;
            }

            int id = sessionManager.getUserId();
            String role = sessionManager.getUserRole();

            boolean updated = userRepository.updateUserProfile(id, name, email);
            if (updated) {
                sessionManager.saveUserSession(id, name, email, role);
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString();
            String newPass = etNewPassword.getText().toString();
            String confirmPass = etConfirmPassword.getText().toString();

            if (TextUtils.isEmpty(oldPass)) {
                etOldPassword.setError("Current password required");
                return;
            }

            if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
                etNewPassword.setError("At least 6 characters");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }

            int id = sessionManager.getUserId();

            boolean changed = userRepository.changePassword(id, oldPass, newPass);
            if (changed) {
                Toast.makeText(this, "Password changed", Toast.LENGTH_SHORT).show();
                etOldPassword.setText("");
                etNewPassword.setText("");
                etConfirmPassword.setText("");
            } else {
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupPasswordToggle(EditText editText, ImageView toggleView) {
        if (editText == null || toggleView == null) return;

        toggleView.setOnClickListener(v -> {
            int inputType = editText.getInputType();
            boolean isPasswordVisible = (inputType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

            if (isPasswordVisible) {
                // Switch back to hidden password
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                // Show password
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }

            editText.setSelection(editText.getText().length());
        });
    }
}
