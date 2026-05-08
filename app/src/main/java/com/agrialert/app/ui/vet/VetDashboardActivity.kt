package com.agrialert.app.ui.vet

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
import com.agrialert.app.databinding.ActivityVetDashboardBinding
import com.agrialert.app.ui.LoginActivity
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.agrialert.app.ui.farmer.NotificationsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VetDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVetDashboardBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVetDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isVet) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupUI() {
        binding.tvGreeting.text = "${session.greeting},"
        binding.tvVetName.text = "Dr. ${session.name}"
        binding.tvVetInitials.text = session.initials
        binding.tvDistrict.text = session.district
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.ReportItem) {
                val intent = Intent(this, VetReportDetailActivity::class.java)
                intent.putExtra("report_id", item.report.id)
                startActivity(intent)
            }
        })
        binding.rvPending.layoutManager = LinearLayoutManager(this)
        binding.rvPending.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        binding.btnViewAll.setOnClickListener {
            startActivity(Intent(this, VetReportListActivity::class.java))
        }

        binding.btnSeeAllPending.setOnClickListener {
            startActivity(Intent(this, VetReportListActivity::class.java))
        }

        binding.btnSendAlert.setOnClickListener {
            startActivity(Intent(this, SendAlertActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                loadStats()
                delay(1000)
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_reports -> {
                    startActivity(Intent(this, VetReportListActivity::class.java))
                    false
                }
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsHistoryActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, VetProfileActivity::class.java))
                    false
                }
                else -> true
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.getTotalInDistrict(session.district).collectLatest { count ->
                binding.tvTotalReports.text = count.toString()
            }
        }

        lifecycleScope.launch {
            repository.getPendingCountInDistrict(session.district).collectLatest { count ->
                binding.tvPendingCount.text = count.toString()
                binding.tvPendingCount.setTextColor(
                    if (count > 0) android.graphics.Color.parseColor("#EF4444")
                    else android.graphics.Color.parseColor("#2563EB")
                )
                
                val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_reports)
                badge.isVisible = count > 0
                badge.number = count
            }
        }

        lifecycleScope.launch {
            repository.getLatestPendingByDistrict(session.district).collectLatest { reports ->
                if (reports.isEmpty()) {
                    binding.rvPending.visibility = View.GONE
                    binding.tvNoPending.visibility = View.VISIBLE
                } else {
                    binding.rvPending.visibility = View.VISIBLE
                    binding.tvNoPending.visibility = View.GONE
                    adapter.updateReports(reports)
                }
            }
        }

        lifecycleScope.launch {
            repository.getUnreadNotificationCount(session.id).collectLatest { count ->
                binding.notificationDot.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        }

        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) { repository.getResolvedThisWeek(session.district) }
            val alerts = withContext(Dispatchers.IO) { repository.getVetAlertCount(session.id) }
            val visits = withContext(Dispatchers.IO) { repository.getVetVisitCount(session.id) }
            
            binding.tvResolvedWeek.text = resolved.toString()
            binding.tvAlertsMonth.text = alerts.toString()
            binding.tvVisitsCount.text = visits.toString()
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
        binding.bottomNav.selectedItemId = R.id.nav_home
    }
}
