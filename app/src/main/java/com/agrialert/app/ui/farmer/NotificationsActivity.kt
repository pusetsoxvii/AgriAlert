package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.AppNotification
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityNotificationsBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isLoggedIn) {
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeNotifications()
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.NotificationItem) {
                handleNotificationClick(item.notification)
            }
        })
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            observeNotifications()
        }
        binding.btnMarkAllRead.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                repository.markAllNotificationsRead(session.id)
            }
        }
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            repository.getNotificationsForUser(session.id).collectLatest { list ->
                if (list.isEmpty()) {
                    binding.rvNotifications.visibility = View.GONE
                    binding.tvEmptyNotifications.visibility = View.VISIBLE
                } else {
                    binding.rvNotifications.visibility = View.VISIBLE
                    binding.tvEmptyNotifications.visibility = View.GONE
                    adapter.updateNotifications(list)
                }
            }
        }

        lifecycleScope.launch {
            repository.getUnreadNotificationCount(session.id).collectLatest { count ->
                binding.toolbar.subtitle = if (count > 0) "$count unread" else "All read"
            }
        }
    }

    private fun handleNotificationClick(n: AppNotification) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.markNotificationRead(n.id)
            }
            
            when (n.type) {
                AppNotification.TYPE_RESPONSE -> {
                    val intent = Intent(this@NotificationsActivity, FarmerReportDetailActivity::class.java)
                    intent.putExtra("report_id", n.referenceId)
                    startActivity(intent)
                }
                AppNotification.TYPE_ALERT -> {
                    val intent = Intent(this@NotificationsActivity, AlertDetailActivity::class.java)
                    intent.putExtra("alert_id", n.referenceId)
                    startActivity(intent)
                }
            }
        }
    }
}
