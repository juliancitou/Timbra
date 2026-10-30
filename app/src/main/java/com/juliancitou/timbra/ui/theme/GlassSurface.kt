package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** Fuente que el vidrio desenfoca; la provee [GlassBackground]. */
val LocalGlassHazeState = staticCompositionLocalOf<HazeState?> { null }

/** `false` fuerza el respaldo sin desenfoque (relleno translúcido), por ejemplo desde Ajustes. */
val LocalGlassBlurEnabled = compositionLocalOf { true }

/** Opacidad del relleno blanco sobre el desenfoque: el diseño pide 8–14 %. */
const val GLASS_FILL_ALPHA = 0.10f

/** Sin desenfoque el relleno sube al 20 % para que el texto siga separado de la aurora. */
const val GLASS_FALLBACK_FILL_ALPHA = 0.20f

private val GlassBlurRadius = 28.dp
private const val GLASS_NOISE = 0.06f
private val GlassShadowElevation = 12.dp

private val GlassBorderBrush = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f)),
)

/**
 * Vidrio translúcido: desenfoca la aurora que queda detrás (Haze), le pone un velo blanco y un
 * borde con degradado. Si no hay fuente o el desenfoque está apagado, usa el relleno al 20 %.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    fillAlpha: Float = GLASS_FILL_ALPHA,
    elevated: Boolean = true,
): Modifier {
    val hazeState = LocalGlassHazeState.current
    val blurEnabled = LocalGlassBlurEnabled.current
    val blurStyle = remember(blurEnabled, fillAlpha) {
        HazeBlurStyle {
            blurEnabled(blurEnabled)
            blurRadius(GlassBlurRadius)
            noiseFactor(GLASS_NOISE)
            backgroundColor(NightBlue)
            colorEffects(listOf(HazeColorEffect.tint(Color.White.copy(alpha = fillAlpha))))
            fallbackColorEffect(HazeColorEffect.tint(Color.White.copy(alpha = GLASS_FALLBACK_FILL_ALPHA)))
        }
    }
    val shadowed = if (elevated) {
        shadow(
            elevation = GlassShadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.35f),
            spotColor = Color.Black.copy(alpha = 0.35f),
        )
    } else {
        this
    }
    val filled = if (hazeState != null) {
        shadowed.clip(shape).hazeBlur(input = HazeInput.Backdrop(hazeState), style = blurStyle)
    } else {
        shadowed.clip(shape).background(Color.White.copy(alpha = GLASS_FALLBACK_FILL_ALPHA))
    }
    return filled.border(width = 1.dp, brush = GlassBorderBrush, shape = shape)
}
