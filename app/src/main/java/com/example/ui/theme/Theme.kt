package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RafikDarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldPrimaryContainer,
    onPrimaryContainer = DarkEmeraldOnPrimaryContainer,
    secondary = DarkGoldSecondary,
    onSecondary = DarkGoldOnSecondary,
    secondaryContainer = DarkGoldSecondaryContainer,
    onSecondaryContainer = DarkGoldOnSecondaryContainer,
    tertiary = DarkTealTertiary,
    background = DarkOasisBackground,
    onBackground = DarkOnSurface,
    surface = DarkOasisSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkOasisSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkOasisSurfaceLow,
    surfaceContainer = DarkOasisSurfaceContainer,
    surfaceContainerHigh = DarkOasisSurfaceHigh,
    outlineVariant = DarkOutlineVariant,
    error = DarkErrorRed,
    errorContainer = DarkErrorContainer
)

private val RafikLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = GoldSecondary,
    onSecondary = GoldOnSecondary,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = TealOnTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = TealOnTertiaryContainer,
    background = SandBackground,
    onBackground = InkOnSurface,
    surface = SandSurface,
    onSurface = InkOnSurface,
    surfaceVariant = SandSurfaceContainer,
    onSurfaceVariant = InkOnSurfaceVariant,
    surfaceContainerLow = SandSurfaceLow,
    surfaceContainer = SandSurfaceContainer,
    surfaceContainerHigh = SandSurfaceHigh,
    outline = SandOutline,
    outlineVariant = SandOutlineVariant,
    error = ErrorRed,
    errorContainer = ErrorContainerLight
)

val RafikShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RafikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RafikDarkColorScheme else RafikLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RafikTypography,
        shapes = RafikShapes,
        content = content
    )
}
