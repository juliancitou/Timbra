package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class GlassButtonStyle { Primary, Secondary }

private val ButtonMinHeight = 52.dp
private const val PRESSED_SCALE = 0.98f
private const val DISABLED_ALPHA = 0.38f
private const val SECONDARY_FRAME_ALPHA = 0.55f

/**
 * Botón con aspecto de recuadro ASCII (┌ - - ┐ │ texto │ └ - - ┘), sin fondo: solo el contorno y
 * el texto. [GlassButtonStyle.Primary] va en ámbar y debe haber uno solo por pantalla;
 * [GlassButtonStyle.Secondary] va en gris y blanco. Al presionar, los guiones se unen en una línea
 * continua.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animate = !isReducedMotionEnabled()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animate) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "buttonScale",
    )
    val joined by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (animate) tween(MotionDuration.SHORT_MS, easing = EmphasizedDecelerate) else tween(0),
        label = "frameJoined",
    )
    val colors = MaterialTheme.colorScheme
    val (frameColor, textColor) = when (style) {
        GlassButtonStyle.Primary -> colors.primary to colors.primary
        GlassButtonStyle.Secondary -> colors.onSurfaceVariant.copy(alpha = SECONDARY_FRAME_ALPHA) to colors.onSurface
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .defaultMinSize(minHeight = ButtonMinHeight)
            .asciiFrame(color = frameColor, pressed = { joined })
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = frameColor),
                enabled = enabled,
                role = Role.Button,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onClick()
            }
            .padding(horizontal = Spacing.large, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
