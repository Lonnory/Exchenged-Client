package com.exchenged.client.update

import android.content.Context
import android.util.Log
import com.exchenged.client.BuildConfig
import com.exchenged.client.model.UpdateInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Отдельный объект для хранения информации о текущей версии
 * и логики сравнения/проверки обновлений.
 */
object AppUpdateHelper {
    private const val TAG = "AppUpdateHelper"
    private const val PREFS_NAME = "update_cache"
    
    private const val KEY_LAST_CRITICAL_CHECK = "last_critical_check"
    private const val KEY_REGULAR_REFUSAL_COUNT = "regular_refusal_count"
    private const val KEY_LAST_REGULAR_REFUSAL_TIME = "last_regular_refusal_time"

    // ТЕКУЩАЯ ВЕРСИЯ ПРИЛОЖЕНИЯ (берется из конфига сборки)
    val currentVersionCode: Int = BuildConfig.VERSION_CODE
    val currentVersionName: String = BuildConfig.VERSION_NAME

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * Проверяет наличие обновления.
     */
    suspend fun checkUpdate(isCritical: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        val url = if (isCritical)
            "https://lonnory.ftp.sh/exchengedupdate/criticalupdate.json"
        else
            "https://lonnory.ftp.sh/exchengedupdate/update.json"

        Log.d(TAG, "Checking for ${if (isCritical) "CRITICAL" else "regular"} update at: $url")
        
        try {
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "ExchengedClient/UpdateChecker")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = response.body?.string() ?: return@withContext null
                val serverInfo = gson.fromJson(json, UpdateInfo::class.java)
                
                if (serverInfo.versionCode > currentVersionCode) {
                    return@withContext serverInfo
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check update: ${e.message}")
        }
        return@withContext null
    }

    fun markCriticalCheckPerformed(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CRITICAL_CHECK, System.currentTimeMillis()).apply()
    }

    fun isCriticalCheckDue(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CRITICAL_CHECK, 0)
        
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Target: Today at 12:00
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val targetToday = calendar.timeInMillis
        
        // If we checked after today's 12:00, we're good
        if (lastCheck >= targetToday && now >= targetToday) return false
        
        // If it's before 12:00 today, and we checked after yesterday's 12:00, we're good
        if (now < targetToday) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val targetYesterday = calendar.timeInMillis
            if (lastCheck >= targetYesterday) return false
        }
        
        return true
    }

    fun recordRegularRefusal(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_REGULAR_REFUSAL_COUNT, 0)
        prefs.edit()
            .putInt(KEY_REGULAR_REFUSAL_COUNT, count + 1)
            .putLong(KEY_LAST_REGULAR_REFUSAL_TIME, System.currentTimeMillis())
            .apply()
    }

    fun shouldOverrideRegularInterval(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_REGULAR_REFUSAL_COUNT, 0)
        val lastRefusal = prefs.getLong(KEY_LAST_REGULAR_REFUSAL_TIME, 0)
        
        // If refused only once, check if 5 hours passed
        if (count == 1) {
            val fiveHoursInMs = 5 * 60 * 60 * 1000L
            return System.currentTimeMillis() - lastRefusal >= fiveHoursInMs
        }
        
        // If refused 0 times, it's not an override, just normal check
        // If refused 2+ times, follow user settings (return false for override)
        return false
    }

    fun canShowRegularDialog(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_REGULAR_REFUSAL_COUNT, 0)
        val lastRefusal = prefs.getLong(KEY_LAST_REGULAR_REFUSAL_TIME, 0)

        if (count == 0) return true
        if (count == 1) {
            val fiveHoursInMs = 5 * 60 * 60 * 1000L
            return System.currentTimeMillis() - lastRefusal >= fiveHoursInMs
        }
        // If refused 2+ times, we don't show on every startup, wait for WorkManager
        return false
    }

    fun resetRegularRefusals(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_REGULAR_REFUSAL_COUNT, 0)
            .putLong(KEY_LAST_REGULAR_REFUSAL_TIME, 0)
            .apply()
    }
}
