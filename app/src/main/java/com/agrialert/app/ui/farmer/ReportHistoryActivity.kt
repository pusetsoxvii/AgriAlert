package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DiseaseReport
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityReportHistoryBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReportHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportHistoryBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    private var allReports: List<DiseaseReport> = emptyList()
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isFarmer) {
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeReports()
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.ReportItem) {
                val intent = Intent(this, FarmerReportDetailActivity::class.java)
                intent.putExtra("report_id", item.report.id)
                startActivity(intent)
            }
        })
        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { filterAndDisplay() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query ?: ""
                filterAndDisplay()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                filterAndDisplay()
                return true
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            observeReports()
        }

        binding.fabReport.setOnClickListener {
            startActivity(Intent(this, ReportFormActivity::class.java))
        }
    }

    private fun observeReports() {
        lifecycleScope.launch {
            repository.getFarmerAll(session.id).collectLatest { reports ->
                allReports = reports
                updateTabBadges()
                filterAndDisplay()
            }
        }
    }

    private fun updateTabBadges() {
        val pendingCount = allReports.count { it.status == DiseaseReport.PENDING }
        val reviewingCount = allReports.count { it.status in listOf(DiseaseReport.INVESTIGATING, DiseaseReport.ADVICE) }
        val resolvedCount = allReports.count { it.status in listOf(DiseaseReport.VISIT, DiseaseReport.RESOLVED) }

        binding.tabLayout.getTabAt(1)?.text = "Pending ($pendingCount)"
        binding.tabLayout.getTabAt(2)?.text = "Reviewing ($reviewingCount)"
        binding.tabLayout.getTabAt(3)?.text = "Resolved ($resolvedCount)"
    }

    private fun filterAndDisplay() {
        val tabIndex = binding.tabLayout.selectedTabPosition
        
        var filtered = when (tabIndex) {
            1 -> allReports.filter { it.status == DiseaseReport.PENDING }
            2 -> allReports.filter { it.status in listOf(DiseaseReport.INVESTIGATING, DiseaseReport.ADVICE) }
            3 -> allReports.filter { it.status in listOf(DiseaseReport.VISIT, DiseaseReport.RESOLVED) }
            else -> allReports
        }

        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.animalType.contains(currentQuery, ignoreCase = true) ||
                it.symptoms.contains(currentQuery, ignoreCase = true)
            }
        }

        adapter.updateReports(filtered)
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
