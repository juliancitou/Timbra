package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mancha de la aurora. Posiciones y radio son fracciones del tamaño de la pantalla, para que se
 * vea igual en cualquier resolución.
 */
private data class AuroraBlob(
    val color: Color,
    val alpha: Float,
    val centerX: Float,
    val centerY: Float,
    val driftX: Float,
    val driftY: Float,
    val radius: Float,
    val phase: Float,
)

private val AuroraBlobs = listOf(
    AuroraBlob(
        SmokeLight,
        alpha = 0.07f,
        centerX = 0.20f,
        centerY = 0.15f,
        driftX = 0.10f,
        driftY = 0.06f,
        radius = 0.70f,
        phase = 0f,
    ),
    AuroraBlob(
        SmokeDark,
        alpha = 0.10f,
        centerX = 0.80f,
        centerY = 0.75f,
        driftX = 0.08f,
        driftY = 0.08f,
        radius = 0.80f,
        phase = 3.1f,
    ),
)

private const val AURORA_CYCLE_MS = 40_000
private const val AURORA_FRAMES_PER_SECOND = 15

private val NightGradient = Brush.verticalGradient(listOf(Ink, Graphite, Charcoal))

/**
 * Fondo de todas las pantallas: degradado negro con dos nubes grises muy tenues que se desplazan lento.
 * El progreso de la animación se lee solo en la fase de dibujo, así que no recompone la pantalla.
 */
@Composable
fun TerminalBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = rememberAuroraProgress(animated = !isReducedMotionEnabled())
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NightGradient)
                .drawBehind { drawAurora(progress.value) },
        )
        content()
    }
}

/**
 * Progreso de la aurora (0 a 1) actualizado a [AURORA_FRAMES_PER_SECOND]. Se mueve tan lento que
 * a 15 cuadros por segundo no se nota, y así la pantalla no tiene que redibujar el fondo en
 * cada cuadro de una pantalla de 120 Hz.
 */
@Composable
private fun rememberAuroraProgress(animated: Boolean): State<Float> {
    val progress = remember { mutableFloatStateOf(0f) }
    if (animated) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) {
                val elapsed = withFrameMillis { it } - start
                progress.floatValue = (elapsed % AURORA_CYCLE_MS).toFloat() / AURORA_CYCLE_MS
                delay(1000L / AURORA_FRAMES_PER_SECOND)
            }
        }
    }
    return progress
}

private fun DrawScope.drawAurora(progress: Float) {
    val angle = progress * 2f * PI.toFloat()
    val shortSide = minOf(size.width, size.height)
    AuroraBlobs.forEach { blob ->
        val center = Offset(
            x = size.width * (blob.centerX + blob.driftX * sin(angle + blob.phase)),
            y = size.height * (blob.centerY + blob.driftY * cos(angle + blob.phase)),
        )
        val radius = shortSide * blob.radius
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob.color.copy(alpha = blob.alpha), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}
