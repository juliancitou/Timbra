package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

enum class GlassButtonStyle { Primary, Secondary }

private val ButtonShape = RoundedCornerShape(GlassCornerRadius)
private val ButtonMinHeight = 52.dp
private const val PRESSED_SCALE = 0.97f
private const val DISABLED_ALPHA = 0.38f

/**
 * Botón de la app. [GlassButtonStyle.Primary] es sólido (ámbar) y debe haber uno solo por
 * pantalla; [GlassButtonStyle.Secondary] es de vidrio para las acciones de apoyo.
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
    val animatePress = !isReducedMotionEnabled()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animatePress) PRESSED_SCALE else 1f,
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "buttonScale",
    )
    val colors = MaterialTheme.colorScheme
    val surface = when (style) {
        GlassButtonStyle.Primary -> Modifier.clip(ButtonShape).background(colors.primary)
        GlassButtonStyle.Secondary -> Modifier.glassSurface(shape = ButtonShape, elevated = false)
    }
    val textColor = if (style == GlassButtonStyle.Primary) colors.onPrimary else colors.onSurface

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .defaultMinSize(minHeight = ButtonMinHeight)
            .then(surface)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = textColor),
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
