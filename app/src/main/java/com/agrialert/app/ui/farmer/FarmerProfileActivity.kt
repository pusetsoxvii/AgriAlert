package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityFarmerProfileBinding
import com.agrialert.app.ui.LoginActivity
import com.agrialert.app.ui.adapter.toDisplayDate
import com.agrialert.app.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FarmerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFarmerProfileBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFarmerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isFarmer) {
            finish()
            return
        }

        setupListeners()
        loadProfile()
        observeStats()
        updateLanguageDisplay()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnEditName.setOnClickListener { toggleNameEdit(true) }
        binding.btnSaveName.setOnClickListener { saveName() }

        binding.btnEditPhone.setOnClickListener { togglePhoneEdit(true) }
        binding.btnSavePhone.setOnClickListener { savePhone() }

        binding.btnChangePassword.setOnClickListener { changePassword() }

        binding.btnLanguage.setOnClickListener { showLanguageDialog() }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            currentUser = withContext(Dispatchers.IO) {
                repository.getUserById(session.id)
            }
            currentUser?.let { user ->
                binding.tvInitials.text = session.initials
                binding.tvName.text = user.name
                binding.etName.setText(user.name)
                binding.tvEmail.text = user.email
                binding.etPhone.setText(user.phone)
                binding.tvDistrict.text = user.district
                binding.tvMemberSince.text = "Member since ${user.createdDate.toDisplayDate()}"
            }
        }
    }

    private fun observeStats() {
        lifecycleScope.launch {
            repository.getFarmerTotal(session.id).collectLatest { count ->
                binding.tvTotalReports.text = count.toString()
            }
        }
    }

    private fun updateLanguageDisplay() {
        val currentLang = session.language
        binding.tvCurrentLanguage.text = if (currentLang == "st") "Sesotho" else "English"
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Sesotho")
        val checkedItem = if (session.language == "st") 1 else 0

        AlertDialog.Builder(this)
            .setTitle("Select Language / Khetha Puo")
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val code = if (which == 1) "st" else "en"
                session.setLanguage(code)
                LocaleHelper.updateResources(this, code)
                
                dialog.dismiss()
                
                // Restart activity to apply language
                val intent = Intent(this, FarmerDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .show()
    }

    private fun toggleNameEdit(editing: Boolean) {
        binding.etName.isEnabled = editing
        binding.btnEditName.visibility = if (editing) View.GONE else View.VISIBLE
        binding.btnSaveName.visibility = if (editing) View.VISIBLE else View.GONE
        if (editing) binding.etName.requestFocus()
    }

    private fun saveName() {
        val newName = binding.etName.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            currentUser?.let { user ->
                val updatedUser = user.copy(name = newName)
                withContext(Dispatchers.IO) {
                    repository.updateUser(updatedUser)
                }
                session.save(updatedUser)
                binding.tvName.text = newName
                toggleNameEdit(false)
                Toast.makeText(this@FarmerProfileActivity, "Name updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun togglePhoneEdit(editing: Boolean) {
        binding.etPhone.isEnabled = editing
        binding.btnEditPhone.visibility = if (editing) View.GONE else View.VISIBLE
        binding.btnSavePhone.visibility = if (editing) View.VISIBLE else View.GONE
        if (editing) binding.etPhone.requestFocus()
    }

    private fun savePhone() {
        val newPhone = binding.etPhone.text.toString().trim()
        if (newPhone.isEmpty()) {
            Toast.makeText(this, "Phone cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            currentUser?.let { user ->
                val updatedUser = user.copy(phone = newPhone)
                withContext(Dispatchers.IO) {
                    repository.updateUser(updatedUser)
                }
                session.save(updatedUser)
                togglePhoneEdit(false)
                Toast.makeText(this@FarmerProfileActivity, "Phone updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePassword() {
        val current = binding.etCurrentPassword.text.toString()
        val newPass = binding.etNewPassword.text.toString()
        val confirm = binding.etConfirmNewPassword.text.toString()

        val user = currentUser ?: return

        if (current != user.password) {
            binding.tilCurrentPassword.error = "Incorrect current password"
            return
        } else binding.tilCurrentPassword.error = null

        if (newPass.length < 6) {
            binding.tilNewPassword.error = "Minimum 6 characters"
            return
        } else binding.tilNewPassword.error = null

        if (newPass != confirm) {
            binding.tilConfirmNewPassword.error = "Passwords do not match"
            return
        } else binding.tilConfirmNewPassword.error = null

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateUser(user.copy(password = newPass))
            }
            Toast.makeText(this@FarmerProfileActivity, "Password changed successfully", Toast.LENGTH_SHORT).show()
            binding.etCurrentPassword.text?.clear()
            binding.etNewPassword.text?.clear()
            binding.etConfirmNewPassword.text?.clear()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Logout") { _, _ ->
                session.clear()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
