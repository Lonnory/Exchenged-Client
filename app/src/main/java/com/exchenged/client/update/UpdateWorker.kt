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
        android.util.Log.d("UpdateWorker", "Starting periodic update check...")
        
        // 1. Check Critical Update (Always check in background if due)
        if (AppUpdateHelper.isCriticalCheckDue(context)) {
            val criticalInfo = AppUpdateHelper.checkUpdate(isCritical = true)
            if (criticalInfo != null) {
                showNotification(criticalInfo, isCritical = true)
                AppUpdateHelper.markCriticalCheckPerformed(context)
                return Result.success()
            }
        }

        // 2. Check Regular Update
        // Background worker execution means either:
        // a) Standard user interval passed
        // b) We are checking specifically because of override (not really possible with WorkManager easily, 
        //    but we can check the state)
        
        // If they haven't refused yet, or 5 hours passed since first refusal, or it's been many refusals 
        // (following user settings means we show it when the scheduled worker runs).
        val regularInfo = AppUpdateHelper.checkUpdate(isCritical = false)
        if (regularInfo != null) {
            showNotification(regularInfo, isCritical = false)
            // If background work found it, we can reset refusal count to let it show in UI on next launch
            // if they dismissed the notification. Or we let MainActivity handle it.
            return Result.success()
        }

        return Result.success()
    }

    private fun showNotification(updateInfo: UpdateInfo, isCritical: Boolean) {
        val channelId = "update_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Обновления приложения",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых версиях приложения"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
            }
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

        val title = if (isCritical) "Критическое обновление безопасности!" else "Доступно новое обновление"
        val text = "Доступна версия ${updateInfo.versionName}. Нажмите, чтобы прочитать список изменений."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + "\n\n" + updateInfo.changelog))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(if (isCritical) 1002 else 1001, notification)
    }
}
