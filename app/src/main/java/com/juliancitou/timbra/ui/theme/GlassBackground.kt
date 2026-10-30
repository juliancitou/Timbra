package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
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

private val NightGradient = Brush.verticalGradient(listOf(Ink, Graphite, Charcoal))

/**
 * Fondo de todas las pantallas: degradado negro con dos nubes grises muy tenues que se desplazan lento.
 * Es la fuente que desenfocan los componentes de vidrio del [content].
 * El progreso de la animación se lee solo en la fase de dibujo, así que no recompone la pantalla.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = rememberAuroraProgress(animated = !isReducedMotionEnabled())
    val hazeState = rememberHazeState()
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(NightGradient)
                .drawBehind { drawAurora(progress.value) },
        )
        CompositionLocalProvider(LocalGlassHazeState provides hazeState) {
            content()
        }
    }
}

@Composable
private fun rememberAuroraProgress(animated: Boolean): State<Float> {
    if (!animated) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "aurora").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AURORA_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auroraProgress",
    )
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
