package com.exchenged.client.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

class UpdateManager(private val context: Context) {

    fun isInstallPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            android.widget.Toast.makeText(context, "Пожалуйста, разрешите установку и нажмите 'Обновить' еще раз", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun cleanupOldUpdates(exceptVersion: String? = null) {
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val files = downloadDir.listFiles { _, name -> 
                name.startsWith("ExchengedClient-") && name.endsWith(".apk") 
            }
            files?.forEach { file ->
                if (exceptVersion == null || !file.name.contains(exceptVersion)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Cleanup failed", e)
        }
    }

    fun downloadUpdate(versionName: String) {
        // 1. First check permission
        if (!isInstallPermissionGranted()) {
            requestInstallPermission()
            return
        }

        val fileName = "ExchengedClient-$versionName.apk"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        // 2. If file already exists (recently downloaded), just install it
        if (file.exists()) {
            android.util.Log.d("UpdateManager", "File already exists, installing: $fileName")
            installApk(fileName)
            return
        }

        // 3. Cleanup all other old APKs before starting new download
        cleanupOldUpdates()

        val url = "https://lonnory.ftp.sh/exchengedupdate/latest-universal.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Exchenged Client Update")
            .setDescription("Version $versionName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_download_id", downloadId).putString("last_file_name", fileName).apply()
        
        android.widget.Toast.makeText(context, "Загрузка обновления началась...", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun installApkFromUri(downloadedUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(downloadedUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If direct URI doesn't work, try to find the file
            val fileName = downloadedUri.lastPathSegment ?: return
            installApk(fileName)
        }
    }

    fun installApk(fileName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            }
        }

        context.startActivity(intent)
    }
}
