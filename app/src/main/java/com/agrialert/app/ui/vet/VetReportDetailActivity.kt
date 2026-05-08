package com.agrialert.app.ui.vet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.*
import com.agrialert.app.databinding.ActivityVetReportDetailBinding
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.agrialert.app.ui.adapter.statusColor
import com.agrialert.app.ui.adapter.toDisplayDate
import com.agrialert.app.util.NotificationHelper
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VetReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVetReportDetailBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter
    private var reportId: Int = -1
    private var currentReport: DiseaseReport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVetReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reportId = intent.getIntExtra("report_id", -1)
        if (reportId == -1) {
            finish()
            return
        }

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        setupRecyclerView()
        setupForm()
        observeData()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSubmitResponse.setOnClickListener { attemptSubmitResponse() }
        binding.btnReopenCase.setOnClickListener { reopenCase() }
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = {})
        binding.rvResponses.layoutManager = LinearLayoutManager(this)
        binding.rvResponses.adapter = adapter
    }

    private fun setupForm() {
        val actions = VetResponse.ALL_ACTIONS
        binding.spinnerAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions)

        binding.etResponseMessage.addTextChangedListener {
            val count = it?.length ?: 0
            binding.tilResponseMessage.helperText = "$count / 500"
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            val reportWithResponses = repository.getReportWithResponses(reportId)
            if (reportWithResponses == null) {
                Toast.makeText(this@VetReportDetailActivity, "Report not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            currentReport = reportWithResponses.report
            populateReportDetails(reportWithResponses.report)
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

    private fun populateReportDetails(report: DiseaseReport) {
        binding.tvAnimalType.text = report.animalType
        binding.tvFarmerName.text = "Submitted by ${report.farmerName}"
        binding.tvStatus.text = report.status
        binding.tvStatus.setBackgroundColor(report.status.statusColor())
        binding.tvSymptoms.text = report.symptoms
        binding.tvAnimalsCount.text = "${report.animalsAffected} animals"
        binding.tvDate.text = report.date.toDisplayDate()

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
                binding.ivPhoto.setOnClickListener { showFullScreenImage(file) }
            }
        }

        if (report.status == DiseaseReport.RESOLVED) {
            binding.responseForm.visibility = View.GONE
            binding.layoutResolved.visibility = View.VISIBLE
        } else {
            binding.responseForm.visibility = View.VISIBLE
            binding.layoutResolved.visibility = View.GONE
        }
    }

    private fun showFullScreenImage(file: File) {
        val imageView = android.widget.ImageView(this)
        Glide.with(this).load(file).into(imageView)
        AlertDialog.Builder(this)
            .setView(imageView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun attemptSubmitResponse() {
        val message = binding.etResponseMessage.text.toString().trim()
        val action = binding.spinnerAction.selectedItem.toString()
        val report = currentReport ?: return

        if (message.length < 10) {
            binding.tilResponseMessage.error = "Please provide more detail (min 10 chars)"
            return
        } else binding.tilResponseMessage.error = null

        setLoading(true)

        lifecycleScope.launch {
            val response = VetResponse(
                reportId = reportId,
                vetId = session.id,
                vetName = session.name,
                message = message,
                action = action,
                date = repository.today()
            )

            val result = withContext(Dispatchers.IO) {
                repository.submitResponse(response, reportId)
            }

            setLoading(false)

            result.onSuccess {
                NotificationHelper.sendResponseNotification(
                    this@VetReportDetailActivity,
                    session.name,
                    report.animalType,
                    report.farmerId,
                    reportId
                )
                
                // Add in-app notification
                withContext(Dispatchers.IO) {
                    repository.insertNotification(AppNotification(
                        userId = report.farmerId,
                        title = "New Vet Response",
                        message = "Dr. ${session.name} has responded to your report.",
                        type = AppNotification.TYPE_RESPONSE,
                        referenceId = reportId,
                        date = repository.today()
                    ))
                }

                Toast.makeText(this@VetReportDetailActivity, "Response sent successfully", Toast.LENGTH_SHORT).show()
                binding.etResponseMessage.text?.clear()
                observeData() // Refresh status
            }.onFailure {
                Toast.makeText(this@VetReportDetailActivity, "Failed to send response", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun reopenCase() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateReportStatus(reportId, DiseaseReport.INVESTIGATING)
            }
            Toast.makeText(this@VetReportDetailActivity, "Case reopened", Toast.LENGTH_SHORT).show()
            observeData()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSubmitResponse.isEnabled = !loading
        binding.pbSubmitting.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmitResponse.text = if (loading) "" else "Submit Response"
    }
}
