package com.exchenged.client

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.exchenged.client.XrayAppCompatFactory.Companion.TAG
import com.exchenged.client.XrayAppCompatFactory.Companion.xrayPATH
import com.exchenged.client.common.GEO_IP
import com.exchenged.client.common.GEO_SITE
import com.exchenged.client.common.repository.Theme
import com.exchenged.client.common.repository.SettingsKeys
import com.exchenged.client.common.repository.dataStore
import com.exchenged.client.common.utils.SocksConfigGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import com.exchenged.client.update.UpdateWorkHelper
import kotlinx.coroutines.flow.first

class ExchengedClientApplication: Application() {

    private val _isDarkTheme = MutableStateFlow(Theme.AUTO_MODE)
    val isDarkTheme: StateFlow<Int> get() = _isDarkTheme

    private val _appTheme = MutableStateFlow(com.exchenged.client.common.model.AppTheme.DYNAMIC)
    val appTheme: StateFlow<com.exchenged.client.common.model.AppTheme> get() = _appTheme

    private val _appWallpaper = MutableStateFlow(com.exchenged.client.common.model.AppWallpaper.LIQUID_GLASS)
    val appWallpaper: StateFlow<com.exchenged.client.common.model.AppWallpaper> get() = _appWallpaper

    private val _appLanguage = MutableStateFlow(com.exchenged.client.common.model.AppLanguage.RUSSIAN)
    val appLanguage: StateFlow<com.exchenged.client.common.model.AppLanguage> get() = _appLanguage

    var contextAvailableCallback: ContextAvailableCallback? = null

    private val appCoroutineScope = CoroutineScope(Dispatchers.IO)

    private fun observeSettings() {
        appCoroutineScope.launch {
            dataStore.data
                .collect { prefs ->
                    _isDarkTheme.value = prefs[SettingsKeys.DARK_MODE] ?: Theme.AUTO_MODE
                    _appTheme.value = try {
                        com.exchenged.client.common.model.AppTheme.valueOf(prefs[SettingsKeys.APP_THEME] ?: com.exchenged.client.common.model.AppTheme.DYNAMIC.name)
                    } catch (e: Exception) {
                        com.exchenged.client.common.model.AppTheme.DYNAMIC
                    }
                    _appWallpaper.value = try {
                        com.exchenged.client.common.model.AppWallpaper.valueOf(prefs[SettingsKeys.APP_WALLPAPER] ?: com.exchenged.client.common.model.AppWallpaper.LIQUID_GLASS.name)
                    } catch (e: Exception) {
                        com.exchenged.client.common.model.AppWallpaper.LIQUID_GLASS
                    }
                    _appLanguage.value = try {
                        com.exchenged.client.common.model.AppLanguage.valueOf(prefs[SettingsKeys.APP_LANGUAGE] ?: com.exchenged.client.common.model.AppLanguage.RUSSIAN.name)
                    } catch (e: Exception) {
                        com.exchenged.client.common.model.AppLanguage.RUSSIAN
                    }
                    
                    // Apply locale globally to all resources
                    com.exchenged.client.utils.LocaleHelper.applyLocale(this@ExchengedClientApplication, _appLanguage.value.code)
                }
        }
    }

    override fun onCreate() {
        super.onCreate()
        contextAvailableCallback?.onContextAvailable(applicationContext)
        observeSettings()
        initXrayFile()
        initSocksConfig()
        initUpdateSystem()
    }

    private fun initUpdateSystem() {
        appCoroutineScope.launch {
            // Only schedule once at startup based on saved settings
            val prefs = dataStore.data.first()
            val interval = prefs[SettingsKeys.UPDATE_CHECK_INTERVAL] ?: 1440L
            val subInterval = prefs[SettingsKeys.SUB_UPDATE_INTERVAL] ?: 0L
            
            UpdateWorkHelper.scheduleUpdateCheck(this@ExchengedClientApplication, interval)
            UpdateWorkHelper.scheduleSubUpdateCheck(this@ExchengedClientApplication, subInterval)
        }
    }

    private fun initXrayFile() {
        appCoroutineScope.launch {
            //init file
            val fileDir = filesDir
            val geoipFile = File(fileDir, GEO_IP)
            val geositeFile = File(fileDir, GEO_SITE)
            xrayPATH = filesDir.absolutePath
            if (!geoipFile.exists()) {
                Log.i(TAG, "copy geoip.dat")
                assets.open(GEO_IP).use { input ->
                    FileOutputStream(geoipFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (!geositeFile.exists()) {
                assets.open(GEO_SITE).use { input ->
                    FileOutputStream(geositeFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun initSocksConfig() {
        appCoroutineScope.launch {
            dataStore.edit {
                val port = it[SettingsKeys.SOCKS_PORT]
                if (port == null || port !in SocksConfigGenerator.portRange) {
                    it[SettingsKeys.SOCKS_PORT] = SocksConfigGenerator.generatePort()
                }
                val password = it[SettingsKeys.SOCKS_PASSWORD]
                if (password == null || password.isBlank()) {
                    it[SettingsKeys.SOCKS_PASSWORD] = SocksConfigGenerator.generatePassword()
                }
                val username = it[SettingsKeys.SOCKS_USERNAME]
                if (username == null || username.isBlank()) {
                    it[SettingsKeys.SOCKS_USERNAME] = SocksConfigGenerator.generateUsername()
                }
            }
        }
    }
}