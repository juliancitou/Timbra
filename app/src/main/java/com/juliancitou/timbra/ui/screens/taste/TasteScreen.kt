package com.juliancitou.timbra.ui.screens.taste

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ
import com.juliancitou.timbra.ui.screens.SampleData
import com.juliancitou.timbra.ui.theme.FrequencyChart
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.GlassSlider

private val TasteRangeDb = -6f..6f

@Composable
fun TasteScreen(onExport: () -> Unit, onBack: () -> Unit) {
    var bassDb by rememberSaveable { mutableFloatStateOf(0f) }
    var voiceDb by rememberSaveable { mutableFloatStateOf(0f) }
    var brightnessDb by rememberSaveable { mutableFloatStateOf(0f) }
    val previewDb = SampleData.curveDb.mapIndexed { index, gain ->
        gain + sampleTasteOffset(POWERAMP_BAND_FREQUENCIES_HZ[index], bassDb, voiceDb, brightnessDb)
    }

    GlassScreen(
        title = stringResource(R.string.taste_title),
        subtitle = stringResource(R.string.taste_subtitle),
        onBack = onBack,
        actions = {
            GlassButton(
                text = stringResource(R.string.action_export),
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassCard {
            FrequencyChart(
                gainsDb = previewDb,
                contentDescription = stringResource(R.string.taste_preview_description),
                estimatedBandIndices = SampleData.estimatedBandIndices,
            )
        }
        TasteControl(R.string.taste_bass, bassDb) { bassDb = it }
        TasteControl(R.string.taste_voice, voiceDb) { voiceDb = it }
        TasteControl(R.string.taste_brightness, brightnessDb) { brightnessDb = it }
    }
}

@Composable
private fun TasteControl(labelRes: Int, valueDb: Float, onValueChange: (Float) -> Unit) {
    val label = stringResource(labelRes)
    GlassCard {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        GlassSlider(
            valueDb = valueDb,
            onValueChange = onValueChange,
            label = label,
            valueRange = TasteRangeDb,
        )
    }
}

/**
 * Aproximación solo para la vista previa de la Fase 1: cada control mueve su zona entera.
 * La curva de gusto real (con transiciones suaves entre zonas) se diseña en la Fase 4.
 */
private fun sampleTasteOffset(frequencyHz: Int, bassDb: Float, voiceDb: Float, brightnessDb: Float): Float =
    when {
        frequencyHz <= 150 -> bassDb
        frequencyHz in 1000..4000 -> voiceDb
        frequencyHz >= 6000 -> brightnessDb
        else -> 0f
    }
