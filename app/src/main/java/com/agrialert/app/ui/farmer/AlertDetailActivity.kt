package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.databinding.ActivityAlertDetailBinding
import com.agrialert.app.ui.adapter.severityColor
import com.agrialert.app.ui.adapter.toDisplayDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertDetailBinding
    private lateinit var repository: AgriAlertRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val alertId = intent.getIntExtra("alert_id", -1)
        if (alertId == -1) {
            finish()
            return
        }

        repository = AgriAlertRepository.build(this)

        setupListeners()
        loadAlert(alertId)
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadAlert(alertId: Int) {
        lifecycleScope.launch {
            val alert = withContext(Dispatchers.IO) {
                repository.getAlertById(alertId)
            }

            if (alert == null) {
                Toast.makeText(this@AlertDetailActivity, "Alert not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // Mark as read
            withContext(Dispatchers.IO) {
                repository.markAlertRead(alertId)
            }

            binding.tvDisease.text = alert.disease
            binding.tvSeverity.text = alert.severity.uppercase()
            binding.tvSeverity.setBackgroundColor(alert.severity.severityColor())
            binding.tvRegion.text = alert.region
            binding.tvDate.text = alert.date.toDisplayDate()
            binding.tvVetName.text = "Issued by ${alert.vetName}"
            binding.tvMessage.text = alert.message

            binding.btnShare.setOnClickListener {
                val shareText = "${alert.disease}\n${alert.region}\n${alert.severity} severity\n\n${alert.message}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "AgriAlert: ${alert.disease}")
                }
                startActivity(Intent.createChooser(intent, "Share alert"))
            }
        }
    }
}
