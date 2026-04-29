package com.exchenged.client.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.exchenged.client.MainActivity
import com.exchenged.client.model.UpdateInfo
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val client = OkHttpClient()
        
        // 1. Check Critical Update First
        val criticalRequest = Request.Builder()
            .url("https://lonnory.ftp.sh/exchengedupdate/criticalupdate.json")
            .build()

        try {
            client.newCall(criticalRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (json != null) {
                        val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)
                        if (isUpdateAvailable(updateInfo)) {
                            showNotification(updateInfo, isCritical = true)
                            return Result.success()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log or ignore
        }

        // 2. Check Regular Update
        val regularRequest = Request.Builder()
            .url("https://lonnory.ftp.sh/exchengedupdate/update.json")
            .build()

        try {
            client.newCall(regularRequest).execute().use { response ->
                if (!response.isSuccessful) return Result.retry()

                val json = response.body?.string() ?: return Result.failure()
                val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)

                if (isUpdateAvailable(updateInfo)) {
                    showNotification(updateInfo, isCritical = false)
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun isUpdateAvailable(updateInfo: UpdateInfo): Boolean {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        return updateInfo.versionCode > currentVersionCode
    }

    private fun showNotification(updateInfo: UpdateInfo, isCritical: Boolean) {
        val channelId = "update_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Обновления приложения",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_update_dialog", true)
            putExtra("update_version_name", updateInfo.versionName)
            putExtra("update_changelog", updateInfo.changelog)
            putExtra("update_version_code", updateInfo.versionCode)
            putExtra("is_critical_update", isCritical)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, if (isCritical) 1002 else 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isCritical) "КРИТИЧЕСКОЕ обновление: ${updateInfo.versionName}" else "Доступно обновление: ${updateInfo.versionName}"
        val text = if (isCritical) "Для использования приложения необходимо обновление." else "Нажмите, чтобы узнать подробности и обновить."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(!isCritical)
            .setOngoing(isCritical)
            .build()

        notificationManager.notify(if (isCritical) 1002 else 1001, notification)
    }
}
