package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class GlassButtonStyle { Primary, Secondary }

private val ButtonShape = RoundedCornerShape(ControlCornerRadius)
private val ButtonMinHeight = 52.dp
private val BracketRestInset = 5.dp
private val BracketPressedInset = 9.dp
private const val PRESSED_SCALE = 0.98f
private const val DISABLED_ALPHA = 0.38f

/**
 * Botón de la app con aire de terminal: esquinas casi rectas y escuadras ⌜ ⌝ ⌞ ⌟ que se cierran
 * hacia el centro al presionar. [GlassButtonStyle.Primary] es ámbar sólido y debe haber uno solo
 * por pantalla; [GlassButtonStyle.Secondary] es de vidrio con borde fino para acciones de apoyo.
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
    val bracketInset by animateDpAsState(
        targetValue = if (pressed && animate) BracketPressedInset else BracketRestInset,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "bracketInset",
    )
    val colors = MaterialTheme.colorScheme
    val (surface, textColor, bracketColor) = when (style) {
        GlassButtonStyle.Primary -> Triple(
            Modifier.clip(ButtonShape).background(colors.primary),
            colors.onPrimary,
            colors.onPrimary.copy(alpha = 0.55f),
        )
        GlassButtonStyle.Secondary -> Triple(
            Modifier
                .glassSurface(shape = ButtonShape, elevated = false)
                .border(1.dp, Color.White.copy(alpha = 0.10f), ButtonShape),
            colors.onSurface,
            colors.primary.copy(alpha = 0.85f),
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .defaultMinSize(minHeight = ButtonMinHeight)
            .then(surface)
            .bracketCorners(color = bracketColor, inset = { bracketInset })
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
