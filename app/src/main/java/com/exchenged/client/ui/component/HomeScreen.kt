package com.exchenged.client.ui.component

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchenged.client.R
import com.exchenged.client.common.model.AppWallpaper
import com.exchenged.client.common.repository.SettingsState
import com.exchenged.client.dto.Subscription
import com.exchenged.client.dto.Node
import com.google.gson.Gson
import com.exchenged.client.ui.navigation.NavigateDestination
import com.exchenged.client.ui.navigation.ScanQR
import com.exchenged.client.ui.navigation.Edit
import com.exchenged.client.ui.navigation.Detail
import com.exchenged.client.ui.navigation.Apps
import com.exchenged.client.viewmodel.SubscriptionViewmodel
import com.exchenged.client.viewmodel.XrayViewmodel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val WavyShape = GenericShape { size, _ ->
    val petals = 9
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val rMax = size.minDimension / 2f
    val rMin = rMax * 0.9f
    
    val angleStep = PI * 2 / petals
    
    moveTo(centerX + rMax, centerY)
    
    for (i in 0 until petals) {
        val startAngle = i * angleStep
        val endAngle = (i + 1) * angleStep
        val midAngle = startAngle + angleStep / 2f
        
        val offset = angleStep * 0.28f
        
        val cp1Angle = startAngle + offset
        val cp2Angle = midAngle - offset
        
        cubicTo(
            (centerX + rMax * cos(cp1Angle)).toFloat(),
            (centerY + rMax * sin(cp1Angle)).toFloat(),
            (centerX + rMin * cos(cp2Angle)).toFloat(),
            (centerY + rMin * sin(cp2Angle)).toFloat(),
            (centerX + rMin * cos(midAngle)).toFloat(),
            (centerY + rMin * sin(midAngle)).toFloat()
        )
        
        val cp3Angle = midAngle + offset
        val cp4Angle = endAngle - offset
        
        cubicTo(
            (centerX + rMin * cos(cp3Angle)).toFloat(),
            (centerY + rMin * sin(cp3Angle)).toFloat(),
            (centerX + rMax * cos(cp4Angle)).toFloat(),
            (centerY + rMax * sin(cp4Angle)).toFloat(),
            (centerX + rMax * cos(endAngle)).toFloat(),
            (centerY + rMax * sin(endAngle)).toFloat()
        )
    }
    close()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    xrayViewmodel: XrayViewmodel,
    subscriptionViewmodel: SubscriptionViewmodel,
    bottomPadding: Dp = 0.dp,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isSettingsOpen: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onNavigate: (NavigateDestination) -> Unit,
    onStartVpn: () -> Unit = {}
) {
    val subscriptions by subscriptionViewmodel.subscriptions.collectAsState()
    val allNodes by xrayViewmodel.getAllLinks().collectAsState(emptyList())
    val nodeDelays by xrayViewmodel.nodeDelays.collectAsState()
    val nodesTesting by xrayViewmodel.nodesTesting.collectAsState()
    val isRunning by xrayViewmodel.isServiceRunning.collectAsState()
    val settingsState: SettingsState by xrayViewmodel.settingsState.collectAsState()
    val isUpdating by xrayViewmodel.isUpdating.collectAsState()
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val subscription by subscriptionViewmodel.selectSubscription.collectAsState()
    var isBottomSheetShow by remember { mutableStateOf(false) }

    if (settingsState.isFirstLaunch) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(stringResource(R.string.first_launch_disclaimer)) },
            confirmButton = {
                Button(
                    onClick = { xrayViewmodel.setFirstLaunch(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    val gson = remember { Gson() }
    val emojiWorkshopState = remember(settingsState.emojiWorkshopConfig) {
        if (settingsState.emojiWorkshopConfig.isNotEmpty()) {
            try {
                gson.fromJson(settingsState.emojiWorkshopConfig, WallpaperState::class.java)
            } catch (e: Exception) {
                WallpaperState()
            }
        } else {
            WallpaperState()
        }
    }

    var connectionTimeSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis()
            while (true) {
                connectionTimeSeconds = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        } else {
            connectionTimeSeconds = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper
        when (settingsState.appWallpaper) {
            AppWallpaper.EMOJI_WORKSHOP -> {
                WallpaperEngine(state = emojiWorkshopState)
            }
            AppWallpaper.GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }
            AppWallpaper.LIQUID_GLASS -> {
                LiquidGlassBackground()
            }
            AppWallpaper.NONE -> {}
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                            }
                        },
                        actions = {
                            var menuExpanded by remember { mutableStateOf(false) }
                            
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_config))
                                }
                                
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.import_clipboard)) },
                                        onClick = {
                                            xrayViewmodel.addXrayConfigFromClipboard(context)
                                            menuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCut, contentDescription = null) }
                                    )
                                    var isManualUrlDialogShow by remember { mutableStateOf(false) }
                                    if (isManualUrlDialogShow) {
                                        var urlText by remember { mutableStateOf("") }
                                        var importAsSubscription by remember { mutableStateOf(false) }
                                        
                                        AlertDialog(
                                            onDismissRequest = { isManualUrlDialogShow = false },
                                            title = { Text(stringResource(R.string.add_server_url)) },
                                            text = {
                                                Column {
                                                    TextField(
                                                        value = urlText,
                                                        onValueChange = { urlText = it },
                                                        placeholder = { Text("vless://... or http://...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    if (urlText.startsWith("http")) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(top = 8.dp).clickable { importAsSubscription = !importAsSubscription }
                                                        ) {
                                                            Checkbox(checked = importAsSubscription, onCheckedChange = { importAsSubscription = it })
                                                            Text(stringResource(R.string.add_as_subscription), modifier = Modifier.padding(start = 8.dp))
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    if (urlText.isNotBlank()) {
                                                        if (importAsSubscription && urlText.startsWith("http")) {
                                                            xrayViewmodel.addLink(urlText, context)
                                                        } else if (urlText.startsWith("http")) {
                                                            xrayViewmodel.importSubscriptionAsNodes(urlText, context)
                                                        } else {
                                                            xrayViewmodel.addLink(urlText, context)
                                                        }
                                                    }
                                                    isManualUrlDialogShow = false
                                                }) {
                                                    Text(stringResource(android.R.string.ok))
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { isManualUrlDialogShow = false }) {
                                                    Text(stringResource(android.R.string.cancel))
                                                }
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.import_url)) },
                                        onClick = {
                                            isManualUrlDialogShow = true
                                            menuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.scan_qr)) },
                                        onClick = {
                                            onNavigate(ScanQR { result ->
                                                if (result.isEmpty()) {
                                                    Toast.makeText(context, context.getString(R.string.decode_qr_failed), Toast.LENGTH_LONG).show()
                                                } else {
                                                    xrayViewmodel.addLink(result, context)
                                                }
                                            })
                                            menuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.add_sub_url)) },
                                        onClick = {
                                            subscriptionViewmodel.setSelectSubscriptionEmpty()
                                            isBottomSheetShow = true
                                            menuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Subscriptions, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.manual_edit)) },
                                        onClick = {
                                            onNavigate(Edit)
                                            menuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_delete_all)) },
                                        onClick = {
                                            xrayViewmodel.showDeleteDialog()
                                            menuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        )
                    )
                    if (isUpdating) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = bottomPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        VpnToggleButton(isRunning = isRunning) {
                            if (!isRunning) {
                                onStartVpn()
                            } else {
                                xrayViewmodel.stopXrayService(context)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRunning) stringResource(R.string.vpn_connected) else stringResource(R.string.vpn_disconnected),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (isRunning) {
                            val hours = connectionTimeSeconds / 3600
                            val minutes = (connectionTimeSeconds % 3600) / 60
                            val seconds = connectionTimeSeconds % 60
                            Text(
                                text = String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    val manualNodes = allNodes.filter { it.subscriptionId == XrayViewmodel.SUB_MANUAL }
                    if (manualNodes.isNotEmpty()) {
                        item {
                            SubscriptionCard(
                                subscription = Subscription(id = XrayViewmodel.SUB_MANUAL, mark = "Manual Configs", url = "Manually added nodes"),
                                nodes = manualNodes,
                                nodeDelays = nodeDelays,
                                nodesTesting = nodesTesting,
                                onNodeSelect = { id -> xrayViewmodel.setSelectedNode(id) },
                                onNodePing = { node -> xrayViewmodel.pingNode(node) },
                                isGlassy = settingsState.appWallpaper == AppWallpaper.LIQUID_GLASS,
                                isUpdating = false,
                                onUpdate = { },
                                onPing = { xrayViewmodel.pingNodes(manualNodes) },
                                onEdit = { },
                                onDelete = { xrayViewmodel.showDeleteDialog(XrayViewmodel.SUB_MANUAL) },
                                onNodeEdit = { node ->
                                    onNavigate(Detail(
                                        id = node.id,
                                        remark = node.remark,
                                        protocol = node.protocolPrefix,
                                        content = node.url
                                    ))
                                },
                                onNodeDelete = { node ->
                                    xrayViewmodel.showDeleteDialog(node.id)
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    items(subscriptions, key = { it.id }) { sub ->
                        val subNodes = allNodes.filter { it.subscriptionId == sub.id }
                        SubscriptionCard(
                            subscription = sub,
                            nodes = subNodes,
                            nodeDelays = nodeDelays,
                            nodesTesting = nodesTesting,
                            onNodeSelect = { id -> xrayViewmodel.setSelectedNode(id) },
                            onNodePing = { node -> xrayViewmodel.pingNode(node) },
                            isGlassy = settingsState.appWallpaper == AppWallpaper.LIQUID_GLASS,
                            isUpdating = isUpdating,
                            onUpdate = { xrayViewmodel.updateSubscription(sub.id, sub.url, context) },
                            onPing = { xrayViewmodel.pingNodes(subNodes) },
                            onEdit = { 
                                if (!sub.isLocked) {
                                    subscriptionViewmodel.getSubscriptionByIdWithCallback(sub.id) {
                                        isBottomSheetShow = true
                                    }
                                }
                            },
                            onDelete = {
                                if (!sub.isLocked) {
                                    subscriptionViewmodel.showDeleteDialog(sub)
                                }
                            },
                            onNodeEdit = { node ->
                                onNavigate(Detail(
                                    id = node.id,
                                    remark = node.remark,
                                    protocol = node.protocolPrefix,
                                    content = node.url
                                ))
                            },
                            onNodeDelete = { node ->
                                xrayViewmodel.showDeleteDialog(node.id)
                            }
                        )
                    }
                }

                ExceptionMessage(
                    shown = showError,
                    msg = stringResource(R.string.config_not_ready),
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (isBottomSheetShow) {
                    AddSubscriptionDialog(
                        subscription = subscription,
                        onDismiss = { isBottomSheetShow = false },
                        onConfirm = { name, url ->
                            subscriptionViewmodel.addOrUpdateSubscription(
                                Subscription(
                                    id = subscription.id,
                                    mark = name,
                                    url = url,
                                    isAutoUpdate = subscription.isAutoUpdate,
                                    updateInterval = subscription.updateInterval,
                                    userInfo = subscription.userInfo,
                                    supportUrl = subscription.supportUrl,
                                    webPageUrl = subscription.webPageUrl,
                                    isLocked = subscription.isLocked,
                                    announce = subscription.announce
                                )
                            )
                            isBottomSheetShow = false
                        }
                    )
                }
            }
        }
    }

    val deleteDialog by subscriptionViewmodel.deleteDialog.collectAsState()
    if (deleteDialog) {
        DeleteDialog(
            onDismissRequest = { subscriptionViewmodel.dismissDeleteDialog() },
        ) {
            subscriptionViewmodel.deleteSubscriptionWithDialog()
        }
    }
    
    val xrayDeleteDialog by xrayViewmodel.deleteDialog.collectAsState()
    if (xrayDeleteDialog) {
        DeleteDialog(
            onDismissRequest = { xrayViewmodel.hideDeleteDialog() }
        ) {
            xrayViewmodel.deleteNodeFromDialog()
        }
    }
}

@Composable
fun LiquidGlassBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid")
    
    val animX1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "x1"
    )
    val animY1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse), label = "y1"
    )
    
    val animX2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "x2"
    )
    val animY2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "y2"
    )
    
    val colorScheme = MaterialTheme.colorScheme
    
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            val width = size.width
            val height = size.height
            
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorScheme.primary.copy(alpha = 0.5f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width * animX1, height * animY1),
                    radius = width * 0.9f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorScheme.secondary.copy(alpha = 0.4f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width * animX2, height * animY2),
                    radius = width * 0.8f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorScheme.tertiary.copy(alpha = 0.3f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width * 0.5f, height * (1 - animY1)),
                    radius = width * 0.7f
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background.copy(alpha = 0.15f))
        )
    }
}

