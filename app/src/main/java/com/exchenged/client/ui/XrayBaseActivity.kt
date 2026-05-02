package com.exchenged.client.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.exchenged.client.ExchengedClientApplication
import com.exchenged.client.common.repository.Theme
import com.exchenged.client.ui.theme.ExchengedClientTheme
import com.exchenged.client.utils.LocaleHelper

abstract class XrayBaseActivity: ComponentActivity(){

    @Composable
    abstract fun Content(isLandscape: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ExchengedClientApplication
        enableEdgeToEdge()
        setContent {
            val darkMode = app.isDarkTheme.collectAsState()
            val appTheme = app.appTheme.collectAsState()
            val appLanguage = app.appLanguage.collectAsState()
            
            val context = LocalContext.current
            val localizedContext = remember(appLanguage.value) {
                LocaleHelper.wrapContext(context, appLanguage.value.code)
            }

            val activity = this
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides activity
            ) {
                ExchengedClientTheme(
                    appTheme = appTheme.value,
                    darkTheme = when (darkMode.value) {
                        Theme.LIGHT_MODE -> false
                        Theme.DARK_MODE -> true
                        else -> isSystemInDarkTheme()
                    }
                ) {
                    Content(false)
                }
            }
        }
    }
}

@Deprecated("use scene instead")
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ShowContentWithOrientation(
    content: @Composable (isLandscape: Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    content(isLandscape)
}