package com.voicetasker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkPrimaryStrong,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkSuccess,
    onTertiary = DarkSuccessContainer,
    tertiaryContainer = DarkSuccessContainer,
    onTertiaryContainer = DarkSuccess,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkErrorContainer,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightPrimaryStrong,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightSuccess,
    onTertiary = Color.White,
    tertiaryContainer = LightSuccessContainer,
    onTertiaryContainer = LightSuccess,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightError
)

@Immutable
data class VoiceTaskerSemanticColors(
    val primaryStrong: Color,
    val warning: Color,
    val warningContainer: Color,
    val success: Color,
    val successContainer: Color,
    val premiumGold: Color,
    val premiumContainer: Color
)

private val LightSemanticColors = VoiceTaskerSemanticColors(
    primaryStrong = LightPrimaryStrong,
    warning = LightWarning,
    warningContainer = LightWarningContainer,
    success = LightSuccess,
    successContainer = LightSuccessContainer,
    premiumGold = LightPremiumGold,
    premiumContainer = LightPremiumContainer
)

private val DarkSemanticColors = VoiceTaskerSemanticColors(
    primaryStrong = DarkPrimaryStrong,
    warning = DarkWarning,
    warningContainer = DarkWarningContainer,
    success = DarkSuccess,
    successContainer = DarkSuccessContainer,
    premiumGold = DarkPremiumGold,
    premiumContainer = DarkPremiumContainer
)

private val LocalVoiceTaskerSemanticColors = staticCompositionLocalOf { LightSemanticColors }

object VoiceTaskerDesign {
    val colors: VoiceTaskerSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVoiceTaskerSemanticColors.current
}

@Composable
fun VoiceTaskerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
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
    CompositionLocalProvider(LocalVoiceTaskerSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VoiceTaskerTypography,
            shapes = VoiceTaskerShapes,
            content = content
        )
    }
}
