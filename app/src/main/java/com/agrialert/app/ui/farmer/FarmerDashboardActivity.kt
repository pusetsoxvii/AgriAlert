package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityFarmerDashboardBinding
import com.agrialert.app.ui.LoginActivity
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FarmerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFarmerDashboardBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFarmerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isFarmer) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        observeData()

        binding.btnReportAnimal.setOnClickListener {
            startActivity(Intent(this, ReportFormActivity::class.java))
        }

        binding.btnViewReports.setOnClickListener {
            startActivity(Intent(this, ReportHistoryActivity::class.java))
        }

        binding.btnKnowledgeBase.setOnClickListener {
            startActivity(Intent(this, KnowledgeBaseActivity::class.java))
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                delay(1500)
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.bottomNav.selectedItemId = com.agrialert.app.R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.agrialert.app.R.id.nav_reports -> {
                    startActivity(Intent(this, ReportHistoryActivity::class.java))
                    false
                }
                com.agrialert.app.R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsListActivity::class.java))
                    false
                }
                com.agrialert.app.R.id.nav_profile -> {
                    startActivity(Intent(this, FarmerProfileActivity::class.java))
                    false
                }
                else -> true
            }
        }
    }

    private fun setupUI() {
        binding.tvGreeting.text = "${session.greeting},"
        binding.tvName.text = session.firstName
        binding.tvInitials.text = session.initials
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.ReportItem) {
                val intent = Intent(this, FarmerReportDetailActivity::class.java)
                intent.putExtra("report_id", item.report.id)
                startActivity(intent)
            }
        })
        binding.rvRecent.layoutManager = LinearLayoutManager(this)
        binding.rvRecent.adapter = adapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.getFarmerTotal(session.id).collectLatest { count ->
                binding.tvTotalReports.text = count.toString()
            }
        }

        lifecycleScope.launch {
            repository.getFarmerPending(session.id).collectLatest { count ->
                binding.tvPendingReports.text = count.toString()
                binding.tvPendingReports.setTextColor(
                    if (count > 0) android.graphics.Color.parseColor("#EF4444")
                    else android.graphics.Color.parseColor("#2563EB")
                )
            }
        }

        lifecycleScope.launch {
            repository.getLatestAlert().collectLatest { alert ->
                if (alert != null) {
                    binding.alertBanner.visibility = View.VISIBLE
                    binding.tvAlertDisease.text = alert.disease
                    binding.tvAlertMessage.text = if (alert.message.length > 80)
                        "${alert.message.take(80)}..." else alert.message
                    binding.alertBanner.setOnClickListener {
                        val intent = Intent(this@FarmerDashboardActivity, AlertDetailActivity::class.java)
                        intent.putExtra("alert_id", alert.id)
                        startActivity(intent)
                    }
                } else {
                    binding.alertBanner.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            repository.getFarmerRecent(session.id).collectLatest { reports ->
                if (reports.isEmpty()) {
                    binding.rvRecent.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvRecent.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                    adapter.updateReports(reports)
                }
            }
        }

        lifecycleScope.launch {
            repository.getUnreadAlertCount(session.district).collectLatest { count ->
                val badge = binding.bottomNav.getOrCreateBadge(com.agrialert.app.R.id.nav_alerts)
                badge.isVisible = count > 0
                badge.number = count
            }
        }

        lifecycleScope.launch {
            repository.getUnreadNotificationCount(session.id).collectLatest { count ->
                binding.notificationDot.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
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

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = com.agrialert.app.R.id.nav_home
    }
}
