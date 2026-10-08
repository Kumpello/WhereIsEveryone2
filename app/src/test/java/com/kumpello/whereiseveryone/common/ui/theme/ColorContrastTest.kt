package com.kumpello.whereiseveryone.common.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun lightTheme_textColorsRemainReadable() = assertReadableText(LightColorScheme)

    @Test
    fun darkTheme_textColorsRemainReadable() = assertReadableText(DarkColorScheme)

    @Test
    fun controlOutlinesRemainVisibleInBothThemes() {
        listOf(LightColorScheme, DarkColorScheme).forEach { colors ->
            assertContrast("Control outline", colors.outline, colors.surface, minimum = 3f)
        }
    }

    private fun assertReadableText(colors: ColorScheme) {
        val textPairs = listOf(
            Triple("Primary action", colors.onPrimary, colors.primary),
            Triple("Secondary action", colors.onSecondary, colors.secondary),
            Triple("Tertiary action", colors.onTertiary, colors.tertiary),
            Triple("Error action", colors.onError, colors.error),
            Triple("Primary container", colors.onPrimaryContainer, colors.primaryContainer),
            Triple("Secondary container", colors.onSecondaryContainer, colors.secondaryContainer),
            Triple("Tertiary container", colors.onTertiaryContainer, colors.tertiaryContainer),
            Triple("Error container", colors.onErrorContainer, colors.errorContainer),
            Triple("Fixed primary", colors.onPrimaryFixed, colors.primaryFixedDim),
            Triple("Fixed primary supporting text", colors.onPrimaryFixedVariant, colors.primaryFixedDim),
            Triple("Fixed secondary", colors.onSecondaryFixed, colors.secondaryFixedDim),
            Triple("Fixed secondary supporting text", colors.onSecondaryFixedVariant, colors.secondaryFixedDim),
            Triple("Fixed tertiary", colors.onTertiaryFixed, colors.tertiaryFixedDim),
            Triple("Fixed tertiary supporting text", colors.onTertiaryFixedVariant, colors.tertiaryFixedDim),
            Triple("Background", colors.onBackground, colors.background),
            Triple("Surface", colors.onSurface, colors.surface),
            Triple("Dialog text", colors.onSurface, colors.surfaceContainerHigh),
            Triple("Supporting text", colors.onSurfaceVariant, colors.surfaceContainerHigh),
            Triple("Accent text", colors.primary, colors.surfaceContainerHigh),
            Triple("Error text", colors.error, colors.surfaceContainerHigh),
            Triple("Tooltip text", colors.inverseOnSurface, colors.inverseSurface),
            Triple("Inverse accent", colors.inversePrimary, colors.inverseSurface)
        )
        textPairs.forEach { (name, foreground, background) ->
            assertContrast(name, foreground, background, minimum = 4.5f)
        }
    }

    private fun assertContrast(name: String, foreground: Color, background: Color, minimum: Float) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("$name has contrast $ratio; expected at least $minimum", ratio >= minimum)
    }
}
