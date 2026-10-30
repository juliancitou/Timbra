package com.juliancitou.timbra.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Qué hace el ícono al tocarlo, antes o mientras se ejecuta la acción. */
enum class IconMotion {
    None,

    /** Gira media vuelta, como un engrane que arranca. La acción empieza a mitad del giro. */
    Spin,

    /** Se impulsa hacia la izquierda y regresa con rebote, como una flecha de "volver". */
    NudgeBack,
}

private const val SPIN_DEGREES = 180f
private const val SPIN_MS = 600
private const val SPIN_ACTION_DELAY_MS = 140L
private val NudgeDistance = 6.dp

/** Solo el ícono, sin fondo ni contorno; el área táctil sigue siendo de 48 dp. */
@Composable
fun GlassIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    motion: IconMotion = IconMotion.None,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val animate = !isReducedMotionEnabled() && motion != IconMotion.None
    val rotation = remember { Animatable(0f) }
    val offset = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(Spacing.minTouchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = Spacing.minTouchTarget / 2),
                role = Role.Button,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                if (!animate) {
                    onClick()
                    return@clickable
                }
                when (motion) {
                    IconMotion.Spin -> scope.launch {
                        launch {
                            val spin = tween<Float>(SPIN_MS, easing = EmphasizedDecelerate)
                            rotation.animateTo(rotation.value + SPIN_DEGREES, spin)
                        }
                        delay(SPIN_ACTION_DELAY_MS)
                        onClick()
                    }
                    IconMotion.NudgeBack -> {
                        scope.launch {
                            offset.animateTo(-1f, tween(90))
                            offset.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 500f))
                        }
                        onClick()
                    }
                    IconMotion.None -> onClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.graphicsLayer {
                rotationZ = rotation.value
                translationX = offset.value * NudgeDistance.toPx()
            },
        )
    }
}
