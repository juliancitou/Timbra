package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** Escena de transiciones compartidas de la navegación; la provee el `NavHost`. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Animación de entrada/salida de la pantalla actual; la provee cada destino del `NavHost`. */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Clave que une el engrane de Inicio con la pantalla de Ajustes. */
const val SETTINGS_TRANSFORM_KEY = "settings"

private val ContainerBounds = BoundsTransform { _, _ ->
    tween(MotionDuration.CONTAINER_TRANSFORM_MS, easing = EmphasizedDecelerate)
}

/**
 * Transformación de contenedor: el elemento con este modificador en una pantalla y el que lo lleva
 * en la otra comparten límites, así que al navegar uno "crece" hasta convertirse en el otro (y se
 * encoge de regreso). Sin escena compartida o con movimiento reducido no hace nada.
 */
@Composable
fun Modifier.containerTransform(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    if (sharedScope == null || visibilityScope == null || isReducedMotionEnabled()) return this
    return with(sharedScope) {
        this@containerTransform.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(tween(MotionDuration.CONTAINER_TRANSFORM_MS / 2, delayMillis = 120)),
            exit = fadeOut(tween(MotionDuration.CONTAINER_TRANSFORM_MS / 3)),
            boundsTransform = ContainerBounds,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(PanelCornerRadius)),
        )
    }
}
