package com.juliancitou.timbra.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

private val StrokeWidth = 1.5.dp
private val CornerArm = 9.dp
private val DashLength = 5.dp
private val DashPitch = 13.dp
private val CornerGap = 6.dp

/**
 * Marco con aspecto de recuadro ASCII, sin relleno:
 *
 * ```
 * ┌ - - - - - - - - ┐
 * │   Texto         │
 * └ - - - - - - - - ┘
 * ```
 *
 * Se dibuja en vez de escribirse con caracteres para que encaje en cualquier ancho y tamaño de
 * letra: los guiones se reparten de forma pareja entre las esquinas. [pressed] (0 a 1) alarga los
 * guiones hasta unirlos en una línea continua.
 */
fun Modifier.asciiFrame(color: Color, pressed: () -> Float = { 0f }): Modifier = drawWithContent {
    drawContent()
    val stroke = StrokeWidth.toPx()
    val half = stroke / 2
    val arm = CornerArm.toPx()
    val left = half
    val top = half
    val right = size.width - half
    val bottom = size.height - half

    drawCorner(color, Offset(left, top), 1f, 1f, arm, stroke)
    drawCorner(color, Offset(right, top), -1f, 1f, arm, stroke)
    drawCorner(color, Offset(left, bottom), 1f, -1f, arm, stroke)
    drawCorner(color, Offset(right, bottom), -1f, -1f, arm, stroke)

    drawLine(color, Offset(left, top + arm), Offset(left, bottom - arm), stroke)
    drawLine(color, Offset(right, top + arm), Offset(right, bottom - arm), stroke)

    val start = left + arm + CornerGap.toPx()
    val end = right - arm - CornerGap.toPx()
    drawDashes(color, start, end, top, stroke, pressed())
    drawDashes(color, start, end, bottom, stroke, pressed())
}

/** Esquina en L (┌ ┐ └ ┘) que sale de [corner] hacia dentro según los signos de dirección. */
private fun DrawScope.drawCorner(
    color: Color,
    corner: Offset,
    horizontal: Float,
    vertical: Float,
    arm: Float,
    stroke: Float,
) {
    drawLine(color, corner, corner + Offset(arm * horizontal, 0f), stroke, StrokeCap.Square)
    drawLine(color, corner, corner + Offset(0f, arm * vertical), stroke, StrokeCap.Square)
}

/**
 * Guiones centrados entre [start] y [end] a la altura [y], con separación ajustada para que el
 * primero y el último toquen los extremos. Con [joined] en 1 los huecos desaparecen.
 */
private fun DrawScope.drawDashes(color: Color, start: Float, end: Float, y: Float, stroke: Float, joined: Float) {
    val span = end - start
    if (span <= 0f) return
    val dash = DashLength.toPx()
    val count = max(1, ((span + DashPitch.toPx() - dash) / DashPitch.toPx()).roundToInt())
    val pitch = if (count == 1) 0f else (span - dash) / (count - 1)
    val length = dash + (pitch - dash).coerceAtLeast(0f) * joined.coerceIn(0f, 1f)
    repeat(count) { index ->
        val x = start + index * pitch
        drawLine(color, Offset(x, y), Offset(minOf(x + length, end), y), stroke)
    }
}
