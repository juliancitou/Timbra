package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Esquinas casi rectas, como ventanas y teclas de una terminal. */
val PanelCornerRadius = 14.dp

/** Casi recta: botones y opciones deben verse como una terminal, no como píldoras. */
val ButtonCornerRadius = 2.dp
val ControlCornerRadius = 8.dp
val ChipCornerRadius = 6.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(ChipCornerRadius),
    medium = RoundedCornerShape(ControlCornerRadius),
    large = RoundedCornerShape(PanelCornerRadius),
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
