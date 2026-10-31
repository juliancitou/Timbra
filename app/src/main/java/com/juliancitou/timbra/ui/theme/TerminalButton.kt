package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class TerminalButtonStyle { Primary, Secondary }

private val ButtonMinHeight = 52.dp
private const val PRESSED_SCALE = 0.98f
private const val DISABLED_ALPHA = 0.38f
private val FrameWidth = 1.5.dp

/**
 * Botón de bordes claros, para que se vea dónde empieza y dónde termina cada opción.
 * [TerminalButtonStyle.Primary] va relleno de ámbar con letra oscura, como el video inverso de una
 * terminal, y debe haber uno solo por pantalla; [TerminalButtonStyle.Secondary] lleva línea continua
 * gris y letra blanca.
 */
@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TerminalButtonStyle = TerminalButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !isReducedMotionEnabled()) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "buttonScale",
    )
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(ButtonCornerRadius)
    val surface = when (style) {
        TerminalButtonStyle.Primary -> Modifier.background(colors.primary, shape)
        TerminalButtonStyle.Secondary -> Modifier.border(FrameWidth, colors.onSurfaceVariant, shape)
    }
    val textColor = if (style == TerminalButtonStyle.Primary) Ink else colors.onSurface

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .defaultMinSize(minHeight = ButtonMinHeight)
            .then(surface)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = textColor),
                enabled = enabled,
                role = Role.Button,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onClick()
            }
            .padding(horizontal = Spacing.medium, vertical = 14.dp),
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
