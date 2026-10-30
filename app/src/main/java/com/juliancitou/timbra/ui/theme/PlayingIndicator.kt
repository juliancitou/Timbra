package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val BARS = 4
private const val CYCLE_MS = 1_200
private const val BAR_OFFSET = 0.27f
private const val BAR_SPEEDUP = 0.35f

/**
 * Pequeño medidor de barras que "baila" mientras algo suena. Es decorativo: no refleja el audio
 * real. Queda quieto si se reduce el movimiento.
 */
@Composable
fun PlayingIndicator(playing: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val animated = playing && !isReducedMotionEnabled()
    val phase = if (animated) {
        rememberInfiniteTransition(label = "meter").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "meterPhase",
        )
    } else {
        null
    }
    Canvas(modifier = modifier.size(width = 22.dp, height = 16.dp)) {
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (BARS - 1)) / BARS
        repeat(BARS) { bar ->
            // Cada barra oscila a su propio ritmo para que el conjunto no se vea mecánico.
            val wave = phase?.value?.let {
                0.5f + 0.5f * sin(2 * PI.toFloat() * (it + bar * BAR_OFFSET) * (1 + bar * BAR_SPEEDUP))
            }
            val level = if (playing) 0.25f + 0.75f * (wave ?: 0.6f) else 0.2f
            val height = size.height * level
            drawRoundRect(
                color = color.copy(alpha = if (playing) 1f else 0.4f),
                topLeft = Offset(bar * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}
