package com.juliancitou.timbra.ui.screens.finder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.EqualizerSettings
import com.juliancitou.timbra.audio.equalizerDifference
import com.juliancitou.timbra.measure.FinderDimension
import com.juliancitou.timbra.measure.ZoneChange
import com.juliancitou.timbra.measure.changes
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.DisplayZone
import com.juliancitou.timbra.ui.theme.EqualizerDisplay
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalPanel
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection
import com.juliancitou.timbra.ui.theme.TerminalTextField
import com.juliancitou.timbra.ui.theme.formatDecibels

private const val DECIBELS_SUFFIX = " dB"

/** Dónde se etiqueta cada zona bajo la gráfica. Balance mueve todo el sonido y no tiene un lugar. */
private val DisplayZones = listOf(
    FinderDimension.Bass to 60f,
    FinderDimension.Clarity to 250f,
    FinderDimension.Voice to 2000f,
    FinderDimension.Sibilance to 7000f,
    FinderDimension.Air to 15_000f,
)

/**
 * Resultado del test: la pantalla de ecualizador, la comparación con y sin ecualizar (con el volumen
 * igualado, para que la diferencia sea solo de tono), lo que se añadió o quitó y el nombre del preset.
 */
@Composable
fun FinderResultScreen(
    state: FinderUiState,
    onBypassChange: (Boolean) -> Unit,
    onTogglePlayback: () -> Unit,
    onPresetNameChange: (String) -> Unit,
    onExport: () -> Unit,
    onRestart: () -> Unit,
    onPreviousRound: () -> Unit,
) {
    BackHandler(onBack = onPreviousRound)
    val preset = state.finder.result()

    TerminalScreen(
        title = stringResource(R.string.finder_result_title),
        lead = stringResource(R.string.finder_result_lead),
        onBack = onPreviousRound,
        actions = {
            TerminalButton(
                text = stringResource(R.string.action_export),
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                TerminalButton(
                    text = stringResource(state.playbackLabelRes()),
                    onClick = onTogglePlayback,
                    style = TerminalButtonStyle.Secondary,
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f),
                )
                TerminalButton(
                    text = stringResource(R.string.action_restart_test),
                    onClick = onRestart,
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        TerminalPanel {
            ResultDisplay(state, preset)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            TerminalChip(
                label = stringResource(R.string.finder_compare_without),
                selected = state.bypass,
                onClick = { onBypassChange(true) },
            )
            TerminalChip(
                label = stringResource(R.string.finder_compare_with),
                selected = !state.bypass,
                onClick = { onBypassChange(false) },
            )
        }
        SoundingStatus(state)
        ChangesSections(state)
        TerminalTextField(
            value = state.presetName,
            onValueChange = onPresetNameChange,
            label = stringResource(R.string.finder_preset_label),
            placeholder = suggestedPresetName(state),
        )
    }
}

/** Lo que suena ahora en cada banda: medido o leído del efecto si se pudo, y si no, lo que se pidió. */
private fun soundingGains(state: FinderUiState, preset: EqualizerSettings): List<Float?> =
    state.sounding?.gainsDb ?: if (state.bypass) preset.bandGainsDb.map { 0f } else preset.bandGainsDb

@Composable
private fun ResultDisplay(state: FinderUiState, preset: EqualizerSettings) {
    val changed = state.finder.changes().let { changes -> (changes.added + changes.removed).map { it.dimension } }
    val zones = DisplayZones.filter { (dimension, _) -> dimension in state.finder.valuesDb }.map { (dimension, hz) ->
        DisplayZone(stringResource(dimension.texts().title).lowercase(), hz, dimension in changed)
    }
    val readoutPlain = stringResource(R.string.finder_display_readout_unmeasured)
    val readoutFull = stringResource(R.string.finder_display_readout)
    EqualizerDisplay(
        presetDb = preset.bandGainsDb,
        soundingDb = soundingGains(state, preset),
        frequenciesHz = preset.bandFrequenciesHz,
        zones = zones,
        statusLabel = stringResource(if (state.bypass) R.string.finder_display_off else R.string.finder_display_on),
        statusActive = !state.bypass,
        legend = stringResource(R.string.finder_display_legend),
        readout = { frequency, wanted, sounding ->
            val presetValue = wanted.removeSuffix(DECIBELS_SUFFIX)
            if (sounding == null) {
                readoutPlain.format(frequency, presetValue)
            } else {
                readoutFull.format(frequency, presetValue, sounding.removeSuffix(DECIBELS_SUFFIX))
            }
        },
        contentDescription = stringResource(R.string.finder_result_chart),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** De dónde sale lo que muestra la pantalla. Si no se pudo confirmar, se dice en rojo. */
@Composable
private fun SoundingStatus(state: FinderUiState) {
    val check = state.sounding
    val proof = check?.proof
    val detail = when {
        check == null -> stringResource(R.string.finder_sounding_pending)
        proof is SoundingProof.ReadFromEffect && proof.isConfirmed ->
            stringResource(R.string.finder_sounding_confirmed, proof.confirmedBands, proof.totalBands)
        proof is SoundingProof.ReadFromEffect ->
            stringResource(R.string.finder_sounding_unconfirmed, proof.confirmedBands, proof.totalBands)
        else -> stringResource(R.string.finder_sounding_measured)
    }
    val failed = proof is SoundingProof.ReadFromEffect && !proof.isConfirmed
    DetailText(detail, color = if (failed) MaterialTheme.colorScheme.error else Color.Unspecified)
}

/** Lo que cambió respecto al sonido original, en palabras: quien no sabe de audio no tiene que leer la gráfica. */
@Composable
private fun ChangesSections(state: FinderUiState) {
    val changes = state.finder.changes()
    @Composable
    fun lines(zones: List<ZoneChange>) = zones.map { (dimension, valueDb) ->
        val texts = dimension.texts()
        stringResource(
            R.string.finder_change_line,
            stringResource(texts.title),
            formatDecibels(valueDb.toFloat()),
            stringResource(if (valueDb < 0) texts.lower else texts.upper),
        )
    }
    if (changes.added.isNotEmpty()) {
        TerminalSection(stringResource(R.string.finder_section_added)) { CodeLines(lines(changes.added)) }
    }
    if (changes.removed.isNotEmpty()) {
        TerminalSection(stringResource(R.string.finder_section_removed)) { CodeLines(lines(changes.removed)) }
    }
    if (changes.unchanged.isNotEmpty()) {
        TerminalSection(stringResource(R.string.finder_section_unchanged)) {
            DetailText(changes.unchanged.map { stringResource(it.texts().title) }.joinToString(", "))
        }
    }
    if (equalizerDifference(state.finder.result()).isSubtle) {
        DetailText(stringResource(R.string.finder_difference_subtle))
    }
    state.finder.doubts.sortedBy { it.ordinal }.forEach { dimension ->
        DetailText(stringResource(R.string.finder_summary_doubt, stringResource(dimension.texts().title)))
    }
}
