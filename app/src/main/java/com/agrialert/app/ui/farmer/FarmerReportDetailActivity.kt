package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.R
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DiseaseReport
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivityFarmerReportDetailBinding
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.agrialert.app.ui.adapter.statusColor
import com.agrialert.app.ui.adapter.toDisplayDate
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FarmerReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFarmerReportDetailBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter
    private var currentReport: DiseaseReport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFarmerReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reportId = intent.getIntExtra("report_id", -1)
        if (reportId == -1) {
            finish()
            return
        }

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isFarmer) {
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeData(reportId)

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = {})
        binding.rvResponses.layoutManager = LinearLayoutManager(this)
        binding.rvResponses.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnEditReport.setOnClickListener {
            currentReport?.let { report ->
                val intent = Intent(this, ReportFormActivity::class.java)
                intent.putExtra("report_id", report.id)
                startActivity(intent)
            }
        }

        binding.btnDeleteReport.setOnClickListener {
            confirmDelete()
        }
    }

    private fun observeData(reportId: Int) {
        lifecycleScope.launch {
            repository.getFarmerAll(session.id).collectLatest { allReports ->
                val report = allReports.find { it.id == reportId }
                if (report == null) {
                    // Report might have been deleted
                    if (currentReport != null) finish() 
                    return@collectLatest
                }

                currentReport = report
                populateReportDetails(report)
                updateTimeline(report.status)
                
                // Allow edit/delete if PENDING or INVESTIGATING (Under Investigation / Asked for Info)
                val canEdit = report.status == DiseaseReport.PENDING || 
                             report.status == DiseaseReport.INVESTIGATING
                binding.layoutActions.visibility = if (canEdit) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            repository.getResponsesForReport(reportId).collectLatest { responses ->
                if (responses.isEmpty()) {
                    binding.tvAwaitingResponse.visibility = View.VISIBLE
                    binding.rvResponses.visibility = View.GONE
                    startPulseAnimation(binding.tvAwaitingResponse)
                } else {
                    binding.tvAwaitingResponse.visibility = View.GONE
                    binding.rvResponses.visibility = View.VISIBLE
                    binding.tvAwaitingResponse.clearAnimation()
                    adapter.updateResponses(responses)
                }
            }
        }
    }

    private fun populateReportDetails(report: DiseaseReport) {
        binding.tvAnimalType.text = report.animalType
        binding.tvDate.text = report.date.toDisplayDate()
        binding.tvSymptoms.text = report.symptoms
        binding.tvAnimalsCount.text = "${report.animalsAffected} animals"
        binding.tvDistrict.text = report.district
        
        binding.tvStatus.text = report.status
        binding.tvStatus.setBackgroundColor(report.status.statusColor())

        if (report.latitude != 0.0) {
            binding.tvCoordinates.text = "%.4f, %.4f".format(report.latitude, report.longitude)
        } else {
            binding.tvCoordinates.text = "Location not captured"
        }

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

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete this report permanently?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    currentReport?.let {
                        withContext(Dispatchers.IO) {
                            repository.deleteReport(it.id)
                        }
                        Toast.makeText(this@FarmerReportDetailActivity, "Report deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTimeline(status: String) {
        val currentStep = when (status) {
            DiseaseReport.PENDING -> 0
            DiseaseReport.INVESTIGATING -> 1
            DiseaseReport.ADVICE -> 2
            DiseaseReport.VISIT -> 3
            DiseaseReport.RESOLVED -> 4
            else -> 0
        }

        val steps = listOf(binding.step0, binding.step1, binding.step2, binding.step3, binding.step4)
        
        steps.forEachIndexed { index, layout ->
            val circle = layout.getChildAt(0)
            val text = layout.getChildAt(1) as android.widget.TextView

            when {
                index < currentStep -> {
                    circle.setBackgroundResource(com.agrialert.app.R.drawable.bg_badge_green)
                }
                index == currentStep -> {
                    circle.setBackgroundResource(com.agrialert.app.R.drawable.bg_circle_blue)
                    text.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                else -> {
                    circle.setBackgroundResource(com.agrialert.app.R.drawable.bg_badge_gray)
                }
            }
        }
    }

    private fun startPulseAnimation(view: View) {
        val anim = AlphaAnimation(0.4f, 1.0f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        view.startAnimation(anim)
    }
}
