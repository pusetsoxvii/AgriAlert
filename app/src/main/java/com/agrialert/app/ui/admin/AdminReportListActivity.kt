package com.agrialert.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.R
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DiseaseReport
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityAdminReportListBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminReportListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminReportListBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    private var farmerId: Int = -1
    private var allReports: List<DiseaseReport> = emptyList()
    private var filteredReports: List<DiseaseReport> = emptyList()
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isAdmin) {
            finish()
            return
        }

        farmerId = intent.getIntExtra("farmer_id", -1)
        val farmerName = intent.getStringExtra("farmer_name")
        if (farmerId != -1) {
            binding.toolbar.subtitle = "Reports for $farmerName"
        }

        setupRecyclerView()
        setupListeners()
        observeReports()
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(
            onItemClick = { item ->
                if (item is ListItem.ReportItem) {
                    val intent = Intent(this, AdminReportDetailActivity::class.java)
                    intent.putExtra("report_id", item.report.id)
                    startActivity(intent)
                }
            }
        )
        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = adapter
    }

    private fun setupListeners() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { applyFilters() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query ?: ""
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                applyFilters()
                return true
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            observeReports()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeReports() {
        val flow = if (farmerId != -1) {
            repository.getReportsByFarmer(farmerId)
        } else {
            repository.getAllReports()
        }

        lifecycleScope.launch {
            flow.collectLatest { reports ->
                allReports = reports
                updateTabBadges()
                applyFilters()
            }
        }
    }

    private fun updateTabBadges() {
        val pending = allReports.count { it.status == DiseaseReport.PENDING }
        val reviewing = allReports.count { it.status in listOf(DiseaseReport.INVESTIGATING, DiseaseReport.ADVICE) }
        val resolved = allReports.count { it.status in listOf(DiseaseReport.VISIT, DiseaseReport.RESOLVED) }

        binding.tabLayout.getTabAt(0)?.text = "All (${allReports.size})"
        binding.tabLayout.getTabAt(1)?.text = "Pending ($pending)"
        binding.tabLayout.getTabAt(2)?.text = "Reviewing ($reviewing)"
        binding.tabLayout.getTabAt(3)?.text = "Resolved ($resolved)"
    }

    private fun applyFilters() {
        val tabIndex = binding.tabLayout.selectedTabPosition
        var filtered = when (tabIndex) {
            1 -> allReports.filter { it.status == DiseaseReport.PENDING }
            2 -> allReports.filter { it.status in listOf(DiseaseReport.INVESTIGATING, DiseaseReport.ADVICE) }
            3 -> allReports.filter { it.status in listOf(DiseaseReport.VISIT, DiseaseReport.RESOLVED) }
            else -> allReports
        }

        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.farmerName.contains(currentQuery, ignoreCase = true) ||
                it.animalType.contains(currentQuery, ignoreCase = true) ||
                it.district.contains(currentQuery, ignoreCase = true) ||
                it.symptoms.contains(currentQuery, ignoreCase = true)
            }
        }

        filteredReports = filtered
        adapter.updateReports(filteredReports)
        binding.tvEmptyState.visibility = if (filteredReports.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_admin_report_sort, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort_newest -> filteredReports = filteredReports.sortedByDescending { it.date }
            R.id.sort_oldest -> filteredReports = filteredReports.sortedBy { it.date }
            R.id.sort_district -> filteredReports = filteredReports.sortedBy { it.district }
            R.id.sort_status -> filteredReports = filteredReports.sortedBy { it.status }
        }
        adapter.updateReports(filteredReports)
        return super.onOptionsItemSelected(item)
    }
}
