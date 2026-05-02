package com.exchenged.client.common.repository

import android.content.Context
import android.util.Log
import androidx.annotation.IntDef
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.exchenged.client.common.model.AppLanguage
import com.exchenged.client.common.model.AppTheme
import com.exchenged.client.common.model.AppWallpaper
import com.exchenged.client.common.model.PingType
import com.exchenged.client.common.utils.SocksConfigGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsState(
    val darkMode: Int = 0,
    val appTheme: AppTheme = AppTheme.DYNAMIC,
    val appWallpaper: AppWallpaper = AppWallpaper.LIQUID_GLASS,
    val appLanguage: AppLanguage = AppLanguage.RUSSIAN,
    val ipV6Enable: Boolean = false,
    val socksPort: Int = 10808,
    val socksUserName: String = "",
    val socksPassword: String = "",
    val socksListen: String = "",
    val dnsIPv4: String = "",
    val dnsIPv6: String = "",
    val delayTestUrl: String = DEFAULT_DELAY_TEST_URL,
    val xrayCoreVersion: String = "unknown",
    val version: String = "1.0.0",
    val geoLiteInstall: Boolean = false,
    val liveUpdateNotification: Boolean = false,
    val bootAutoStart: Boolean = false,
    val hexTunEnable: Boolean = true,
    val hideFromRecents: Boolean = false,
    val emojiWorkshopConfig: String = "",
    val pingType: PingType = PingType.GET,
    val isFirstLaunch: Boolean = true,
    val updateCheckInterval: Long = 1440, // minutes (default 24h)
    val subUpdateInterval: Long = 0, // minutes (0 = off)
    val manualSubName: String = "Сервера",
    val emergencyUpdateTime: String = "03:00", // HH:mm
    val emergencyUpdateDay: Int = -1, // -1 for daily, 1-7 for day of week
    val isDeveloperMode: Boolean = false,
    val developerUnlocked: Boolean = false
)
object SettingsKeys {
    val DARK_MODE = intPreferencesKey("dark_mode")
    val APP_THEME = stringPreferencesKey("app_theme")
    val APP_WALLPAPER = stringPreferencesKey("app_wallpaper")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val IPV6_ENABLE = booleanPreferencesKey("ipv6_enable")
    val SOCKS_PORT = intPreferencesKey("socks_port")
    val SOCKS_USERNAME = stringPreferencesKey("socks_username")
    val SOCKS_PASSWORD = stringPreferencesKey("socks_password")
    val SOCKS_LISTEN = stringPreferencesKey("socks_listen")
    val DNS_IPV4 = stringPreferencesKey("dns_ipv4")
    val DNS_IPV6 = stringPreferencesKey("dns_ipv6")
    val VERSION = stringPreferencesKey("version")
    val DELAY_TEST_URL = stringPreferencesKey("delay_test_site")
    //to json
    val ALLOW_PACKAGES = stringPreferencesKey("allow_packages")
    val XRAY_CORE_VERSION = stringPreferencesKey("xray_version")
    val GEO_LITE_INSTALL = booleanPreferencesKey("geo_lite_install")
    val LIVE_UPDATE_NOTIFICATION = booleanPreferencesKey("live_update_notification")
    val BOOT_AUTO_START = booleanPreferencesKey("boot_auto_start")

    val HEX_TUN_ENABLE = booleanPreferencesKey("hex_tun_open")

    val HIDE_FROM_RECENTS = booleanPreferencesKey("hide_from_recents")
    val EMOJI_WORKSHOP_CONFIG = stringPreferencesKey("emoji_workshop_config")
    val PING_TYPE = stringPreferencesKey("ping_type")
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val UPDATE_CHECK_INTERVAL = longPreferencesKey("update_check_interval")
    val SUB_UPDATE_INTERVAL = longPreferencesKey("sub_update_interval")
    val EMERGENCY_UPDATE_TIME = stringPreferencesKey("emergency_update_time")
    val EMERGENCY_UPDATE_DAY = intPreferencesKey("emergency_update_day")
    val IS_DEVELOPER_MODE = booleanPreferencesKey("is_developer_mode")
    val MANUAL_SUB_NAME = stringPreferencesKey("manual_sub_name")
    val DEVELOPER_UNLOCKED = booleanPreferencesKey("developer_unlocked")
}

const val DEFAULT_DELAY_TEST_URL = "https://www.google.com"

val listType = object : TypeToken<MutableList<String>>() {}.type

@IntDef(value = [
    Theme.LIGHT_MODE,
    Theme.DARK_MODE,
    Theme.AUTO_MODE
])
@Retention(AnnotationRetention.SOURCE)
annotation class Theme {
    companion object {
        const val LIGHT_MODE = 0
        const val DARK_MODE = 1
        const val AUTO_MODE = 2
    }
}


