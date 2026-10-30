package com.juliancitou.timbra.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escuadras en las cuatro esquinas (⌜ ⌝ ⌞ ⌟), como el marco de selección de un editor de código o
 * el visor de una cámara: el toque de "terminal" sobre el vidrio. [inset] las aleja del borde;
 * animarlo hace que se "cierren" al presionar. Se leen en la fase de dibujo, sin recomponer.
 */
fun Modifier.bracketCorners(
    color: Color,
    inset: () -> Dp,
    length: Dp = 9.dp,
    strokeWidth: Dp = 1.5.dp,
): Modifier = drawWithContent {
    drawContent()
    val margin = inset().toPx()
    val arm = length.toPx()
    val stroke = strokeWidth.toPx()
    val left = margin
    val top = margin
    val right = size.width - margin
    val bottom = size.height - margin
    val corners = listOf(
        Triple(Offset(left, top), 1f, 1f),
        Triple(Offset(right, top), -1f, 1f),
        Triple(Offset(left, bottom), 1f, -1f),
        Triple(Offset(right, bottom), -1f, -1f),
    )
    corners.forEach { (corner, horizontal, vertical) ->
        drawLine(color, corner, corner + Offset(arm * horizontal, 0f), stroke, StrokeCap.Square)
        drawLine(color, corner, corner + Offset(0f, arm * vertical), stroke, StrokeCap.Square)
    }
}
