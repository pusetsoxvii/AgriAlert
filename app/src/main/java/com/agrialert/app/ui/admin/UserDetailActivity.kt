package com.agrialert.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DiseaseReport
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityUserDetailBinding
import com.agrialert.app.ui.adapter.roleColor
import com.agrialert.app.ui.adapter.toDisplayDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private var userId: Int = -1
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("user_id", -1)
        if (userId == -1) {
            finish()
            return
        }

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        setupListeners()
        loadUserData()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnToggleStatus.setOnClickListener {
            val isCurrentlyActive = user?.status == User.STATUS_ACTIVE
            val action = if (isCurrentlyActive) "Deactivate" else "Activate"
            
            AlertDialog.Builder(this)
                .setTitle("$action User")
                .setMessage("Are you sure you want to $action ${user?.name}?")
                .setPositiveButton(action) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            repository.updateUserStatus(userId, if (isCurrentlyActive) User.STATUS_INACTIVE else User.STATUS_ACTIVE)
                        }
                        loadUserData()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnViewReports.setOnClickListener {
            val intent = Intent(this, AdminReportListActivity::class.java)
            intent.putExtra("farmer_id", userId)
            intent.putExtra("farmer_name", user?.name)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            user = withContext(Dispatchers.IO) { repository.getUserById(userId) }
            user?.let { u ->
                binding.tvInitials.text = u.initials
                binding.tvInitials.setBackgroundColor(u.role.roleColor())
                binding.tvName.text = u.name
                binding.tvRole.text = u.role.uppercase()
                binding.tvStatus.text = u.status.uppercase()
                binding.tvEmail.text = u.email
                binding.tvPhone.text = u.phone
                binding.tvDistrict.text = u.district
                binding.tvMemberSince.text = "Member since ${u.createdDate.toDisplayDate()}"

                if (u.status == User.STATUS_ACTIVE) {
                    binding.btnToggleStatus.text = "Deactivate Account"
                    binding.btnToggleStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444"))
                } else {
                    binding.btnToggleStatus.text = "Activate Account"
                    binding.btnToggleStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981"))
                }

                if (u.role == User.ROLE_ADMIN) {
                    binding.btnToggleStatus.visibility = View.GONE
                    binding.tvAdminNote.visibility = View.VISIBLE
                    binding.statsSection.visibility = View.GONE
                } else {
                    binding.btnToggleStatus.visibility = View.VISIBLE
                    binding.tvAdminNote.visibility = View.GONE
                    binding.statsSection.visibility = View.VISIBLE
                    loadStats(u)
                }

                binding.btnViewReports.visibility = if (u.role == User.ROLE_FARMER) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadStats(u: User) {
        lifecycleScope.launch {
            if (u.role == User.ROLE_FARMER) {
                binding.tvStatLabel1.text = "Reports submitted"
                binding.tvStatValue1.text = withContext(Dispatchers.IO) { repository.countForFarmer(u.id).toString() }
                
                binding.tvStatLabel2.text = "Pending reports"
                binding.tvStatValue2.text = "Calculating..." // Simplified for demo
                
                binding.tvStatLabel3.text = "Resolved reports"
                binding.tvStatValue3.text = "Calculating..."
            } else if (u.role == User.ROLE_VET) {
                binding.tvStatLabel1.text = "Responses sent"
                binding.tvStatValue1.text = withContext(Dispatchers.IO) { repository.getVetResponseCount(u.id).toString() }
                
                binding.tvStatLabel2.text = "Farm visits scheduled"
                binding.tvStatValue2.text = withContext(Dispatchers.IO) { repository.getVetVisitCount(u.id).toString() }

                binding.tvStatLabel3.text = "Alerts sent"
                binding.tvStatValue3.text = withContext(Dispatchers.IO) { repository.getVetAlertCount(u.id).toString() }
            }
        }
    }
}
