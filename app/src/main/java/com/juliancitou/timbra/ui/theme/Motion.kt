package com.juliancitou.timbra.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object MotionDuration {
    const val SHORT_MS = 200
    const val MEDIUM_MS = 300
    const val LONG_MS = 350

    /** La transformación del engrane a Ajustes va más lenta a propósito, para que se aprecie. */
    const val CONTAINER_TRANSFORM_MS = 550

    /** Separación entre elementos de una lista que aparecen uno tras otro. */
    const val STAGGER_MS = 45
}

/** Curvas "enfatizadas" de Material 3: arranque rápido y frenado suave al entrar; al revés al salir. */
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * `true` si el usuario desactivó las animaciones del sistema (Opciones de accesibilidad o de
 * desarrollador ponen la escala de animación en 0). En ese caso la aurora queda fija.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    return remember(contentResolver) {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private val EntranceOffset = 10.dp

/**
 * Entrada escalonada: el elemento número [index] aparece desvaneciendo y subiendo 10 dp, un poco
 * después que el anterior. Solo la primera vez que se muestra; sin animación si se reduce el
 * movimiento.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val animated = !isReducedMotionEnabled()
    val progress = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animated) return@LaunchedEffect
        delay(index * MotionDuration.STAGGER_MS.toLong())
        progress.animateTo(1f, tween(MotionDuration.MEDIUM_MS, easing = EmphasizedDecelerate))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1 - progress.value) * EntranceOffset.toPx()
    }
}
