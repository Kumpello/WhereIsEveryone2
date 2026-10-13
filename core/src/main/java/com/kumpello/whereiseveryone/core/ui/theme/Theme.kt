package com.kumpello.whereiseveryone.core.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kumpello.whereiseveryone.core.ui.components.Button
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val DarkColorScheme = darkColorScheme(

    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,

    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,

    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,

    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,

    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,

    primaryFixed = LightPrimaryContainer,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = LightOnPrimaryContainer,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = LightSecondaryContainer,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = LightOnSecondaryContainer,
    onSecondaryFixedVariant = DarkSecondaryContainer,
    tertiaryFixed = LightTertiaryContainer,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = LightOnTertiaryContainer,
    onTertiaryFixedVariant = DarkTertiaryContainer,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    surfaceDim = DarkSurface,
    surfaceBright = DarkSurfaceContainerHighest,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceTint = DarkPrimary,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    error = DarkError,
    onError = DarkOnError,

    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

internal val LightColorScheme = lightColorScheme(

    primary = LightPrimary,
    onPrimary = LightOnPrimary,

    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,

    secondary = LightSecondary,
    onSecondary = LightOnSecondary,

    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,

    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,

    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,

    primaryFixed = LightPrimaryContainer,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = LightOnPrimaryContainer,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = LightSecondaryContainer,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = LightOnSecondaryContainer,
    onSecondaryFixedVariant = DarkSecondaryContainer,
    tertiaryFixed = LightTertiaryContainer,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = LightOnTertiaryContainer,
    onTertiaryFixedVariant = DarkTertiaryContainer,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    surfaceDim = LightSurfaceContainerHighest,
    surfaceBright = LightSurface,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceTint = LightPrimary,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,

    outline = LightOutline,
    outlineVariant = LightOutlineVariant,

    error = LightError,
    onError = LightOnError,

    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

@Composable
fun WhereIsEveryoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = if (darkTheme)
            DarkColorScheme
        else
            LightColorScheme,

        typography = Typography,
        shapes = Shapes,

        content = content
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemePreview() {
    WhereIsEveryoneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Text("Where Is Everyone", style = MaterialTheme.typography.headlineMedium)
                Text("Stay connected with your friends.", style = MaterialTheme.typography.bodyLarge)
                Button.Primary(text = "Primary action", onClick = {})
                Button.Secondary(text = "Secondary action", onClick = {})
            }
        }
    }
}
