package com.juliancitou.timbra.ui.screens.finder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.measure.BLIND_OPTIONS
import com.juliancitou.timbra.measure.BLIND_TRIALS
import com.juliancitou.timbra.measure.BlindVerdict
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalScreen

/** Dos opciones sin nombre: una es la ecualización y otra el original. Al final se dice cuál gustó más. */
@Composable
fun BlindTestScreen(
    state: FinderUiState,
    onListen: (Int) -> Unit,
    onChoose: () -> Unit,
    onTogglePlayback: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val test = state.blindTest ?: return
    if (test.isFinished) {
        TerminalScreen(
            title = stringResource(R.string.blind_title),
            lead = stringResource(
                when (test.verdict) {
                    BlindVerdict.PrefersEqualization -> R.string.blind_verdict_equalization
                    BlindVerdict.PrefersOriginal -> R.string.blind_verdict_original
                    BlindVerdict.NoClearPreference -> R.string.blind_verdict_none
                },
            ),
            onBack = onBack,
            actions = {
                TerminalButton(
                    text = stringResource(R.string.action_ready),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            DetailText(stringResource(R.string.blind_score, test.choseEqualization, BLIND_TRIALS))
            DetailText(stringResource(R.string.blind_caveat))
        }
        return
    }
    TerminalScreen(
        title = stringResource(R.string.blind_title),
        lead = stringResource(R.string.blind_lead),
        onBack = onBack,
        actions = {
            TerminalButton(
                text = stringResource(R.string.blind_choose),
                onClick = onChoose,
                enabled = state.blindOption != null,
                modifier = Modifier.fillMaxWidth(),
            )
            TerminalButton(
                text = stringResource(state.playbackLabelRes()),
                onClick = onTogglePlayback,
                style = TerminalButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        DetailText(stringResource(R.string.blind_progress, test.trial + 1, BLIND_TRIALS))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            repeat(BLIND_OPTIONS) { option ->
                TerminalChip(
                    label = stringResource(R.string.blind_option, option + 1),
                    selected = state.blindOption == option,
                    onClick = { onListen(option) },
                )
            }
        }
    }
}
