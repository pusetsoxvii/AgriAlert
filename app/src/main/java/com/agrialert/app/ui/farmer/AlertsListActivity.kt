package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityAlertsListBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AlertsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsListBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        setupRecyclerView()
        setupListeners()
        observeAlerts()
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.AlertItem) {
                lifecycleScope.launch {
                    repository.markAlertRead(item.alert.id)
                    val intent = Intent(this@AlertsListActivity, AlertDetailActivity::class.java)
                    intent.putExtra("alert_id", item.alert.id)
                    startActivity(intent)
                }
            }
        })
        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvAlerts.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            observeAlerts()
        }
    }

    private fun observeAlerts() {
        lifecycleScope.launch {
            repository.getAlertsForDistrict(session.district).collectLatest { alerts ->
                if (alerts.isEmpty()) {
                    binding.rvAlerts.visibility = View.GONE
                    binding.tvEmptyAlerts.visibility = View.VISIBLE
                } else {
                    binding.rvAlerts.visibility = View.VISIBLE
                    binding.tvEmptyAlerts.visibility = View.GONE
                    adapter.updateAlerts(alerts)
                }
            }
        }

        lifecycleScope.launch {
            repository.getUnreadAlertCount(session.district).collectLatest { count ->
                binding.toolbar.subtitle = if (count > 0) "$count unread" else "All alerts"
            }
        }
    }
}
