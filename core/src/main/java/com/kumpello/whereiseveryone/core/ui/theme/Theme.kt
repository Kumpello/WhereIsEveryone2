package com.kumpello.whereiseveryone.core.ui.theme

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
