package com.agrialert.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DiseaseReport
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityAdminReportDetailBinding
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.agrialert.app.ui.adapter.statusColor
import com.agrialert.app.ui.adapter.toDisplayDate
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AdminReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminReportDetailBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter
    private var reportId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reportId = intent.getIntExtra("report_id", -1)
        if (reportId == -1) {
            finish()
            return
        }

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        setupRecyclerView()
        setupOverrideForm()
        observeData()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnUpdateStatus.setOnClickListener { updateStatus() }
        binding.btnDeleteReport.setOnClickListener { confirmDelete() }
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = {})
        binding.rvResponses.layoutManager = LinearLayoutManager(this)
        binding.rvResponses.adapter = adapter
    }

    private fun setupOverrideForm() {
        val statuses = DiseaseReport.ALL_STATUSES
        binding.spinnerStatusOverride.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
    }

    private fun observeData(reload: Boolean = false) {
        lifecycleScope.launch {
            val reportWithResponses = repository.getReportWithResponses(reportId)
            if (reportWithResponses == null) {
                Toast.makeText(this@AdminReportDetailActivity, "Report not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val report = reportWithResponses.report
            populateReportInfo(report)
            
            val statusIndex = DiseaseReport.ALL_STATUSES.indexOf(report.status)
            if (statusIndex != -1) binding.spinnerStatusOverride.setSelection(statusIndex)
        }

        lifecycleScope.launch {
            repository.getResponsesForReport(reportId).collectLatest { responses ->
                if (responses.isEmpty()) {
                    binding.tvNoResponses.visibility = View.VISIBLE
                    binding.rvResponses.visibility = View.GONE
                } else {
                    binding.tvNoResponses.visibility = View.GONE
                    binding.rvResponses.visibility = View.VISIBLE
                    adapter.updateResponses(responses)
                }
            }
        }
    }

    private fun populateReportInfo(report: DiseaseReport) {
        binding.tvAnimalType.text = report.animalType
        binding.tvFarmerName.text = "Submitted by ${report.farmerName}"
        binding.tvStatus.text = report.status
        binding.tvStatus.setBackgroundColor(report.status.statusColor())
        binding.tvSymptoms.text = report.symptoms
        binding.tvAnimalsCount.text = "${report.animalsAffected} animals"
        binding.tvDistrict.text = report.district
        binding.tvDate.text = report.date.toDisplayDate()

        if (report.photoPath.isNotEmpty()) {
            val file = File(report.photoPath)
            if (file.exists()) {
                binding.ivPhoto.visibility = View.VISIBLE
                binding.tvNoPhoto.visibility = View.GONE
                Glide.with(this).load(file).centerCrop().into(binding.ivPhoto)
            } else {
                binding.ivPhoto.visibility = View.GONE
                binding.tvNoPhoto.visibility = View.VISIBLE
            }
        } else {
            binding.ivPhoto.visibility = View.GONE
            binding.tvNoPhoto.visibility = View.VISIBLE
        }
    }

    private fun updateStatus() {
        val selectedStatus = binding.spinnerStatusOverride.selectedItem.toString()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateReportStatus(reportId, selectedStatus)
            }
            Toast.makeText(this@AdminReportDetailActivity, "Status updated to $selectedStatus", Toast.LENGTH_SHORT).show()
            observeData()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Delete this report permanently? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.deleteReport(reportId)
                    }
                    Toast.makeText(this@AdminReportDetailActivity, "Report deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
