package com.agrialert.app.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = AgriAlertRepository.build(applicationContext)
        val database = AgriAlertDatabase.get(applicationContext)
        val reportDao = database.reportDao()

        try {
            val unsyncedReports = reportDao.getUnsyncedReports()
            Log.d("SyncWorker", "Found ${unsyncedReports.size} unsynced reports")

            for (report in unsyncedReports) {
                // SIMULATION: In a real app, this is where the API call happens
                // e.g., apiService.uploadReport(report)
                Log.d("SyncWorker", "Syncing report ID: ${report.id}")
                
                // Simulate network latency
                delay(2000)

                // Mark as synced in local database
                reportDao.markSynced(report.id)
                Log.d("SyncWorker", "Report ${report.id} marked as synced")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error during sync: ${e.message}")
            Result.retry()
        }
    }
}
