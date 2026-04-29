package com.exchenged.client.ui.component

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.exchenged.client.ui.navigation.Logcat
import com.exchenged.client.ui.navigation.Config
import com.exchenged.client.ui.navigation.Home
import com.exchenged.client.ui.navigation.list_navigation
import com.exchenged.client.viewmodel.XrayViewmodel
import com.exchenged.client.R
import com.exchenged.client.ui.navigation.Apps
import com.exchenged.client.ui.navigation.Detail
import com.exchenged.client.ui.navigation.Edit
import com.exchenged.client.ui.navigation.NavigateDestination
import com.exchenged.client.ui.navigation.Settings
import com.exchenged.client.ui.navigation.Subscription
import com.exchenged.client.ui.scene.ExchengedClientSceneStrategy
import com.exchenged.client.ui.scene.rememberExchengedClientSceneStrategy
import com.exchenged.client.viewmodel.AppsViewmodel
import com.exchenged.client.viewmodel.DetailViewmodel
import com.exchenged.client.viewmodel.SettingsViewmodel
import com.exchenged.client.viewmodel.SubscriptionViewmodel
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.exchenged.client.ui.navigation.ScanQR
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchengedClientContainer(
    xrayViewmodel: XrayViewmodel,
    detailViewmodel: DetailViewmodel,
    settingsViewmodel: SettingsViewmodel,
    subscriptViewmodel: SubscriptionViewmodel,
    appViewmodel: AppsViewmodel,
    modifier: Modifier = Modifier,
    onStartVpn: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val windowSizeClass = androidx.compose.material3.adaptive.currentWindowAdaptiveInfo().windowSizeClass
    val isTablet = windowSizeClass.isWidthAtLeastBreakpoint(androidx.window.core.layout.WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    androidx.activity.compose.BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val navBackStack = rememberNavBackStack(
        Home
    )

    val pendingRoute by xrayViewmodel.pendingRoute.collectAsState()

    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navBackStack.routeTo(route)
            xrayViewmodel.setPaddingRoute(null) // Reset after navigation
        }
    }

    SharedTransitionLayout {
        val sharedTransitionScope = this
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isTablet,
            drawerContent = {
                if (!isTablet) {
                    androidx.compose.material3.ModalDrawerSheet(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        drawerContainerColor = MaterialTheme.colorScheme.background,
                        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    ) {
                        SettingsScreen(
                            viewmodel = settingsViewmodel,
                            sharedTransitionScope = sharedTransitionScope,
                            onNavigate = { destination: NavigateDestination ->
                                scope.launch { drawerState.close() }
                                navBackStack.routeTo(destination)
                            },
                            onClose = {
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavDisplay(
                    backStack = navBackStack,
                    onBack = {navBackStack.routeBack()},
                    sceneStrategies = listOf(rememberExchengedClientSceneStrategy()),
                    sharedTransitionScope = sharedTransitionScope,
                    entryProvider = { key ->
                        when(key) {
                            is Home -> NavEntry(key) {
                                HomeScreen(
                                    xrayViewmodel = xrayViewmodel,
                                    subscriptionViewmodel = subscriptViewmodel,
                                    sharedTransitionScope = sharedTransitionScope,
                                    isSettingsOpen = drawerState.isOpen,
                                    onSettingsClick = { 
                                        if (isTablet) {
                                            navBackStack.routeTo(Settings) 
                                        } else {
                                            scope.launch { drawerState.open() }
                                        }
                                    },
                                    onNavigate = { navBackStack.routeTo(it) },
                                    onStartVpn = onStartVpn
                                )
                            }
                            is Apps -> NavEntry(
                                key = key,
                                metadata = ExchengedClientSceneStrategy.subscreen()
                            ) {
                                AppsScreen(
                                    viewmodel = appViewmodel,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                )
                            }

                            is Settings -> NavEntry(
                                key = key,
                                metadata = ExchengedClientSceneStrategy.settings()
                            ) {
                                SettingsScreen(
                                    viewmodel = settingsViewmodel,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                    onNavigate = { navBackStack.routeTo(it) },
                                    onClose = { navBackStack.routeBack() }
                                )
                            }
                            is com.exchenged.client.ui.navigation.EmojiWorkshop -> NavEntry(key) {
                                EmojiWallpaperScreen(
                                    viewmodel = settingsViewmodel,
                                    onBack = { navBackStack.routeBack() }
                                )
                            }
                            is Logcat -> NavEntry(
                                key = key,
                                metadata = ExchengedClientSceneStrategy.subscreen()
                            ) {
                                LogcatScreen(
                                    viewmodel = xrayViewmodel,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                )
                            }
                            is Edit -> NavEntry(key) { 
                                EditScreen(
                                    detailViewmodel = detailViewmodel,
                                    onBack = { navBackStack.routeBack() },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                ) 
                            }
                            is Detail -> NavEntry(
                                key = key,
                                metadata = ExchengedClientSceneStrategy.detail()
                            ) {
                                EditScreen(
                                    nodeId = key.id,
                                    remark = key.remark,
                                    protocol = key.protocol,
                                    initialContent = key.content,
                                    detailViewmodel = detailViewmodel,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                    onBack = { navBackStack.routeBack() }
                                )
                            }
                            is ScanQR -> NavEntry(
                                key = key,
                                metadata = metadata {
                                    put(NavDisplay.TransitionKey) {
                                        slideInHorizontally {it} togetherWith slideOutHorizontally {-it}
                                    }
                                    // Transition when navigating AWAY from this screen (Popping back)
                                    put(NavDisplay.PopTransitionKey) {
                                        slideInHorizontally {-it} togetherWith slideOutHorizontally {it}
                                    }
                                }
                            ) {
                                QRCodeScannerScreen(
                                    onBack = {navBackStack.routeBack()},
                                    onResult = { code ->
                                        key.onResult(code)
                                        navBackStack.routeBack()
                                    }
                                )
                            }
                            else -> NavEntry(key) { Text(stringResource(R.string.unknown_route)) }
                        }
                    },
                    predictivePopTransitionSpec = { _ ->
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                )
            }
        }
    }
}



@Composable
fun LogcatActionButton(
    xrayViewmodel: XrayViewmodel
) {
    val context = LocalContext.current
    IconButton(
        onClick = {xrayViewmodel.exportLogcatToClipboard(context)}
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.copu),
            contentDescription = "",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ConfigActionButton(
    xrayViewmodel: XrayViewmodel,
    onNavigate: (NavigateDestination) -> Unit
) {
    var expend by remember { mutableStateOf(false) }
    val context = LocalContext.current
    IconButton(
        onClick = {expend = !expend}
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = ""
        )
    }
    DropdownMenu(
        expanded = expend,
        onDismissRequest = {expend = false},
        offset = DpOffset(x = (-8).dp,y = 0.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        DropdownMenuItem(
            text = {Text(stringResource(R.string.menu_subscription))},
            onClick = {
                expend = false
                onNavigate(Subscription)
                //xrayViewmodel.startSubscriptionActivity(context)
            }
        )
        DropdownMenuItem(
            text = {Text(stringResource(R.string.menu_delete_all))},
            onClick = {
                expend = false
                xrayViewmodel.showDeleteDialog(/*delete all*/)
            }
        )
    }
}


private fun NavBackStack<NavKey>.routeTo(key: NavKey) {
    if (lastOrNull() == key) {
        return
    }

    if (key == Home) {
        removeAll(this)
    }else {
        if (contains(key)) {
            remove(key)
        }
    }
    add(key)

}

private fun NavBackStack<NavKey>.routeBack() {
    val nav = lastOrNull()
    if (nav == Home) {
        // exit the application
        return
    }

    removeLastOrNull()
}