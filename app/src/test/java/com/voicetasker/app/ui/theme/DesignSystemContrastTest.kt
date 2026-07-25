package com.voicetasker.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class DesignSystemContrastTest {

    @Test
    fun `light semantic text pairs meet WCAG AA`() {
        assertContrast(LightPrimary, LightOnPrimary, 4.5)
        assertContrast(LightPrimaryContainer, LightOnPrimaryContainer, 4.5)
        assertContrast(LightBackground, LightTextPrimary, 4.5)
        assertContrast(LightBackground, LightTextSecondary, 4.5)
        assertContrast(LightError, Color.White, 4.5)
        assertContrast(LightWarningContainer, LightWarning, 4.5)
        assertContrast(LightSuccessContainer, LightSuccess, 4.5)
        assertContrast(LightPremiumContainer, LightPremiumGold, 4.5)
    }

    @Test
    fun `dark semantic text pairs meet WCAG AA`() {
        assertContrast(DarkPrimary, DarkOnPrimary, 4.5)
        assertContrast(DarkPrimaryContainer, DarkOnPrimaryContainer, 4.5)
        assertContrast(DarkBackground, DarkTextPrimary, 4.5)
        assertContrast(DarkSurface, DarkTextSecondary, 4.5)
        assertContrast(DarkError, DarkErrorContainer, 4.5)
        assertContrast(DarkWarning, DarkWarningContainer, 4.5)
        assertContrast(DarkSuccess, DarkSuccessContainer, 4.5)
        assertContrast(DarkPremiumGold, DarkPremiumContainer, 4.5)
    }

    @Test
    fun `interactive sizing tokens meet the approved minimums`() {
        assertEquals(48.dp, VoiceTaskerSizing.minimumTouchTarget)
        assertEquals(56.dp, VoiceTaskerSizing.primaryFab)
        assertTrue(VoiceTaskerSizing.secondaryFab >= 48.dp)
        assertTrue(VoiceTaskerSizing.inputMinimumHeight >= 52.dp)
    }

    private fun assertContrast(first: Color, second: Color, minimum: Double) {
        val lighter = maxOf(first.relativeLuminance(), second.relativeLuminance())
        val darker = minOf(first.relativeLuminance(), second.relativeLuminance())
        val contrast = (lighter + 0.05) / (darker + 0.05)
        assertTrue(
            "Expected contrast >= $minimum, actual $contrast",
            contrast >= minimum
        )
    }

    private fun Color.relativeLuminance(): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) {
                value / 12.92
            } else {
                ((value + 0.055) / 1.055).pow(2.4)
            }
        }

        return 0.2126 * linearize(red) +
            0.7152 * linearize(green) +
            0.0722 * linearize(blue)
    }
}
