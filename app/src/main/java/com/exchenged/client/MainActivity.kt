package com.exchenged.client

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.app.Activity
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.exchenged.client.ui.component.ExchengedClientContainer
import com.exchenged.client.ui.component.UpdateDialog
import com.exchenged.client.update.UpdateManager
import com.exchenged.client.update.UpdateWorkHelper
import com.exchenged.client.viewmodel.XrayViewmodel
import com.exchenged.client.ui.XrayBaseActivity
import com.exchenged.client.ui.navigation.ScanQR
import com.exchenged.client.viewmodel.AppsViewmodel
import com.exchenged.client.viewmodel.AppsViewmodelFactory
import com.exchenged.client.viewmodel.DetailViewmodel
import com.exchenged.client.viewmodel.DetailViewmodelFactory
import com.exchenged.client.viewmodel.SettingsViewmodel
import com.exchenged.client.viewmodel.SettingsViewmodelFactory
import com.exchenged.client.viewmodel.SubscriptionViewmodel
import com.exchenged.client.viewmodel.SubscriptionViewmodelFactory
import com.exchenged.client.viewmodel.XrayViewmodelFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity @Inject constructor(
    val xrayViewmodelFactory: XrayViewmodelFactory,
    val detailViewmodelFactory: DetailViewmodelFactory,
    val settingsViewmodelFactory: SettingsViewmodelFactory,
    val subscriptionViewmodelFactory: SubscriptionViewmodelFactory,
    val appViewmodelFactory: AppsViewmodelFactory
) : XrayBaseActivity() {

    private lateinit var xrayViewmodel: XrayViewmodel
    private lateinit var settingsViewmodel: SettingsViewmodel
    
    private lateinit var vpnRequestLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SourceLockedOrientationActivity")
    @Composable
    override fun Content(isLandscape: Boolean) {

        val detailViewmodel =
            ViewModelProvider.create(this,detailViewmodelFactory)[DetailViewmodel::class.java]
        val subscriptionViewmodel = ViewModelProvider
            .create(this, subscriptionViewmodelFactory)[SubscriptionViewmodel::class.java]
        val appViewmodel =
            ViewModelProvider.create(this, appViewmodelFactory)[AppsViewmodel::class.java]

        checkNotificationPermission()
        
        var showUpdateDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var updateVersionName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
        var updateChangelog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
        var isCriticalUpdate by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            checkUpdates(
                onUpdateFound = { info, critical ->
                    updateVersionName = info.versionName
                    updateChangelog = info.changelog
                    isCriticalUpdate = critical
                    showUpdateDialog = true
                }
            )
        }

        androidx.compose.runtime.LaunchedEffect(intent) {
            if (intent.getBooleanExtra("show_update_dialog", false)) {
                updateVersionName = intent.getStringExtra("update_version_name") ?: ""
                updateChangelog = intent.getStringExtra("update_changelog") ?: ""
                isCriticalUpdate = intent.getBooleanExtra("is_critical_update", false)
                showUpdateDialog = true
            }
        }

        if (showUpdateDialog) {
            if (isCriticalUpdate) {
                androidx.activity.compose.BackHandler(enabled = true) {
                    // Do nothing, blocking back button
                }
            }
            UpdateDialog(
                versionName = updateVersionName,
                changelog = updateChangelog,
                isCritical = isCriticalUpdate,
                onUpdate = {
                    val updateManager = UpdateManager(this@MainActivity)
                    updateManager.downloadUpdate(updateVersionName)
                    if (!isCriticalUpdate) showUpdateDialog = false
                },
                onDismiss = {
                    UpdateWorkHelper.scheduleUpdateCheck(this@MainActivity, 5)
                    showUpdateDialog = false
                }
            )
        }

        ExchengedClientContainer(
            xrayViewmodel,
            detailViewmodel,
            settingsViewmodel,
            subscriptionViewmodel,
            appViewmodel,
            onStartVpn = {
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    vpnRequestLauncher.launch(intent)
                } else {
                    xrayViewmodel.startXrayService(this)
                }
            }
        )
    }

    private suspend fun checkUpdates(onUpdateFound: (com.exchenged.client.model.UpdateInfo, Boolean) -> Unit) {
        val client = okhttp3.OkHttpClient()
        val gson = com.google.gson.Gson()

        // 1. Check Critical
        try {
            val criticalRequest = okhttp3.Request.Builder()
                .url("https://lonnory.ftp.sh/exchengedupdate/criticalupdate.json")
                .build()
            
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(criticalRequest).execute()
            }
            if (response.isSuccessful) {
                val json = response.body?.string()
                if (json != null) {
                    val info = gson.fromJson(json, com.exchenged.client.model.UpdateInfo::class.java)
                    if (isUpdateAvailable(info)) {
                        onUpdateFound(info, true)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkUpdates Critical failed", e)
        }

        // 2. Check Regular
        try {
            val regularRequest = okhttp3.Request.Builder()
                .url("https://lonnory.ftp.sh/exchengedupdate/update.json")
                .build()
            
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(regularRequest).execute()
            }
            if (response.isSuccessful) {
                val json = response.body?.string()
                if (json != null) {
                    val info = gson.fromJson(json, com.exchenged.client.model.UpdateInfo::class.java)
                    if (isUpdateAvailable(info)) {
                        onUpdateFound(info, false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkUpdates Regular failed", e)
        }
    }

    private fun isUpdateAvailable(updateInfo: com.exchenged.client.model.UpdateInfo): Boolean {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        return updateInfo.versionCode > currentVersionCode
    }

    companion object {
        const val TAG = "MainActivity"
        const val ACTION_OPEN_SCAN = "open_scan"
        const val ACTION_START_SERVICE = "start_service"
        const val ACTION_STOP_SERVICE = "stop_service"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        xrayViewmodel = ViewModelProvider(this, xrayViewmodelFactory)[XrayViewmodel::class.java]
        settingsViewmodel = ViewModelProvider.create(this, settingsViewmodelFactory)[SettingsViewmodel::class.java]
        
        vpnRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                xrayViewmodel.startXrayService(this)
            }
        }

        super.onCreate(savedInstanceState)

        handleShortcutIntent(intent)
        lifecycleScope.launch {
            settingsViewmodel.settingsState.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    activityManager.appTasks.forEach {
                        val taskInfo = it.taskInfo
                        val currentTaskId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            taskInfo.taskId
                        } else {
                            @Suppress("DEPRECATION")
                            taskInfo.id
                        }
                        if (currentTaskId == taskId) {
                            //set flag: Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            it.setExcludeFromRecents(state.hideFromRecents)
                        }
                    }
                }

        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                //TODO migrate to after click the start button
            } else {
                Toast.makeText(this, "Доступ к уведомлениям отклонен", Toast.LENGTH_SHORT).show()
            }
        }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //todo migrate to after click the start button
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(this)
                        .setTitle("notification permission")
                        .setMessage("need notification permission to keep service alive")
                        .setPositiveButton("grant") { _, _ ->
                            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("reject", null)
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            //todo migrate to before click the start button
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent) {
        // Retrieve the extra defined in shortcuts.xml
        val action = intent.getStringExtra("shortcut_action")
        when(action) {
            ACTION_OPEN_SCAN -> {
                xrayViewmodel.setPaddingRoute(ScanQR { result ->
                    if (result.isEmpty()) {
                        Toast.makeText(this, "Отменено", Toast.LENGTH_LONG).show();
                    }else {
                        xrayViewmodel.addLink(result)
                    }
                })
            }
            ACTION_START_SERVICE -> {
                if (!xrayViewmodel.isServiceRunning()) {
                    xrayViewmodel.startXrayService(this)
                    finish()
                }
            }
            ACTION_STOP_SERVICE -> {
                if (xrayViewmodel.isServiceRunning()) {
                    xrayViewmodel.stopXrayService(this)
                    finish()
                }
            }
        }
    }

}
