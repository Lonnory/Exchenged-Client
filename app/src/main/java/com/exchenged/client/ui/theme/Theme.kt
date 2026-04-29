package com.exchenged.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.exchenged.client.common.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFFF0F0F0),
    onSecondary = Color.Black,
    tertiary = Color(0xFFE0E0E0),
    onTertiary = Color.Black,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color.Black,
    outline = Color(0xFFD1D1D1),
    surfaceTint = Color.White
)

private val PurpleBlueColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF1A1A1A),
    onSecondary = Color.White,
    background = Color.Black,
    surface = Color.Black,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val BlackWhiteColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFB0B0B0),
    background = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color(0xFFD1D1D1)
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color.Black,
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color.White,
    background = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color(0xFFD0BCFF),
    secondaryContainer = Color.Black,
    onSecondaryContainer = Color(0xFFEADDFF),
    surfaceTint = Color.Black
)

private val RedBlackColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8B0000),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFF5252),
    onSecondary = Color.Black,
    background = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color.White
)

private val OceanColorScheme = darkColorScheme(
    primary = Color(0xFF80DEEA),
    onPrimary = Color(0xFF00363C),
    secondary = Color(0xFF4DB6AC),
    background = Color(0xFF001F24),
    surface = Color(0xFF001F24),
    onSurface = Color(0xFFE0F3F4),
    surfaceVariant = Color(0xFF3F4E50),
    onSurfaceVariant = Color(0xFFBFC8CA)
)

@Composable
fun ExchengedClientTheme(
    appTheme: AppTheme = AppTheme.DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.DYNAMIC -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        AppTheme.PURPLE_BLUE -> PurpleBlueColorScheme
        AppTheme.BLACK_WHITE -> BlackWhiteColorScheme
        AppTheme.AMOLED -> AmoledColorScheme
        AppTheme.RED_GRAY -> RedBlackColorScheme
        AppTheme.OCEAN -> OceanColorScheme
        AppTheme.LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
