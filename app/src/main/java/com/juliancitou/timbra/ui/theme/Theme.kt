package com.juliancitou.timbra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TimbraColorScheme = darkColorScheme(
    primary = Aqua,
    onPrimary = AquaDeep,
    primaryContainer = AquaContainer,
    onPrimaryContainer = Aqua,
    secondary = Violet,
    onSecondary = VioletDeep,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = WarningAmber,
    onTertiary = WarningAmberDeep,
    error = DangerRed,
    onError = DangerRedDeep,
    background = NightBlue,
    onBackground = TextPrimary,
    surface = DeepIndigo,
    onSurface = TextPrimary,
    surfaceVariant = NightViolet,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
)

/** Tema siempre oscuro: el vidrio translúcido y la aurora se diseñaron solo para fondo oscuro. */
@Composable
fun TimbraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TimbraColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
