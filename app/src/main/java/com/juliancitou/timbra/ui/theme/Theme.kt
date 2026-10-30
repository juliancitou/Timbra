package com.juliancitou.timbra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TimbraColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = AmberDeep,
    primaryContainer = AmberContainer,
    onPrimaryContainer = Amber,
    secondary = Orange,
    onSecondary = OrangeDeep,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = TextSecondary,
    onTertiary = Ink,
    error = DangerRed,
    onError = DangerRedDeep,
    background = Ink,
    onBackground = TextPrimary,
    surface = Charcoal,
    onSurface = TextPrimary,
    surfaceVariant = Graphite,
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
