package com.agrialert.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.R
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.AppNotification
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityAdminNotificationBinding
import com.agrialert.app.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminNotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminNotificationBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    
    private var selectedTarget = "all"
    private var recipientCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isAdmin) {
            finish()
            return
        }

        setupListeners()
        selectTarget("all")
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.cardAllUsers.setOnClickListener { selectTarget("all") }
        binding.cardFarmersOnly.setOnClickListener { selectTarget(User.ROLE_FARMER) }
        binding.cardVetsOnly.setOnClickListener { selectTarget(User.ROLE_VET) }

        binding.etNotificationTitle.addTextChangedListener { updatePreview() }
        binding.etNotificationMessage.addTextChangedListener { 
            updatePreview()
            binding.tilNotificationMessage.helperText = "${it?.length ?: 0} / 300"
        }

        binding.btnSendNotification.setOnClickListener { validateAndSend() }
    }

    private fun selectTarget(target: String) {
        selectedTarget = target
        
        binding.cardAllUsers.strokeWidth = if (target == "all") 4 else 2
        binding.cardFarmersOnly.strokeWidth = if (target == User.ROLE_FARMER) 4 else 2
        binding.cardVetsOnly.strokeWidth = if (target == User.ROLE_VET) 4 else 2
        
        val activeColor = getColor(R.color.clayBlue)
        val inactiveColor = getColor(R.color.clayBorder)
        
        binding.cardAllUsers.strokeColor = if (target == "all") activeColor else inactiveColor
        binding.cardFarmersOnly.strokeColor = if (target == User.ROLE_FARMER) activeColor else inactiveColor
        binding.cardVetsOnly.strokeColor = if (target == User.ROLE_VET) activeColor else inactiveColor

        loadRecipientCount()
    }

    private fun loadRecipientCount() {
        lifecycleScope.launch {
            recipientCount = withContext(Dispatchers.IO) {
                if (selectedTarget == "all") {
                    repository.countByRole(User.ROLE_FARMER) + 
                    repository.countByRole(User.ROLE_VET) + 
                    repository.countByRole(User.ROLE_ADMIN)
                } else {
                    repository.countByRole(selectedTarget)
                }
            }
            binding.tvRecipientCount.text = "Sending to $recipientCount users"
        }
    }

    private fun updatePreview() {
        val title = binding.etNotificationTitle.text.toString()
        val msg = binding.etNotificationMessage.text.toString()

        binding.previewCard.tvTitle.text = if (title.isEmpty()) "Notification Title" else title
        binding.previewCard.tvMessage.text = if (msg.isEmpty()) "Message content preview..." else msg
        binding.previewCard.tvDate.text = "Just now"
        binding.previewCard.tvType.text = "System"
        binding.previewCard.unreadIndicator.visibility = View.VISIBLE
    }

    private fun validateAndSend() {
        val title = binding.etNotificationTitle.text.toString().trim()
        val message = binding.etNotificationMessage.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilNotificationTitle.error = "Enter a title"
            return
        } else binding.tilNotificationTitle.error = null

        if (message.length < 10) {
            binding.tilNotificationMessage.error = "Message must be at least 10 characters"
            return
        } else binding.tilNotificationMessage.error = null

        AlertDialog.Builder(this)
            .setTitle("Confirm Broadcast")
            .setMessage("Send this notification to $recipientCount users?")
            .setPositiveButton("Send") { _, _ -> sendNotification(title, message) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendNotification(title: String, message: String) {
        setLoading(true)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (selectedTarget == "all") {
                    repository.insertNotificationsForAll(title, message)
                } else {
                    repository.insertNotificationsForRole(selectedTarget, title, message, AppNotification.TYPE_SYSTEM)
                }
            }

            NotificationHelper.sendSystemNotification(this@AdminNotificationActivity, title, message)

            setLoading(false)
            Toast.makeText(this@AdminNotificationActivity, "Notification broadcasted successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSendNotification.isEnabled = !loading
        binding.pbSending.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSendNotification.text = if (loading) "" else "Broadcast Notification"
    }
}
