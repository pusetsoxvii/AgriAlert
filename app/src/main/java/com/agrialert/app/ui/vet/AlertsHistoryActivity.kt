package com.agrialert.app.ui.vet

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityAlertsHistoryBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.agrialert.app.ui.farmer.AlertDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AlertsHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsHistoryBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isVet) {
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
        binding.swipeRefresh.setOnRefreshListener {
            observeAlerts()
            binding.swipeRefresh.isRefreshing = false
        }
        binding.fabSendAlert.setOnClickListener {
            startActivity(Intent(this, SendAlertActivity::class.java))
        }
    }

    private fun observeAlerts() {
        lifecycleScope.launch {
            repository.getVetAlerts(session.id).collectLatest { alerts ->
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
    }
}
