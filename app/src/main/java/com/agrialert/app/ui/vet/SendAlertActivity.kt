package com.agrialert.app.ui.vet

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.R
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.Alert
import com.agrialert.app.data.AppNotification
import com.agrialert.app.data.SessionManager
import com.agrialert.app.databinding.ActivitySendAlertBinding
import com.agrialert.app.ui.adapter.severityColor
import com.agrialert.app.ui.adapter.toDisplayDate
import com.agrialert.app.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendAlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendAlertBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private var selectedSeverity: String = ""
    private var farmerCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        setupSpinners()
        setupSeveritySelection()
        setupListeners()
        updatePreview()
    }

    private fun setupSpinners() {
        val diseases = listOf("Select disease type") + Alert.DISEASES
        binding.spinnerDisease.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, diseases)

        val regions = listOf("Select target region") + Alert.DISTRICTS
        binding.spinnerRegion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regions)

        binding.spinnerRegion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val region = regions[position]
                if (region != "Select target region") {
                    loadFarmerCount(region)
                }
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerDisease.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadFarmerCount(region: String) {
        lifecycleScope.launch {
            farmerCount = withContext(Dispatchers.IO) {
                repository.getFarmerCount(region)
            }
            binding.tvFarmerCount.text = "This will notify $farmerCount farmers in $region"
        }
    }

    private fun setupSeveritySelection() {
        binding.cardLow.setOnClickListener { selectSeverity("Low") }
        binding.cardMedium.setOnClickListener { selectSeverity("Medium") }
        binding.cardHigh.setOnClickListener { selectSeverity("High") }
    }

    private fun selectSeverity(severity: String) {
        selectedSeverity = severity
        
        binding.cardLow.strokeWidth = if (severity == "Low") 4 else 2
        binding.cardMedium.strokeWidth = if (severity == "Medium") 4 else 2
        binding.cardHigh.strokeWidth = if (severity == "High") 4 else 2
        
        binding.cardLow.strokeColor = if (severity == "Low") getColor(R.color.clayBlue) else getColor(R.color.clayBorder)
        binding.cardMedium.strokeColor = if (severity == "Medium") getColor(R.color.clayBlue) else getColor(R.color.clayBorder)
        binding.cardHigh.strokeColor = if (severity == "High") getColor(R.color.clayBlue) else getColor(R.color.clayBorder)
        
        updatePreview()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.etAlertMessage.addTextChangedListener {
            binding.tilAlertMessage.helperText = "${it?.length ?: 0} / 300"
            updatePreview()
        }

        binding.btnSend.setOnClickListener { validateAndConfirm() }
    }

    private fun updatePreview() {
        val disease = binding.spinnerDisease.selectedItem?.toString() ?: ""
        val region = binding.spinnerRegion.selectedItem?.toString() ?: ""
        val message = binding.etAlertMessage.text.toString()

        binding.previewCard.tvDisease.text = if (disease == "Select disease type") "Disease Name" else disease
        binding.previewCard.tvRegion.text = if (region == "Select target region") "Region" else region
        binding.previewCard.tvSeverity.text = if (selectedSeverity.isEmpty()) "Severity" else selectedSeverity
        binding.previewCard.tvSeverity.setBackgroundColor(selectedSeverity.severityColor())
        binding.previewCard.accentBar.setBackgroundColor(selectedSeverity.severityColor())
        binding.previewCard.tvMessage.text = if (message.isEmpty()) "Message preview..." else message
        binding.previewCard.tvDate.text = repository.today().toDisplayDate()
        binding.previewCard.tvVetName.text = "Issued by Dr. ${session.name}"
    }

    private fun validateAndConfirm() {
        val disease = binding.spinnerDisease.selectedItem.toString()
        val region = binding.spinnerRegion.selectedItem.toString()
        val message = binding.etAlertMessage.text.toString().trim()

        if (disease == "Select disease type") {
            Toast.makeText(this, "Please select a disease", Toast.LENGTH_SHORT).show()
            return
        }
        if (region == "Select target region") {
            Toast.makeText(this, "Please select a region", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedSeverity.isEmpty()) {
            Toast.makeText(this, "Please select severity level", Toast.LENGTH_SHORT).show()
            return
        }
        if (message.length < 30) {
            binding.tilAlertMessage.error = "Description must be at least 30 characters"
            return
        } else binding.tilAlertMessage.error = null

        AlertDialog.Builder(this)
            .setTitle("Confirm Alert")
            .setMessage("Send $selectedSeverity alert about $disease to $farmerCount farmers in $region?")
            .setPositiveButton("Send") { _, _ -> sendAlert(disease, region, message) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendAlert(disease: String, region: String, message: String) {
        setLoading(true)
        lifecycleScope.launch {
            val alert = Alert(
                vetId = session.id,
                vetName = "Dr. ${session.name}",
                disease = disease,
                region = region,
                message = message,
                severity = selectedSeverity,
                date = repository.today()
            )

            val result = withContext(Dispatchers.IO) { repository.sendAlert(alert) }
            
            setLoading(false)
            
            result.onSuccess { alertId ->
                NotificationHelper.sendAlertNotification(this@SendAlertActivity, disease, region, selectedSeverity, alertId.toInt())
                
                // Add in-app notifications for all farmers in region
                withContext(Dispatchers.IO) {
                    repository.insertNotificationsForRole(
                        role = "Farmer",
                        title = "DISEASE ALERT: $disease",
                        message = "Official alert issued for $region. Tap for details.",
                        type = AppNotification.TYPE_ALERT
                    )
                }
                
                Toast.makeText(this@SendAlertActivity, "Alert sent successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@SendAlertActivity, "Failed to send alert", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSend.isEnabled = !loading
        binding.pbSending.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSend.text = if (loading) "" else "Send Official Alert"
    }
}
