package com.agrialert.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.Alert
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityAdminAlertsListBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.agrialert.app.ui.farmer.AlertDetailActivity
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminAlertsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminAlertsListBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    private var allAlerts: List<Alert> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAlertsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isAdmin) {
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeAlerts()
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.AlertItem) {
                val intent = Intent(this, AlertDetailActivity::class.java)
                intent.putExtra("alert_id", item.alert.id)
                startActivity(intent)
            }
        })
        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvAlerts.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { filterAlerts() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.swipeRefresh.setOnRefreshListener {
            observeAlerts()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeAlerts() {
        lifecycleScope.launch {
            repository.getAllAlerts().collectLatest { alerts ->
                allAlerts = alerts
                updateTabBadges()
                filterAlerts()
            }
        }
    }

    private fun updateTabBadges() {
        val low = allAlerts.count { it.severity == "Low" }
        val med = allAlerts.count { it.severity == "Medium" }
        val high = allAlerts.count { it.severity == "High" }

        binding.tabLayout.getTabAt(0)?.text = "All (${allAlerts.size})"
        binding.tabLayout.getTabAt(1)?.text = "Low ($low)"
        binding.tabLayout.getTabAt(2)?.text = "Med ($med)"
        binding.tabLayout.getTabAt(3)?.text = "High ($high)"
    }

    private fun filterAlerts() {
        val tabIndex = binding.tabLayout.selectedTabPosition
        val filtered = when (tabIndex) {
            1 -> allAlerts.filter { it.severity == "Low" }
            2 -> allAlerts.filter { it.severity == "Medium" }
            3 -> allAlerts.filter { it.severity == "High" }
            else -> allAlerts
        }

        adapter.updateAlerts(filtered)
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
