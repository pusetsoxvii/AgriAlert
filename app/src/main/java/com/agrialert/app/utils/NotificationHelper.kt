package com.agrialert.app.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.agrialert.app.AgriAlertApp
import com.agrialert.app.R
import com.agrialert.app.ui.farmer.FarmerReportDetailActivity
import com.agrialert.app.ui.vet.VetDashboardActivity
import com.agrialert.app.ui.farmer.AlertDetailActivity

object NotificationHelper {

    // Called after farmer submits disease report
    // Notifies vet officers to check new report
    fun sendReportNotification(
        context: Context,
        farmerName: String,
        animalType: String,
        district: String,
        reportId: Int
    ) {
        val intent = Intent(
            context, VetDashboardActivity::class.java
        ).apply {
            putExtra("report_id", reportId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, reportId, intent,
            PendingIntent.FLAG_IMMUTABLE or
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        buildAndSend(
            context = context,
            notificationId = reportId,
            title = "New disease report",
            text = "$farmerName reported sick " +
                "$animalType in $district",
            pendingIntent = pendingIntent
        )
    }

    // Called after vet submits response
    // Notifies farmer their report has a response
    fun sendResponseNotification(
        context: Context,
        vetName: String,
        animalType: String,
        farmerId: Int,
        reportId: Int
    ) {
        val intent = Intent(
            context,
            FarmerReportDetailActivity::class.java
        ).apply {
            putExtra("report_id", reportId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, reportId + 1000, intent,
            PendingIntent.FLAG_IMMUTABLE or
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        buildAndSend(
            context = context,
            notificationId = reportId + 1000,
            title = "Report response received",
            text = "$vetName responded to your " +
                "$animalType report",
            pendingIntent = pendingIntent
        )
    }

    // Called after vet sends disease alert
    // Notifies all farmers about outbreak
    fun sendAlertNotification(
        context: Context,
        disease: String,
        region: String,
        severity: String,
        alertId: Int
    ) {
        val intent = Intent(
            context, AlertDetailActivity::class.java
        ).apply {
            putExtra("alert_id", alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alertId + 2000, intent,
            PendingIntent.FLAG_IMMUTABLE or
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        buildAndSend(
            context = context,
            notificationId = alertId + 2000,
            title = "Disease alert: $disease",
            text = "$region — $severity severity " +
                "outbreak detected",
            pendingIntent = pendingIntent
        )
    }

    // Called by admin to send system notification
    fun sendSystemNotification(
        context: Context,
        title: String,
        message: String
    ) {
        buildAndSend(
            context = context,
            notificationId = System.currentTimeMillis()
                .toInt(),
            title = title,
            text = message,
            pendingIntent = null
        )
    }

    private fun buildAndSend(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        pendingIntent: PendingIntent?
    ) {
        val builder = NotificationCompat
            .Builder(context, AgriAlertApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text))

        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat
                .from(context)
                .notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
            // Notification silently skipped
        }
    }
}
