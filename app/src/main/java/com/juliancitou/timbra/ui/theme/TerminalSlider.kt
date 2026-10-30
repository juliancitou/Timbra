package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** [Large] muestra el valor grande (medición); [Compact] etiqueta y valor en una línea (gusto). */
enum class TerminalSliderSize { Large, Compact }

private val ThumbWidth = 14.dp
private val ThumbHeight = 34.dp
private val ThumbShape = RoundedCornerShape(4.dp)
private const val DRAGGING_THUMB_SCALE = 1.18f
private val SliderHeight = 56.dp

/**
 * Control de ganancia en dB, pensado para el pulgar. Avanza en pasos de [stepDb] con un "clic"
 * háptico en cada paso; el valor va con dígitos tabulares para que no "baile".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSlider(
    valueDb: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -12f..12f,
    stepDb: Float = 0.5f,
    size: TerminalSliderSize = TerminalSliderSize.Large,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val lastStep = remember { mutableFloatStateOf(valueDb) }
    val stepCount = ((valueRange.endInclusive - valueRange.start) / stepDb).roundToInt() - 1
    val formattedValue = formatDecibels(valueDb)
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val dragging by interactionSource.collectIsDraggedAsState()
    val pressing by interactionSource.collectIsPressedAsState()
    val thumbScale by animateFloatAsState(
        targetValue = if ((dragging || pressing) && !isReducedMotionEnabled()) DRAGGING_THUMB_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "thumbScale",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        when (size) {
            TerminalSliderSize.Large -> FitText(
                text = formattedValue,
                style = MaterialTheme.typography.displayMedium.withTabularNumbers(),
                color = colors.onSurface,
            )
            TerminalSliderSize.Compact -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.titleMedium.withTabularNumbers(),
                    color = colors.primary,
                )
            }
        }
        Slider(
            value = valueDb,
            onValueChange = { newValue ->
                if (newValue != lastStep.floatValue) {
                    lastStep.floatValue = newValue
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                onValueChange(newValue)
            },
            valueRange = valueRange,
            steps = stepCount,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                activeTrackColor = colors.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            interactionSource = interactionSource,
            thumb = {
                // Perilla con forma de cursor de terminal (▌) que crece mientras se arrastra.
                Box(
                    modifier = Modifier
                        .size(width = ThumbWidth, height = ThumbHeight)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .shadow(6.dp, ThumbShape)
                        .background(colors.primary, ThumbShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), ThumbShape),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(SliderHeight)
                .semantics {
                    contentDescription = label
                    stateDescription = formattedValue
                },
        )
    }
}
