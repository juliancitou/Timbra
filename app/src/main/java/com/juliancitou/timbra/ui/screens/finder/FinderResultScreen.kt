package com.juliancitou.timbra.ui.screens.finder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.equalizerDifference
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.FrequencyChart
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalPanel
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection
import com.juliancitou.timbra.ui.theme.TerminalTextField
import com.juliancitou.timbra.ui.theme.formatDecibels
import com.juliancitou.timbra.ui.theme.formatHertz

/** Menos que esto, en dB, casi no se distingue de oído. */
private const val NOTICEABLE_CHANGE_DB = 0.5

/**
 * Resultado del test: la curva final, la comparación con y sin ecualizar (con el volumen igualado,
 * para que la diferencia sea solo de tono) y el nombre del preset antes de exportar.
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
            FrequencyChart(
                gainsDb = preset.bandGainsDb,
                contentDescription = stringResource(R.string.finder_result_chart),
                frequenciesHz = preset.bandFrequenciesHz,
                overlayDb = state.sounding?.gainsDb,
            )
        }
        DetailText(stringResource(R.string.finder_chart_legend))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            TerminalChip(
                label = stringResource(R.string.finder_compare_with),
                selected = !state.bypass,
                onClick = { onBypassChange(false) },
            )
            TerminalChip(
                label = stringResource(R.string.finder_compare_without),
                selected = state.bypass,
                onClick = { onBypassChange(true) },
            )
        }
        SoundingStatus(state)
        DifferenceSection(state)
        TerminalTextField(
            value = state.presetName,
            onValueChange = onPresetNameChange,
            label = stringResource(R.string.finder_preset_label),
            placeholder = suggestedPresetName(state),
        )
        ChangesSummary(state)
    }
}

/** Qué está sonando y cómo lo sabe la app: si no se pudo confirmar, se dice en rojo. */
@Composable
private fun SoundingStatus(state: FinderUiState) {
    val current = stringResource(if (state.bypass) R.string.finder_compare_without else R.string.finder_compare_with)
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
    TerminalSection(stringResource(R.string.finder_section_sounding)) {
        CodeLines(listOf(current))
        DetailText(detail, color = if (failed) MaterialTheme.colorScheme.error else Color.Unspecified)
    }
}

/** La diferencia en números, con cada banda a la mano para quien quiera revisarla. */
@Composable
private fun DifferenceSection(state: FinderUiState) {
    val preset = state.finder.result()
    val difference = equalizerDifference(preset)
    var showBands by rememberSaveable { mutableStateOf(false) }
    TerminalSection(stringResource(R.string.finder_section_difference)) {
        CodeLines(
            listOf(
                stringResource(R.string.finder_difference_bands, difference.changedBands, preset.bandGainsDb.size),
                stringResource(
                    R.string.finder_difference_strongest,
                    formatDecibels(difference.strongestDb),
                    formatHertz(difference.strongestHz),
                ),
            ),
        )
        if (difference.isSubtle) DetailText(stringResource(R.string.finder_difference_subtle))
        TerminalChip(
            label = stringResource(R.string.finder_difference_toggle),
            selected = showBands,
            onClick = { showBands = !showBands },
        )
        if (showBands) {
            val overlay = state.sounding?.gainsDb
            CodeLines(
                preset.bandFrequenciesHz.indices.map { band ->
                    val now = overlay?.getOrNull(band)?.let(::formatDecibels)
                        ?: stringResource(R.string.finder_difference_unmeasured)
                    stringResource(
                        R.string.finder_difference_band,
                        formatHertz(preset.bandFrequenciesHz[band]),
                        formatDecibels(preset.bandGainsDb[band]),
                        now,
                    )
                },
            )
        }
    }
}

/** Lo que cambió, en palabras: quien no sabe de audio no tiene por qué leer la gráfica. */
@Composable
private fun ChangesSummary(state: FinderUiState) {
    val lines = state.finder.valuesDb.entries.sortedBy { it.key.ordinal }.map { (dimension, valueDb) ->
        val texts = dimension.texts()
        val change = when {
            valueDb <= -NOTICEABLE_CHANGE_DB -> stringResource(texts.lower)
            valueDb >= NOTICEABLE_CHANGE_DB -> stringResource(texts.upper)
            else -> stringResource(R.string.finder_summary_same)
        }
        stringResource(R.string.finder_summary_line, stringResource(texts.title), change)
    }
    TerminalSection(stringResource(R.string.finder_section_changes)) {
        CodeLines(lines)
        state.finder.doubts.sortedBy { it.ordinal }.forEach { dimension ->
            DetailText(stringResource(R.string.finder_summary_doubt, stringResource(dimension.texts().title)))
        }
    }
}
