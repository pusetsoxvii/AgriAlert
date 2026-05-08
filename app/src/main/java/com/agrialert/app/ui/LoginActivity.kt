package com.agrialert.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DatabaseSeeder
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityLoginBinding
import com.agrialert.app.ui.admin.AdminDashboardActivity
import com.agrialert.app.ui.farmer.FarmerDashboardActivity
import com.agrialert.app.ui.vet.VetDashboardActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        // Seed database if first launch
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                DatabaseSeeder(this@LoginActivity).seedIfNeeded()
            }
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnForgotPassword.setOnClickListener {
            Toast.makeText(this, "Contact your administrator", Toast.LENGTH_SHORT).show()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        var isValid = true
        if (email.isEmpty()) {
            binding.tilEmail.error = "Enter your email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Enter your password"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (!isValid) return

        setLoading(true)

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                repository.login(email, password)
            }

            setLoading(false)

            if (user != null) {
                if (user.status == User.STATUS_INACTIVE) {
                    binding.tilEmail.error = "Account deactivated. Contact your administrator."
                } else {
                    session.save(user)
                    navigateToDashboard(user.role)
                }
            } else {
                Toast.makeText(this@LoginActivity, "Incorrect email or password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role) {
            User.ROLE_FARMER -> Intent(this, FarmerDashboardActivity::class.java)
            User.ROLE_VET -> Intent(this, VetDashboardActivity::class.java)
            User.ROLE_ADMIN -> Intent(this, AdminDashboardActivity::class.java)
            else -> {
                session.clear()
                return
            }
        }
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.text = if (loading) "" else "Sign in"
    }
}