@Singleton
class SettingsRepository
@Inject constructor(private val context: Context) {

    val settingsFlow = context.dataStore.data.map { prefs ->
        SettingsState(
            darkMode = prefs[SettingsKeys.DARK_MODE] ?: 0,
            appTheme = try {
                AppTheme.valueOf(prefs[SettingsKeys.APP_THEME] ?: AppTheme.DYNAMIC.name)
            } catch (e: Exception) {
                AppTheme.DYNAMIC
            },
            appWallpaper = try {
                AppWallpaper.valueOf(prefs[SettingsKeys.APP_WALLPAPER] ?: AppWallpaper.LIQUID_GLASS.name)
            } catch (e: Exception) {
                AppWallpaper.LIQUID_GLASS
            },
            appLanguage = try {
                AppLanguage.valueOf(prefs[SettingsKeys.APP_LANGUAGE] ?: AppLanguage.RUSSIAN.name)
            } catch (e: Exception) {
                AppLanguage.RUSSIAN
            },
            ipV6Enable = prefs[SettingsKeys.IPV6_ENABLE] == true,
            socksPort = prefs[SettingsKeys.SOCKS_PORT] ?: 10808,
            socksUserName = prefs[SettingsKeys.SOCKS_USERNAME]?:"",
            socksPassword = prefs[SettingsKeys.SOCKS_PASSWORD]?:"",
            socksListen = prefs[SettingsKeys.SOCKS_LISTEN]?:"127.0.0.1",
            dnsIPv4 = prefs[SettingsKeys.DNS_IPV4] ?: "8.8.8.8,1.1.1.1",
            dnsIPv6 = prefs[SettingsKeys.DNS_IPV6] ?: "2001:4860:4860::8888",
            delayTestUrl = prefs[SettingsKeys.DELAY_TEST_URL] ?: DEFAULT_DELAY_TEST_URL,
            version = prefs[SettingsKeys.VERSION] ?: "1.0.0",
            xrayCoreVersion = prefs[SettingsKeys.XRAY_CORE_VERSION]?:"unknown",
            geoLiteInstall = prefs[SettingsKeys.GEO_LITE_INSTALL] == true,
            liveUpdateNotification = prefs[SettingsKeys.LIVE_UPDATE_NOTIFICATION] == true,
            bootAutoStart = prefs[SettingsKeys.BOOT_AUTO_START] == true,
            hexTunEnable =  prefs[SettingsKeys.HEX_TUN_ENABLE]?:true,
            hideFromRecents = prefs[SettingsKeys.HIDE_FROM_RECENTS] == true,
            emojiWorkshopConfig = prefs[SettingsKeys.EMOJI_WORKSHOP_CONFIG] ?: "",
            pingType = try {
                PingType.valueOf(prefs[SettingsKeys.PING_TYPE] ?: PingType.GET.name)
            } catch (e: Exception) {
                PingType.GET
            },
            manualSubName = prefs[SettingsKeys.MANUAL_SUB_NAME] ?: "Сервера",
            isFirstLaunch = prefs[SettingsKeys.IS_FIRST_LAUNCH] ?: true,
            updateCheckInterval = prefs[SettingsKeys.UPDATE_CHECK_INTERVAL] ?: 1440,
            subUpdateInterval = prefs[SettingsKeys.SUB_UPDATE_INTERVAL] ?: 0,
            emergencyUpdateTime = prefs[SettingsKeys.EMERGENCY_UPDATE_TIME] ?: "03:00",
            emergencyUpdateDay = prefs[SettingsKeys.EMERGENCY_UPDATE_DAY] ?: -1,
            isDeveloperMode = prefs[SettingsKeys.IS_DEVELOPER_MODE] == true,
            developerUnlocked = prefs[SettingsKeys.DEVELOPER_UNLOCKED] == true
        )

    }

    val packagesFlow = context.dataStore.data.map { prefs ->
        Gson().fromJson<MutableList<String>>(prefs[SettingsKeys.ALLOW_PACKAGES], listType) ?: emptyList()
    }

    suspend fun setDarkMode(@Theme darkMode: Int) {
        context.dataStore.edit {
            it[SettingsKeys.DARK_MODE] = darkMode
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit {
            it[SettingsKeys.APP_THEME] = theme.name
        }
    }

    suspend fun setAppWallpaper(wallpaper: AppWallpaper) {
        context.dataStore.edit {
            it[SettingsKeys.APP_WALLPAPER] = wallpaper.name
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit {
            it[SettingsKeys.APP_LANGUAGE] = language.name
        }
    }

    suspend fun setEmojiWorkshopConfig(config: String) {
        context.dataStore.edit {
            it[SettingsKeys.EMOJI_WORKSHOP_CONFIG] = config
        }
    }

    suspend fun setIpV6Enable(enable: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.IPV6_ENABLE] = enable
        }
    }

    suspend fun setSocksPort(port: Int) {
        context.dataStore.edit {
            it[SettingsKeys.SOCKS_PORT] = port
        }
    }

    suspend fun setDnsIPv4(dns: String) {
        context.dataStore.edit {
            it[SettingsKeys.DNS_IPV4] = dns
        }
    }

    suspend fun setDnsIPv6(dns: String) {
        context.dataStore.edit {
            it[SettingsKeys.DNS_IPV6] = dns
        }
    }
    suspend fun setXrayCoreVersion(version: String) {
        context.dataStore.edit {
            it[SettingsKeys.XRAY_CORE_VERSION] = version
        }
    }

    suspend fun setDelayTestUrl(url:String) {
        context.dataStore.edit {
            it[SettingsKeys.DELAY_TEST_URL] = url
        }
    }

    suspend fun setAllowedPackages(packages: List<String>) {
        val listJson = Gson().toJson(packages, listType)
        context.dataStore.edit {
            it[SettingsKeys.ALLOW_PACKAGES] = listJson
        }
    }

    suspend fun setGeoLiteInstall(installed: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.GEO_LITE_INSTALL] = installed
        }
    }

    suspend fun setLiveUpdateNotification(enable: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.LIVE_UPDATE_NOTIFICATION] = enable
        }
    }

    suspend fun setBootAutoStart(enable: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.BOOT_AUTO_START] = enable
        }
    }

    suspend fun setHexTunState(enable: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.HEX_TUN_ENABLE] = enable
        }
    }

    suspend fun setHideFromRecentsState(enable: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.HIDE_FROM_RECENTS] = enable
        }
    }

    suspend fun setSocksUsername(username: String) {
        context.dataStore.edit {
            it[SettingsKeys.SOCKS_USERNAME] = username
        }
    }

    suspend fun setSocksPassword(password: String) {
        context.dataStore.edit {
            it[SettingsKeys.SOCKS_PASSWORD] = password
        }
    }

    suspend fun setSocksListen(address: String) {
        context.dataStore.edit {
            it[SettingsKeys.SOCKS_LISTEN] = address
        }
    }

    suspend fun setPingType(pingType: PingType) {
        context.dataStore.edit {
            it[SettingsKeys.PING_TYPE] = pingType.name
        }
    }

    suspend fun setManualSubName(name: String) {
        context.dataStore.edit {
            it[SettingsKeys.MANUAL_SUB_NAME] = name
        }
    }

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.IS_FIRST_LAUNCH] = isFirstLaunch
        }
    }

    suspend fun setUpdateCheckInterval(hours: Long) {
        context.dataStore.edit {
            it[SettingsKeys.UPDATE_CHECK_INTERVAL] = hours
        }
    }

    suspend fun setSubUpdateInterval(minutes: Long) {
        context.dataStore.edit {
            it[SettingsKeys.SUB_UPDATE_INTERVAL] = minutes
        }
    }

    suspend fun setEmergencyUpdateTime(time: String) {
        context.dataStore.edit {
            it[SettingsKeys.EMERGENCY_UPDATE_TIME] = time
        }
    }

    suspend fun setEmergencyUpdateDay(day: Int) {
        context.dataStore.edit {
            it[SettingsKeys.EMERGENCY_UPDATE_DAY] = day
        }
    }

    suspend fun setDeveloperMode(enable: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.IS_DEVELOPER_MODE] = enable
        }
    }

    suspend fun setDeveloperUnlocked(unlocked: Boolean) {
        context.dataStore.edit {
            it[SettingsKeys.DEVELOPER_UNLOCKED] = unlocked
        }
    }

    suspend fun addAllowedPackages(packageName: String) {
        context.dataStore.edit { prefs ->
            val listJson = prefs[SettingsKeys.ALLOW_PACKAGES] ?: "[]"
            val list: MutableList<String> = Gson().fromJson(listJson, listType) ?: mutableListOf()

            if (!list.contains(packageName)) {
                list.add(packageName)
            }
            Log.i("test", "addAllowedPackages: ${list.size}")
            prefs[SettingsKeys.ALLOW_PACKAGES] = Gson().toJson(list, listType)
        }
    }

    suspend fun removeAllowedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val listJson = prefs[SettingsKeys.ALLOW_PACKAGES] ?: "[]"

            val list: MutableList<String> = Gson().fromJson(listJson, listType) ?: mutableListOf()

            val newList = list.filter { it != packageName }

            prefs[SettingsKeys.ALLOW_PACKAGES] = Gson().toJson(newList, listType)
        }
    }

    suspend fun getAllowedPackages(): List<String> {
        val prefs = context.dataStore.data.first()
        val json = prefs[SettingsKeys.ALLOW_PACKAGES] ?: "[]"
        return Gson().fromJson(json, listType) ?: emptyList()
    }

}