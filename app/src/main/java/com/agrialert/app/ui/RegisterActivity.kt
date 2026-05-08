package com.agrialert.app.ui

import android.R
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.Alert
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var repository: AgriAlertRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)

        setupSpinners()

        binding.btnRegister.setOnClickListener { attemptRegister() }
    }

    private fun setupSpinners() {
        val roles = listOf("Select role", User.ROLE_FARMER, "Vet Officer")
        binding.spinnerRole.adapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, roles)

        val districts = listOf("Select district") + Alert.DISTRICTS.filter { it != "All districts" }
        binding.spinnerDistrict.adapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, districts)
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim().lowercase()
        val phone = binding.etPhone.text.toString().trim()
        val roleDisplay = binding.spinnerRole.selectedItem.toString()
        val district = binding.spinnerDistrict.selectedItem.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Enter your full name"
            isValid = false
        } else binding.tilName.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Enter your email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        } else binding.tilEmail.error = null

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Enter your phone number"
            isValid = false
        } else binding.tilPhone.error = null

        if (roleDisplay == "Select role") {
            Toast.makeText(this, "Select your role", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (district == "Select district") {
            Toast.makeText(this, "Select your district", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Create a password"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Minimum 6 characters"
            isValid = false
        } else binding.tilPassword.error = null

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else binding.tilConfirmPassword.error = null

        if (!isValid) return

        val role = if (roleDisplay == "Vet Officer") User.ROLE_VET else User.ROLE_FARMER

        setLoading(true)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.register(
                    User(
                        name = name,
                        email = email,
                        phone = phone,
                        role = role,
                        district = district,
                        password = password,
                        createdDate = repository.today()
                    )
                )
            }

            setLoading(false)

            result.onSuccess {
                Toast.makeText(this@RegisterActivity, "Account created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                binding.tilEmail.error = it.message
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.text = if (loading) "" else "Create account"
    }
}
