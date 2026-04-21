package com.gentlefit.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GentlePink50,
    onPrimary = Color.White,
    primaryContainer = GentlePink80,
    onPrimaryContainer = GentlePink20,
    secondary = SageGreen50,
    onSecondary = Color.White,
    secondaryContainer = SageGreen80,
    onSecondaryContainer = SageGreen20,
    tertiary = WarmCream50,
    onTertiary = WarmCream10,
    tertiaryContainer = WarmCream80,
    onTertiaryContainer = WarmCream20,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ErrorSoft,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = GentlePink40,
    onPrimary = GentlePink10,
    primaryContainer = GentlePink20,
    onPrimaryContainer = GentlePink80,
    secondary = SageGreen40,
    onSecondary = SageGreen10,
    secondaryContainer = SageGreen20,
    onSecondaryContainer = SageGreen80,
    tertiary = WarmCream40,
    onTertiary = WarmCream10,
    tertiaryContainer = WarmCream20,
    onTertiaryContainer = WarmCream80,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = ErrorSoft,
    onError = Color.White
)

@Composable
fun GentleFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GentleFitTypography,
        shapes = GentleFitShapes,
        content = content
    )
}
