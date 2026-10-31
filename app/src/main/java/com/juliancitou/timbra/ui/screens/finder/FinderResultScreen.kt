package com.juliancitou.timbra.ui.screens.finder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.FrequencyChart
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalPanel
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalTextField

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
                    text = stringResource(if (state.isPlaying) R.string.action_pause else R.string.action_play),
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
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
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
        TerminalTextField(
            value = state.presetName,
            onValueChange = onPresetNameChange,
            label = stringResource(R.string.finder_preset_label),
            placeholder = suggestedPresetName(state),
        )
    }
}
