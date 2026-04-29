package com.exchenged.client.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.exchenged.client.R
import com.exchenged.client.common.model.AppTheme
import com.exchenged.client.common.model.AppWallpaper
import com.exchenged.client.ui.navigation.Apps
import com.exchenged.client.ui.navigation.NavigateDestination
import com.exchenged.client.viewmodel.SettingsViewmodel

@Composable
fun SettingsScreen(
    viewmodel: SettingsViewmodel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onNavigate: (NavigateDestination) -> Unit,
    onClose: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("main") }
    val settingsState by viewmodel.settingsState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    var tapCount by remember { mutableIntStateOf(0) }

    var showEditDialog by remember { mutableStateOf<String?>(null) }
    var dialogValue by remember { mutableStateOf("") }
    var dialogTitle by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        AnimatedContent(
            targetState = currentScreen,
            label = "settings_nav"
        ) { screen ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (screen != "main" && screen != "bug_report") {
                        IconButton(onClick = { currentScreen = "main" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (screen == "bug_report") {
                        IconButton(onClick = { currentScreen = "main" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                        Text(
                            text = when(screen) {
                                "theme" -> stringResource(R.string.select_theme)
                                "wallpaper" -> stringResource(R.string.wallpaper)
                                "customization" -> stringResource(R.string.customization)
                                "network" -> stringResource(R.string.network_part)
                                "subscription_settings" -> stringResource(R.string.subscription_settings)
                                "ping_type" -> stringResource(R.string.ping_type)
                                "bug_report" -> stringResource(R.string.bug_report)
                                "updates" -> stringResource(R.string.updates)
                                "about" -> stringResource(R.string.about_part)
                                else -> stringResource(R.string.settings_title)
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    when (screen) {
                        "main" -> {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.customization)) },
                                leadingContent = { Icon(Icons.Default.Brush, null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                modifier = Modifier.clickable { currentScreen = "customization" }
                            )
                            if (settingsState.isDeveloperMode) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.network_part)) },
                                    leadingContent = { Icon(Icons.Default.NetworkCheck, null) },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                    modifier = Modifier.clickable { currentScreen = "network" }
                                )
                            }
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.subscription_settings)) },
                                leadingContent = { Icon(Icons.Default.Settings, null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                modifier = Modifier.clickable { currentScreen = "subscription_settings" }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.bug_report)) },
                                leadingContent = { Icon(Icons.Default.BugReport, null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                modifier = Modifier.clickable { currentScreen = "bug_report" }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.updates)) },
                                leadingContent = { Icon(Icons.Default.SystemUpdate, null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                modifier = Modifier.clickable { currentScreen = "updates" }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.about_part)) },
                                leadingContent = { Icon(Icons.Default.Info, null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                modifier = Modifier.clickable { currentScreen = "about" }
                            )
                        }
                        "bug_report" -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.bug_report_disclaimer),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+Jqad17P2vn0zMzZi")).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Cannot open Telegram link", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.go_to_telegram))
                                }
                                OutlinedButton(
                                    onClick = { currentScreen = "main" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.back))
                                }
                            }
                        }
                        "customization" -> {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.color_theme)) },
                                supportingContent = { Text(stringResource(settingsState.appTheme.labelRes)) },
                                leadingContent = { Icon(Icons.Default.Palette, null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                modifier = Modifier.clickable { currentScreen = "theme" }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.wallpaper_effects)) },
                                supportingContent = { Text(stringResource(settingsState.appWallpaper.labelRes)) },
                                leadingContent = { Icon(Icons.Default.Wallpaper, null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                modifier = Modifier.clickable { currentScreen = "wallpaper" }
                            )
                            if (settingsState.appWallpaper == AppWallpaper.EMOJI_WORKSHOP) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.configure_emoji_workshop)) },
                                    leadingContent = { Icon(Icons.Default.Face, null) },
                                    modifier = Modifier.clickable { onNavigate(com.exchenged.client.ui.navigation.EmojiWorkshop) }
                                )
                            }
                        }
                        "network" -> {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    ListItem(
                                        headlineContent = { Text(stringResource(R.string.socks_address_listen_title)) },
                                        supportingContent = { Text(settingsState.socksListen) },
                                        leadingContent = { Icon(Icons.Default.Router, null) },
                                        modifier = Modifier.clickable { 
                                            dialogTitle = R.string.socks_address_listen_title
                                            dialogValue = settingsState.socksListen
                                            showEditDialog = "socks_listen"
                                        }
                                    )
                                }
                                item {
                                    ListItem(
                                        headlineContent = { Text(stringResource(R.string.socks_port)) },
                                        supportingContent = { Text(settingsState.socksPort.toString()) },
                                        leadingContent = { Icon(Icons.Default.Numbers, null) },
                                        modifier = Modifier.clickable { 
                                            dialogTitle = R.string.socks_port
                                            dialogValue = settingsState.socksPort.toString()
                                            showEditDialog = "socks_port"
                                        }
                                    )
                                }
                                item {
                                    ListItem(
                                        headlineContent = { Text(stringResource(R.string.socks_username_title)) },
                                        supportingContent = { Text(settingsState.socksUserName.ifEmpty { stringResource(R.string.none) }) },
                                        leadingContent = { Icon(Icons.Default.Person, null) },
                                        modifier = Modifier.clickable { 
                                            dialogTitle = R.string.socks_username_title
                                            dialogValue = settingsState.socksUserName
                                            showEditDialog = "socks_username"
                                        }
                                    )
                                }
                                item {
                                    ListItem(
                                        headlineContent = { Text(stringResource(R.string.socks_password_title)) },
                                        supportingContent = { Text(if (settingsState.socksPassword.isEmpty()) stringResource(R.string.none) else "********") },
                                        leadingContent = { Icon(Icons.Default.Password, null) },
                                        modifier = Modifier.clickable { 
                                            dialogTitle = R.string.socks_password_title
                                            dialogValue = settingsState.socksPassword
                                            showEditDialog = "socks_password"
                                        }
                                    )
                                }
                                item {
                                    ListItem(
                                        headlineContent = { Text(stringResource(R.string.dns_ipv4)) },
                                        supportingContent = { Text(settingsState.dnsIPv4) },
                                        leadingContent = { Icon(Icons.Default.Dns, null) },
                                        modifier = Modifier.clickable { 
                                            dialogTitle = R.string.dns_ipv4
                                            dialogValue = settingsState.dnsIPv4
                                            showEditDialog = "dns_ipv4"
                                        }
                                    )
                                }
                                if (settingsState.ipV6Enable) {
                                    item {
                                        ListItem(
                                            headlineContent = { Text(stringResource(R.string.dns_ipv6)) },
                                            supportingContent = { Text(settingsState.dnsIPv6) },
                                            leadingContent = { Icon(Icons.Default.Dns, null) },
                                            modifier = Modifier.clickable { 
                                                dialogTitle = R.string.dns_ipv6
                                                dialogValue = settingsState.dnsIPv6
                                                showEditDialog = "dns_ipv6"
                                            }
                                        )
                                    }
                                }
                                item {
                                    ListItem(
                                        headlineContent = { Text(stringResource(R.string.test_url)) },
                                        supportingContent = { Text(settingsState.delayTestUrl) },
                                        leadingContent = { Icon(Icons.Default.Link, null) },
                                        modifier = Modifier.clickable { 
                                            dialogTitle = R.string.test_url
                                            dialogValue = settingsState.delayTestUrl
                                            showEditDialog = "test_url"
                                        }
                                    )
                                }
                                item {
                                    SettingsCheckBox(
                                        title = R.string.enable_ipv6,
                                        description = R.string.enable_ipv6_description,
                                        icon = Icons.Default.NetworkCheck,
                                        checked = settingsState.ipV6Enable,
                                        onCheckedChange = { viewmodel.setIpV6Enable(it) }
                                    )
                                }
                                item {
                                    SettingsCheckBox(
                                        title = R.string.enable_hextun_title,
                                        description = R.string.enable_hex_tun_desc,
                                        icon = Icons.Default.Security,
                                        checked = settingsState.hexTunEnable,
                                        onCheckedChange = { viewmodel.setHexTunEnable(it) }
                                    )
                                }
                            }
                        }
                        "subscription_settings" -> {
                            Column {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.allow_app_settings)) },
                                    leadingContent = { Icon(Icons.Default.Apps, null) },
                                    modifier = Modifier.clickable { onNavigate(Apps) }
                                )
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.ping_type)) },
                                    supportingContent = { Text(settingsState.pingType.name) },
                                    leadingContent = { Icon(Icons.Default.Speed, null) },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                    modifier = Modifier.clickable { currentScreen = "ping_type" }
                                )
                            }
                        }
                    "updates" -> {
                        var showIntervalDialog by remember { mutableStateOf(false) }
                        var showFrequencyDialog by remember { mutableStateOf(false) }
                        var showTimeDialog by remember { mutableStateOf(false) }

                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            item {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.update_check_interval)) },
                                    supportingContent = { 
                                        Text(
                                            if (settingsState.updateCheckInterval <= 0) stringResource(R.string.off)
                                            else if (settingsState.updateCheckInterval == 1L) stringResource(R.string.hour_1)
                                            else if (settingsState.updateCheckInterval == 5L) stringResource(R.string.hours_5)
                                            else if (settingsState.updateCheckInterval == 12L) stringResource(R.string.hours_12)
                                            else "${settingsState.updateCheckInterval} " + stringResource(R.string.hours_24).substringAfter(" ")
                                        ) 
                                    },
                                    leadingContent = { Icon(Icons.Default.Update, null) },
                                    modifier = Modifier.clickable { showIntervalDialog = true }
                                )
                            }
                            item {
                                Text(
                                    text = stringResource(R.string.emergency_updates_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.check_frequency)) },
                                    supportingContent = { Text(if (settingsState.emergencyUpdateDay == -1) stringResource(R.string.daily) else stringResource(R.string.weekly_sunday)) },
                                    leadingContent = { Icon(Icons.Default.Event, null) },
                                    modifier = Modifier.clickable { showFrequencyDialog = true }
                                )
                            }
                            item {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.check_time)) },
                                    supportingContent = { Text(settingsState.emergencyUpdateTime) },
                                    leadingContent = { Icon(Icons.Default.Schedule, null) },
                                    modifier = Modifier.clickable { showTimeDialog = true }
                                )
                            }
                        }

                        if (showIntervalDialog) {
                            AlertDialog(
                                onDismissRequest = { showIntervalDialog = false },
                                title = { Text(stringResource(R.string.select_interval)) },
                                text = {
                                    Column {
                                        listOf(0L, 1L, 5L, 12L, 24L).forEach { hours ->
                                            ListItem(
                                                headlineContent = { 
                                                    Text(
                                                        when(hours) {
                                                            0L -> stringResource(R.string.off)
                                                            1L -> stringResource(R.string.hour_1)
                                                            5L -> stringResource(R.string.hours_5)
                                                            12L -> stringResource(R.string.hours_12)
                                                            else -> stringResource(R.string.hours_24)
                                                        }
                                                    ) 
                                                },
                                                modifier = Modifier.clickable {
                                                    viewmodel.setUpdateCheckInterval(context, hours)
                                                    showIntervalDialog = false
                                                }
                                            )
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showIntervalDialog = false }) { Text(stringResource(R.string.cancel)) } }
                            )
                        }
                        
                        if (showFrequencyDialog) {
                            AlertDialog(
                                onDismissRequest = { showFrequencyDialog = false },
                                title = { Text(stringResource(R.string.select_frequency)) },
                                text = {
                                    Column {
                                        ListItem(
                                            headlineContent = { Text(stringResource(R.string.daily)) },
                                            modifier = Modifier.clickable {
                                                viewmodel.setEmergencyUpdateDay(context, -1)
                                                showFrequencyDialog = false
                                            }
                                        )
                                        ListItem(
                                            headlineContent = { Text(stringResource(R.string.weekly_sunday)) },
                                            modifier = Modifier.clickable {
                                                viewmodel.setEmergencyUpdateDay(context, 1)
                                                showFrequencyDialog = false
                                            }
                                        )
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showFrequencyDialog = false }) { Text(stringResource(R.string.cancel)) } }
                            )
                        }

                        if (showTimeDialog) {
                            var timeText by remember { mutableStateOf(settingsState.emergencyUpdateTime) }
                            AlertDialog(
                                onDismissRequest = { showTimeDialog = false },
                                title = { Text(stringResource(R.string.check_time)) },
                                text = {
                                    OutlinedTextField(
                                        value = timeText,
                                        onValueChange = { timeText = it },
                                        label = { Text(stringResource(R.string.check_time)) }
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewmodel.setEmergencyUpdateTime(context, timeText)
                                        showTimeDialog = false
                                    }) { Text(stringResource(R.string.ok)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTimeDialog = false }) { Text(stringResource(R.string.cancel)) }
                                }
                            )
                        }
                    }
                        "about" -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.app_version)) },
                                    supportingContent = { Text(com.exchenged.client.BuildConfig.VERSION_NAME) },
                                    leadingContent = { Icon(Icons.Default.Numbers, null) },
                                    modifier = Modifier.clickable {
                                        if (!settingsState.developerUnlocked) {
                                            tapCount++
                                            if (tapCount >= 3) {
                                                viewmodel.setDeveloperUnlocked(true)
                                                android.widget.Toast.makeText(context, R.string.developer_unlocked_toast, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.xray_core_version)) },
                                    supportingContent = { Text(settingsState.xrayCoreVersion) },
                                    leadingContent = { Icon(Icons.Default.Code, null) }
                                )
                                if (settingsState.developerUnlocked) {
                                    SettingsCheckBox(
                                        title = R.string.developer_mode,
                                        description = 0, // No description
                                        icon = Icons.Default.DeveloperMode,
                                        checked = settingsState.isDeveloperMode,
                                        onCheckedChange = { viewmodel.setDeveloperMode(it) }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = stringResource(R.string.repo_site),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.repo_description)) },
                                    leadingContent = { 
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_github),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        ) 
                                    },
                                    modifier = Modifier.clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lonnory/Exchenged-Client")).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Cannot open browser", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    "ping_type" -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(com.exchenged.client.common.model.PingType.entries) { type ->
                                    Card(
                                        onClick = { viewmodel.setPingType(type) },
                                        border = if (type == settingsState.pingType) androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ListItem(
                                            headlineContent = { Text(type.name) },
                                            trailingContent = { if (type == settingsState.pingType) Icon(Icons.Default.Check, null) }
                                        )
                                    }
                                }
                            }
                        }
                    "theme" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AppTheme.entries) { theme ->
                                Card(
                                    onClick = { viewmodel.setAppTheme(theme) },
                                    border = if (theme == settingsState.appTheme) androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ListItem(
                                        headlineContent = { Text(stringResource(theme.labelRes)) },
                                        trailingContent = { if (theme == settingsState.appTheme) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                    "wallpaper" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AppWallpaper.entries) { wallpaper ->
                                Card(
                                    onClick = { viewmodel.setAppWallpaper(wallpaper) },
                                    border = if (wallpaper == settingsState.appWallpaper) androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ListItem(
                                        headlineContent = { Text(stringResource(wallpaper.labelRes)) },
                                        trailingContent = { if (wallpaper == settingsState.appWallpaper) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(stringResource(dialogTitle)) },
            text = {
                OutlinedTextField(
                    value = dialogValue,
                    onValueChange = { dialogValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (showEditDialog) {
                        "socks_listen" -> viewmodel.setSocksListen(dialogValue)
                        "socks_port" -> dialogValue.toIntOrNull()?.let { viewmodel.setSocksPort(it) }
                        "socks_username" -> viewmodel.setSocksUsername(dialogValue)
                        "socks_password" -> viewmodel.setSocksPassword(dialogValue)
                        "dns_ipv4" -> viewmodel.setDnsIpV4(dialogValue)
                        "dns_ipv6" -> viewmodel.setDnsIpV6(dialogValue)
                        "test_url" -> viewmodel.setDelayTestUrl(dialogValue)
                    }
                    showEditDialog = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun SettingsCheckBox(
    @StringRes title: Int,
    @StringRes description: Int = 0,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = if (description != 0) { { Text(stringResource(description)) } } else null,
        leadingContent = icon?.let { { 
            Icon(
                imageVector = it, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            ) 
        } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}
