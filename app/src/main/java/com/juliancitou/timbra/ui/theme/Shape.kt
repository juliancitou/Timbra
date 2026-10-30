package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Esquinas casi rectas, como ventanas y teclas de una terminal: las tarjetas de vidrio llevan
 * [GlassCornerRadius] y los controles pequeños, menos.
 */
val GlassCornerRadius = 14.dp
val ControlCornerRadius = 8.dp
val ChipCornerRadius = 6.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(ChipCornerRadius),
    medium = RoundedCornerShape(ControlCornerRadius),
    large = RoundedCornerShape(GlassCornerRadius),
    extraLarge = RoundedCornerShape(20.dp),
)

object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val minTouchTarget = 48.dp
    val screenHorizontal = 32.dp
}
