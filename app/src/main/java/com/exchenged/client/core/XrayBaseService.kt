package com.exchenged.client.core

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.exchenged.client.MainActivity
import com.exchenged.client.MainActivity.Companion.ACTION_START_SERVICE
import com.exchenged.client.MainActivity.Companion.ACTION_STOP_SERVICE
import com.exchenged.client.R
import com.exchenged.client.common.repository.SettingsRepository
import com.exchenged.client.helper.NotificationHelper
import com.exchenged.client.viewmodel.XrayViewmodel.Companion.EXTRA_LINK
import com.exchenged.client.viewmodel.XrayViewmodel.Companion.EXTRA_PROTOCOL
import com.exchenged.client.tun2socks.utils.NetPreferences
import com.exchenged.client.tun2socks.Tun2SocksService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@SuppressLint("VpnServicePolicy")
class XrayBaseService
@Inject constructor(
    private val tun2SocksService: Tun2SocksService,
    private val xrayCoreManager: XrayCoreManager,
    private val settingsRepo: SettingsRepository,
    private val notificationHelper: NotificationHelper
): VpnService(){

    companion object {

        const val TAG = "XrayBaseService"

        const val CONNECT = "connect"

        const val DISCONNECT = "disconnect"

        const val RESTART = "restart"

        private val _statusFlow = MutableStateFlow(false)
        val statusFlow = _statusFlow.asStateFlow()


        fun updateStatus(isRunning: Boolean) {
            _statusFlow.tryEmit(isRunning)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    var tunFd: ParcelFileDescriptor? = null


    private fun showForegroundNotification() {
        notificationHelper.showNotification()
        val notification = notificationHelper.makeNotification(Pair(0.0, 0.0))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID, 
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action
        return when(action) {
            DISCONNECT -> {
                Log.i(TAG, "onStartCommand: stop...")
                serviceScope.launch {
                    stopXrayCoreService()
                    updateStatus(false)
                    updateToggleShortcut(false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    notificationHelper.hideNotification()
                }

                START_NOT_STICKY
            }
            CONNECT -> {
                Log.i(TAG, "onStartCommand: start...")
                val link = intent.getStringExtra(EXTRA_LINK)
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL)
                
                if (link == null || protocol == null) {
                    Log.e(TAG, "Missing extras: link=$link, protocol=$protocol")
                    stopSelf()
                    return START_NOT_STICKY
                }

                showForegroundNotification()

                serviceScope.launch {
                    try {
                        xrayCoreManager.addConsumer { data ->
                            notificationHelper.updateNotificationIfNeeded(data)
                        }
                        startXrayCoreService(link, protocol)
                        updateStatus(true)
                        updateToggleShortcut(true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start service", e)
                        updateStatus(false)
                        stopSelf()
                    }
                }
                START_STICKY
            }
            RESTART -> {
                Log.i(TAG, "onStartCommand: restart...")
                val link = intent.getStringExtra(EXTRA_LINK)
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL)
                
                if (link == null || protocol == null) {
                    Log.e(TAG, "Missing extras for restart")
                    updateStatus(false)
                    stopSelf()
                    return START_NOT_STICKY
                }

                showForegroundNotification()

                serviceScope.launch {
                    try {
                        stopXrayCoreService()
                        startXrayCoreService(link, protocol)
                        restartToast()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restart service", e)
                        updateStatus(false)
                        stopSelf()
                    }
                }
                START_STICKY
            }
            else -> { START_NOT_STICKY }
        }
    }


    override fun onDestroy() {
        Log.i(TAG, "onDestroy: close VPN")
        super.onDestroy()
        tunFd?.close()
        tunFd = null
    }



    private suspend fun startVpn() {
        val prefs  = NetPreferences(this)
        val builder = Builder()
        val settings = settingsRepo.settingsFlow.first()
        val dnsServers = settings.dnsIPv4.split(",").filter { it.isNotBlank() }

        if (dnsServers.isNotEmpty()) {
            dnsServers.forEach { builder.addDnsServer(it.trim()) }
        } else {
            builder.addDnsServer("8.8.8.8")
        }
        val allowedPackages = settingsRepo.getAllowedPackages()
        if (!allowedPackages.isEmpty()) {
            allowedPackages.forEach {
                try {
                    builder.addAllowedApplication(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add allowed application: $it", e)
                }
            }
        }else {
            builder.addDisallowedApplication(applicationContext.packageName)
        }
        if (settings.ipV6Enable) {
            builder.addAddress(prefs.tunnelIpv6Address,prefs.tunnelIpv6Prefix)
        }
        tunFd = builder.setSession(resources.getString(R.string.app_label))
            .addAddress(prefs.tunnelIpv4Address, prefs.tunnelIpv4Prefix)
            .addRoute("0.0.0.0",0)
            .setMtu(prefs.tunnelMtu)
            .setBlocking(false)
            .establish() ?: throw Exception("Failed to establish VPN interface")
    }

    private fun stopVPN() {
        tunFd?.close()
        tunFd = null
    }

    @SuppressLint("ShowToast")
    private suspend fun restartToast() {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                applicationContext,
                R.string.service_restart_toast,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private suspend fun startXrayCoreService(link: String, protocol: String) {
        val settingState = settingsRepo.settingsFlow.first()
        startVpn()
        if (settingState.hexTunEnable) {
            xrayCoreManager.startXrayCore(link, protocol, -1)
            // Wait for Xray to bind to the SOCKS port before starting tun2socks
            kotlinx.coroutines.delay(500)
            tunFd?.let {
                serviceScope.launch(Dispatchers.IO) {
                    tun2SocksService.startTun2Socks(it.fd)
                }
            }
        }else {
            xrayCoreManager.startXrayCore(link,protocol,tunFd?.fd)
        }
    }

    private suspend fun stopXrayCoreService() {
        if (tun2SocksService.isRunning()) tun2SocksService.stopTun2Socks()
        stopVPN()
        xrayCoreManager.stopXrayCore()
    }

    // Call this method whenever VPN state changes
    fun updateToggleShortcut(isRunning: Boolean) {
        // Define the intent that the shortcut will fire
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.exchenged.client.ACTION_TOGGLE_PROXY"
            putExtra("shortcut_action",if (isRunning) ACTION_STOP_SERVICE else ACTION_START_SERVICE)
        }

        // Configure shortcut label and icon based on state
        val label = if (isRunning) getString(R.string.stop) else getString(R.string.start)
        val iconRes = if (isRunning) R.drawable.ic_turn_on else R.drawable.ic_turn_off

        val shortcut = ShortcutInfoCompat.Builder(this, "shortcut_toggle")
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(this, iconRes))
            .setIntent(intent)
            .build()

        // Update the shortcut on the launcher
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
    }

}