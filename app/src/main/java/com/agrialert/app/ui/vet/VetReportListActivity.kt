package com.agrialert.app.ui.vet

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.R
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DiseaseReport
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityVetReportListBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VetReportListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVetReportListBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    private var allReports: List<DiseaseReport> = emptyList()
    private var filteredReports: List<DiseaseReport> = emptyList()
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVetReportListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isVet) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        observeReports()

        binding.swipeRefresh.setOnRefreshListener {
            observeReports()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query ?: ""
                applyFilter()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                applyFilter()
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.ReportItem) {
                val intent = Intent(this, VetReportDetailActivity::class.java)
                intent.putExtra("report_id", item.report.id)
                startActivity(intent)
            }
        })
        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                applyFilter()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeReports() {
        lifecycleScope.launch {
            repository.getReportsByDistrict(session.district).collectLatest { reports ->
                allReports = reports
                updateTabBadges()
                applyFilter()
            }
        }
    }

    private fun updateTabBadges() {
        val pending = allReports.count { it.status == DiseaseReport.PENDING }
        val investigating = allReports.count { it.status == DiseaseReport.INVESTIGATING }
        val resolved = allReports.count { it.status in listOf(DiseaseReport.ADVICE, DiseaseReport.VISIT, DiseaseReport.RESOLVED) }

        binding.tabLayout.getTabAt(1)?.text = "Pending ($pending)"
        binding.tabLayout.getTabAt(2)?.text = "Investigating ($investigating)"
        binding.tabLayout.getTabAt(3)?.text = "Resolved ($resolved)"
    }

    private fun applyFilter() {
        val tabIndex = binding.tabLayout.selectedTabPosition
        var filtered = when (tabIndex) {
            1 -> allReports.filter { it.status == DiseaseReport.PENDING }
            2 -> allReports.filter { it.status == DiseaseReport.INVESTIGATING }
            3 -> allReports.filter { it.status in listOf(DiseaseReport.ADVICE, DiseaseReport.VISIT, DiseaseReport.RESOLVED) }
            else -> allReports
        }

        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.animalType.contains(currentQuery, ignoreCase = true) ||
                it.symptoms.contains(currentQuery, ignoreCase = true) ||
                it.farmerName.contains(currentQuery, ignoreCase = true)
            }
        }

        filteredReports = filtered
        adapter.updateReports(filteredReports)
        binding.tvEmptyState.visibility = if (filteredReports.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_vet_report_sort, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort_newest -> {
                filteredReports = filteredReports.sortedByDescending { it.date }
                adapter.updateReports(filteredReports)
            }
            R.id.sort_oldest -> {
                filteredReports = filteredReports.sortedBy { it.date }
                adapter.updateReports(filteredReports)
            }
            R.id.sort_status -> {
                filteredReports = filteredReports.sortedBy { it.status }
                adapter.updateReports(filteredReports)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