@Composable
fun AddSubscriptionDialog(
    subscription: Subscription,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(subscription.mark) }
    var url by remember { mutableStateOf(subscription.url) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (subscription.id <= 0) stringResource(R.string.add_subscription) else stringResource(R.string.edit_subscription)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, url) }, enabled = name.isNotBlank() && url.isNotBlank()) {
                Text(stringResource(R.string.confirm_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn))
            }
        }
    )
}

@Composable
fun VpnToggleButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    val colorScheme = MaterialTheme.colorScheme
    val outerColor by animateColorAsState(
        if (isRunning) colorScheme.primary.copy(alpha = 0.5f) else colorScheme.primary.copy(alpha = 0.3f),
        label = "outerColor"
    )
    val innerColor by animateColorAsState(
        if (isRunning) colorScheme.primary else colorScheme.surfaceVariant,
        label = "innerColor"
    )
    val iconColor by animateColorAsState(
        if (isRunning) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
        label = "iconColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(outerColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Внутренняя фигура "цветок/звезда"
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize(0.72f)
                .clip(WavyShape)
                .background(innerColor)
        ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Запуск/Остановка VPN",
                    modifier = Modifier.size(72.dp),
                    tint = iconColor
                )
        }
    }
}

@Composable
fun SubscriptionCard(
    subscription: Subscription,
    nodes: List<Node> = emptyList(),
    nodeDelays: Map<Int, Long> = emptyMap(),
    nodesTesting: Set<Int> = emptySet(),
    isGlassy: Boolean,
    isUpdating: Boolean = false,
    onNodeSelect: (Int) -> Unit = {},
    onNodePing: (Node) -> Unit = {},
    onUpdate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNodeEdit: (Node) -> Unit = {},
    onNodeDelete: (Node) -> Unit = {},
    onPing: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isUpdating) 360f else 0f,
        animationSpec = if (isUpdating) {
            infiniteRepeatable(tween(1000, easing = LinearEasing))
        } else {
            tween(0)
        },
        label = "refreshRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlassy) colorScheme.surface.copy(alpha = 0.4f) else colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = subscription.mark,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUpdate,
                        modifier = Modifier.size(32.dp),
                        enabled = !isUpdating
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Update",
                            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation },
                            tint = colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.primary
                        )
                    }
                    if (!subscription.isLocked) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Menu",
                                    modifier = Modifier.size(20.dp),
                                    tint = colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Support buttons right under the title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!subscription.supportUrl.isNullOrBlank()) {
                    IconButton(
                        onClick = { uriHandler.openUri(subscription.supportUrl!!) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "Support",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!subscription.webPageUrl.isNullOrBlank()) {
                    IconButton(
                        onClick = { uriHandler.openUri(subscription.webPageUrl!!) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Web Page",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Announce under support buttons
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = colorScheme.primaryContainer.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = subscription.announce ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Ping button in the bottom right under announce
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onPing,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Ping",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (nodes.isNotEmpty()) {
                        Text(
                            text = "Servers:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        nodes.forEach { node ->
                            NodeCard(
                                node = node,
                                onChoose = { onNodeSelect(node.id) },
                                onTest = { onNodePing(node) },
                                onEdit = { onNodeEdit(node) },
                                delete = { onNodeDelete(node) },
                                delayMs = nodeDelays[node.id] ?: -1L,
                                testing = nodesTesting.contains(node.id),
                                selected = node.selected,
                                roundCorner = true,
                                backgroundColor = colorScheme.surface.copy(alpha = 0.6f),
                                contentColor = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    
                    Text(
                        text = subscription.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
