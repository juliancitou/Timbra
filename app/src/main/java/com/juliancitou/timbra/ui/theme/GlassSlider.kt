package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val ThumbSize = 32.dp
private val SliderHeight = 56.dp

/**
 * Control de ganancia en dB, pensado para el pulgar. Avanza en pasos de [stepDb] con un "clic"
 * háptico en cada paso, y muestra el valor grande arriba con dígitos tabulares.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSlider(
    valueDb: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -12f..12f,
    stepDb: Float = 0.5f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val lastStep = remember { mutableFloatStateOf(valueDb) }
    val stepCount = ((valueRange.endInclusive - valueRange.start) / stepDb).roundToInt() - 1
    val formattedValue = formatDecibels(valueDb)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = formattedValue,
            style = MaterialTheme.typography.displayMedium.withTabularNumbers(),
            color = colors.onSurface,
        )
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
            thumb = {
                Box(
                    modifier = Modifier
                        .size(ThumbSize)
                        .shadow(6.dp, CircleShape)
                        .background(colors.primary, CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
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
