package com.exchenged.client.update

import android.content.Context
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

object UpdateWorkHelper {
    fun scheduleUpdateCheck(context: Context, intervalMinutes: Long) {
        if (intervalMinutes <= 0) {
            WorkManager.getInstance(context).cancelUniqueWork("regular_update_check")
            return
        }
        
        // WorkManager minimum interval is 15 minutes
        val effectiveInterval = if (intervalMinutes < 15) 15L else intervalMinutes
        
        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(effectiveInterval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "regular_update_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun scheduleSubUpdateCheck(context: Context, intervalMinutes: Long) {
        if (intervalMinutes <= 0) {
            WorkManager.getInstance(context).cancelUniqueWork("sub_update_check")
            return
        }
        
        // WorkManager minimum interval is 15 minutes
        val effectiveInterval = if (intervalMinutes < 15) 15L else intervalMinutes
        
        val workRequest = PeriodicWorkRequestBuilder<SubscriptionUpdateWorker>(effectiveInterval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sub_update_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
