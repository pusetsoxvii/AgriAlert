package com.agrialert.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.R
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityAdminDashboardBinding
import com.agrialert.app.ui.LoginActivity
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.agrialert.app.ui.farmer.NotificationsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var usersAdapter: UniversalAdapter
    private lateinit var reportsAdapter: UniversalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isAdmin) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupRecyclerViews()
        setupListeners()
        observeData()
    }

    private fun setupUI() {
        binding.tvAdminInitials.text = session.initials
    }

    private fun setupRecyclerViews() {
        usersAdapter = UniversalAdapter(
            onItemClick = { item ->
                if (item is ListItem.UserItem) {
                    val intent = Intent(this, UserDetailActivity::class.java)
                    intent.putExtra("user_id", item.user.id)
                    startActivity(intent)
                }
            },
            onActionClick = { item, action ->
                if (item is ListItem.UserItem) {
                    handleUserAction(item.user, action)
                }
            }
        )
        binding.rvRecentUsers.layoutManager = LinearLayoutManager(this)
        binding.rvRecentUsers.adapter = usersAdapter

        reportsAdapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.ReportItem) {
                val intent = Intent(this, AdminReportDetailActivity::class.java)
                intent.putExtra("report_id", item.report.id)
                startActivity(intent)
            }
        })
        binding.rvRecentReports.layoutManager = LinearLayoutManager(this)
        binding.rvRecentReports.adapter = reportsAdapter
    }

    private fun setupListeners() {
        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        binding.tvSeeAllUsers.setOnClickListener { startActivity(Intent(this, UserManagementActivity::class.java)) }
        binding.btnManageUsers.setOnClickListener { startActivity(Intent(this, UserManagementActivity::class.java)) }

        binding.tvSeeAllReports.setOnClickListener { startActivity(Intent(this, AdminReportListActivity::class.java)) }
        binding.btnAllReports.setOnClickListener { startActivity(Intent(this, AdminReportListActivity::class.java)) }

        binding.btnAllAlerts.setOnClickListener { startActivity(Intent(this, AdminAlertsListActivity::class.java)) }
        binding.btnViewStats.setOnClickListener { startActivity(Intent(this, AdminStatsActivity::class.java)) }

        binding.swipeRefresh.setOnRefreshListener {
            loadStats()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> {
                    startActivity(Intent(this, UserManagementActivity::class.java))
                    false
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, AdminReportListActivity::class.java))
                    false
                }
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AdminAlertsListActivity::class.java))
                    false
                }
                else -> true
            }
        }
    }

    private fun observeData() {
        loadStats()

        lifecycleScope.launch {
            repository.getAllUsers().collectLatest { users ->
                usersAdapter.updateUsers(users.take(5))
            }
        }

        lifecycleScope.launch {
            repository.getAllReports().collectLatest { reports ->
                reportsAdapter.updateReports(reports.take(5))
            }
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val stats = withContext(Dispatchers.IO) { repository.getAdminStats() }
            binding.tvTotalFarmers.text = stats.totalFarmers.toString()
            binding.tvTotalVets.text = stats.totalVets.toString()
            binding.tvTotalReports.text = stats.totalReports.toString()
            
            val openCases = stats.pendingReports + stats.investigatingReports
            binding.tvOpenCases.text = openCases.toString()
            binding.tvOpenCases.setTextColor(
                if (openCases > 0) android.graphics.Color.RED else android.graphics.Color.WHITE
            )

            binding.tvResolvedCases.text = stats.resolvedReports.toString()
            binding.tvAlertsSent.text = stats.totalAlerts.toString()
            binding.tvReportsWeek.text = stats.reportsThisWeek.toString()
            binding.tvNewUsers.text = stats.newUsersThisMonth.toString()
        }
    }

    private fun handleUserAction(user: User, action: String) {
        val title = if (action == "activate") "Activate User" else "Deactivate User"
        val message = if (action == "activate") "Activate ${user.name}? They will be able to log in."
        else "Deactivate ${user.name}? They will not be able to log in."

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(if (action == "activate") "Activate" else "Deactivate") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.updateUserStatus(user.id, if (action == "activate") "Active" else "Inactive")
                    }
                    loadStats()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_home
    }
}
