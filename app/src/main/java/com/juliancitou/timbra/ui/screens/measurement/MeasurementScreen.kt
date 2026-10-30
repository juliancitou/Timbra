package com.juliancitou.timbra.ui.screens.measurement

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import com.juliancitou.timbra.ui.theme.EmphasizedDecelerate
import com.juliancitou.timbra.ui.theme.FitText
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.GlassSlider
import com.juliancitou.timbra.ui.theme.LeadText
import com.juliancitou.timbra.ui.theme.MotionDuration
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.formatHertz
import com.juliancitou.timbra.ui.theme.withTabularNumbers

/** Empieza en una banda media para que el ejemplo muestre una explicación representativa. */
private const val SAMPLE_START_BAND_INDEX = 6

@Composable
fun MeasurementScreen(onFinished: () -> Unit, onBack: () -> Unit) {
    var bandIndex by rememberSaveable { mutableIntStateOf(SAMPLE_START_BAND_INDEX) }
    var gainDb by rememberSaveable { mutableFloatStateOf(0f) }
    val bandCount = POWERAMP_BAND_FREQUENCIES_HZ.size
    val frequencyHz = POWERAMP_BAND_FREQUENCIES_HZ[bandIndex]
    val progress by animateFloatAsState(
        targetValue = (bandIndex + 1f) / bandCount,
        animationSpec = tween(MotionDuration.LONG_MS, easing = EmphasizedDecelerate),
        label = "bandProgress",
    )

    fun nextBand() {
        if (bandIndex == bandCount - 1) {
            onFinished()
        } else {
            bandIndex++
            gainDb = 0f
        }
    }

    GlassScreen(
        title = stringResource(R.string.measurement_title),
        lead = stringResource(R.string.measurement_lead),
        onBack = onBack,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                GlassButton(
                    text = stringResource(R.string.action_repeat),
                    onClick = { gainDb = 0f },
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(R.string.action_skip),
                    onClick = ::nextBand,
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            GlassButton(
                text = stringResource(R.string.action_sounds_equal),
                onClick = ::nextBand,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            SectionLabel(stringResource(R.string.measurement_progress, bandIndex + 1, bandCount))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = Color.White.copy(alpha = 0.10f),
                drawStopIndicator = {},
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            FitText(
                text = formatHertz(frequencyHz),
                style = MaterialTheme.typography.displaySmall.withTabularNumbers(),
                color = MaterialTheme.colorScheme.primary,
            )
            LeadText(stringResource(bandHintRes(frequencyHz)))
        }
        GlassSlider(
            valueDb = gainDb,
            onValueChange = { gainDb = it },
            label = stringResource(R.string.measurement_band_gain),
        )
    }
}

/** Explicación corta de qué se oye en cada zona, para quien no sabe de audio. */
@StringRes
private fun bandHintRes(frequencyHz: Int): Int = when {
    frequencyHz < 50 -> R.string.measurement_hint_sub_bass
    frequencyHz < 120 -> R.string.measurement_hint_bass
    frequencyHz < 400 -> R.string.measurement_hint_mud
    frequencyHz < 1500 -> R.string.measurement_hint_voice
    frequencyHz < 4500 -> R.string.measurement_hint_presence
    frequencyHz < 13000 -> R.string.measurement_hint_sibilance
    else -> R.string.measurement_hint_air
}
