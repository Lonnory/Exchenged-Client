package com.exchenged.client.update

import android.content.Context
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

object UpdateWorkHelper {
    fun scheduleUpdateCheck(context: Context, intervalHours: Long) {
        if (intervalHours <= 0) {
            WorkManager.getInstance(context).cancelUniqueWork("regular_update_check")
            return
        }
        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(intervalHours, TimeUnit.HOURS)
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

    fun scheduleEmergencyUpdateCheck(context: Context, time: String, dayOfWeek: Int) {
        val timeParts = time.split(":")
        if (timeParts.size != 2) return
        val hour = timeParts[0].toIntOrNull() ?: 3
        val minute = timeParts[1].toIntOrNull() ?: 0
        
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (dayOfWeek != -1) {
            // Calendar days are 1-indexed (Sunday=1), ensure we match this or adjust.
            // Requirement says 1-7 (Sun-Sat).
            calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek)
        }

        if (calendar.timeInMillis <= now) {
            if (dayOfWeek == -1) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            } else {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val initialDelay = calendar.timeInMillis - now
        val repeatInterval = if (dayOfWeek == -1) 1L else 7L

        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(repeatInterval, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "emergency_update_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
