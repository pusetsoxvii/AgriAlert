package com.agrialert.app.ui.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityAdminStatsBinding
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AdminStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminStatsBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager

    private var fromDate = ""
    private var toDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isAdmin) {
            finish()
            return
        }

        setupListeners()
        loadAllData()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.tvFromDate.setOnClickListener { showDatePicker(true) }
        binding.tvToDate.setOnClickListener { showDatePicker(false) }

        binding.btnApplyFilter.setOnClickListener {
            if (fromDate.isNotEmpty() && toDate.isNotEmpty()) {
                applyDateFilter()
            } else {
                Toast.makeText(this, "Select both dates", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener { shareStats() }
    }

    private fun loadAllData() {
        lifecycleScope.launch {
            val stats = withContext(Dispatchers.IO) { repository.getAdminStats() }
            val chartData = withContext(Dispatchers.IO) { repository.getChartData() }

            updateSummaryCards(stats.totalReports, stats.totalFarmers, stats.pendingReports + stats.investigatingReports, stats.resolvedReports)
            setupPieChart(chartData.byAnimalType.map { PieEntry(it.count.toFloat(), it.animalType) })
            setupBarChart(chartData.daily)
            setupDistrictList(chartData.byDistrict, stats.totalReports)
        }
    }

    private fun updateSummaryCards(reports: Int, farmers: Int, open: Int, resolved: Int) {
        binding.tvTotalReports.text = reports.toString()
        binding.tvTotalFarmers.text = farmers.toString()
        binding.tvOpenCases.text = open.toString()
        binding.tvResolvedCases.text = resolved.toString()
        binding.tvOpenCases.setTextColor(if (open > 0) Color.parseColor("#EF4444") else Color.parseColor("#2563EB"))
    }

    private fun setupPieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) {
            binding.tvNoAnimalData.visibility = View.VISIBLE
            binding.pieChart.visibility = View.GONE
            return
        }
        binding.tvNoAnimalData.visibility = View.GONE
        binding.pieChart.visibility = View.VISIBLE

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }
        
        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            centerText = "By Animal"
            setEntryLabelColor(Color.BLACK)
            animateY(1000)
            invalidate()
        }
    }

    private fun setupBarChart(daily: List<com.agrialert.app.data.DailyCount>) {
        if (daily.isEmpty()) {
            binding.tvNoWeekData.visibility = View.VISIBLE
            binding.barChart.visibility = View.GONE
            return
        }

        binding.tvNoWeekData.visibility = View.GONE
        binding.barChart.visibility = View.VISIBLE

        val entries = daily.mapIndexed { i, it -> BarEntry(i.toFloat(), it.count.toFloat()) }
        val labels = daily.map { it.date.takeLast(5) }

        val dataSet = BarDataSet(entries, "Reports").apply {
            color = Color.parseColor("#2563EB")
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }

        binding.barChart.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            description.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupDistrictList(districts: List<com.agrialert.app.data.DistrictCount>, total: Int) {
        binding.layoutDistricts.removeAllViews()
        val inflater = layoutInflater
        
        districts.forEach { item ->
            val row = inflater.inflate(android.R.layout.simple_list_item_2, binding.layoutDistricts, false)
            val text1 = row.findViewById<TextView>(android.R.id.text1)
            val text2 = row.findViewById<TextView>(android.R.id.text2)
            
            text1.text = item.district
            text1.setTextColor(Color.parseColor("#1E3A5F"))
            text1.setTypeface(null, android.graphics.Typeface.BOLD)
            
            text2.text = "${item.count} reports"
            
            val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
            pb.max = if (total > 0) total else 100
            pb.progress = item.count
            pb.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#BFDBFE"))
            
            binding.layoutDistricts.addView(row)
            binding.layoutDistricts.addView(pb)
        }
    }

    private fun showDatePicker(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val date = "%04d-%02d-%02d".format(y, m + 1, d)
            if (isFrom) {
                fromDate = date
                binding.tvFromDate.text = date
            } else {
                toDate = date
                binding.tvToDate.text = date
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun applyDateFilter() {
        Toast.makeText(this, "Filtering from $fromDate to $toDate", Toast.LENGTH_SHORT).show()
        loadAllData()
    }

    private fun shareStats() {
        lifecycleScope.launch {
            val stats = withContext(Dispatchers.IO) { repository.getAdminStats() }
            val summary = """
                AgriAlert System Statistics
                Generated: ${repository.today()}
                
                Total Reports: ${stats.totalReports}
                Farmers: ${stats.totalFarmers}
                Vet Officers: ${stats.totalVets}
                Open Cases: ${stats.pendingReports + stats.investigatingReports}
                Resolved: ${stats.resolvedReports}
                Alerts Issued: ${stats.totalAlerts}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, summary)
                putExtra(Intent.EXTRA_SUBJECT, "AgriAlert Stats Report")
            }
            startActivity(Intent.createChooser(intent, "Share Statistics"))
        }
    }
}
