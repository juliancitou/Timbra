package com.juliancitou.timbra.ui.screens.finder

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.measure.FinderDimension
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.OptionNodes
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonRow
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalHoldChip
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection

/** Textos de cada zona: título, pregunta, qué cambia y qué significa cada extremo. */
internal data class DimensionTexts(
    @StringRes val title: Int,
    @StringRes val question: Int,
    @StringRes val hint: Int,
    @StringRes val lower: Int,
    @StringRes val upper: Int,
)

internal fun FinderDimension.texts() = when (this) {
    FinderDimension.Balance -> DimensionTexts(
        R.string.finder_balance_title, R.string.finder_balance_question, R.string.finder_balance_hint,
        R.string.finder_balance_lower, R.string.finder_balance_upper,
    )
    FinderDimension.Bass -> DimensionTexts(
        R.string.finder_bass_title, R.string.finder_bass_question, R.string.finder_bass_hint,
        R.string.finder_bass_lower, R.string.finder_bass_upper,
    )
    FinderDimension.Clarity -> DimensionTexts(
        R.string.finder_clarity_title, R.string.finder_clarity_question, R.string.finder_clarity_hint,
        R.string.finder_clarity_lower, R.string.finder_clarity_upper,
    )
    FinderDimension.Voice -> DimensionTexts(
        R.string.finder_voice_title, R.string.finder_voice_question, R.string.finder_voice_hint,
        R.string.finder_voice_lower, R.string.finder_voice_upper,
    )
    FinderDimension.Sibilance -> DimensionTexts(
        R.string.finder_sibilance_title, R.string.finder_sibilance_question, R.string.finder_sibilance_hint,
        R.string.finder_sibilance_lower, R.string.finder_sibilance_upper,
    )
    FinderDimension.Air -> DimensionTexts(
        R.string.finder_air_title, R.string.finder_air_question, R.string.finder_air_hint,
        R.string.finder_air_lower, R.string.finder_air_upper,
    )
}

/**
 * Una ronda del test: la pregunta, en qué fijarse, los cinco nodos para escuchar y los botones para
 * reproducir, elegir o dejarlo como estaba. La flecha sale a la pantalla anterior; "Regresar" vuelve a la
 * ronda anterior tal como estaba antes de responderla. Los nodos no muestran dB: quien no sabe de audio
 * elige de oído.
 */
@Composable
fun FinderRoundScreen(
    state: FinderUiState,
    onAudition: (Int) -> Unit,
    onChoose: () -> Unit,
    onKeepCurrent: () -> Unit,
    onHoldCurrent: (Boolean) -> Unit,
    onTogglePlayback: () -> Unit,
    onPreviousRound: () -> Unit,
    onExit: () -> Unit,
) {
    val finder = state.finder
    val round = finder.round
    val texts = round.dimension.texts()
    val options = finder.options()
    val currentIndex = finder.currentOptionIndex
    BackHandler(onBack = onExit)

    TerminalScreen(
        title = stringResource(texts.title),
        lead = stringResource(texts.question),
        onBack = onExit,
        actions = {
            TerminalButtonRow {
                TerminalButton(
                    text = stringResource(state.playbackLabelRes()),
                    onClick = onTogglePlayback,
                    style = TerminalButtonStyle.Secondary,
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                TerminalButton(
                    text = stringResource(R.string.action_choose),
                    onClick = onChoose,
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            TerminalButtonRow {
                TerminalButton(
                    text = stringResource(R.string.action_previous_round),
                    onClick = onPreviousRound,
                    style = TerminalButtonStyle.Secondary,
                    enabled = !state.preparing && finder.previous != null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                TerminalButton(
                    text = stringResource(R.string.action_none),
                    onClick = onKeepCurrent,
                    style = TerminalButtonStyle.Secondary,
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        },
    ) {
        val progress = when {
            round.recheck -> R.string.finder_progress_recheck
            round.fine -> R.string.finder_progress_fine
            else -> R.string.finder_progress_coarse
        }
        TerminalSection(stringResource(progress, finder.roundIndex + 1, finder.rounds.size)) {
            DetailText(
                stringResource(
                    when {
                        state.preparing -> R.string.finder_preparing
                        round.recheck -> R.string.finder_recheck_hint
                        round.fine -> R.string.finder_fine_hint
                        else -> texts.hint
                    },
                ),
            )
        }
        val currentLabel = stringResource(R.string.finder_node_current)
        val labels = options.indices.map { index ->
            if (index == currentIndex && !round.recheck) currentLabel else ('A' + index).toString()
        }
        val descriptions = options.indices.map { index ->
            val position = stringResource(R.string.finder_node_description, index + 1, options.size)
            if (index == currentIndex && !round.recheck) "$position, $currentLabel" else position
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.large)) {
            OptionNodes(
                values = labels,
                selectedIndex = state.auditionIndex,
                onSelect = onAudition,
                lowerLabel = stringResource(texts.lower),
                upperLabel = stringResource(texts.upper),
                nodeDescription = { descriptions[it] },
                enabled = !state.preparing,
                playing = state.isPlaying,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!round.recheck) {
                TerminalHoldChip(stringResource(R.string.finder_hold_current), onHoldCurrent)
            }
        }
        if (finder.roundIndex == 0) {
            TerminalSection(stringResource(R.string.finder_section_how)) {
                CodeLines(
                    listOf(
                        stringResource(R.string.finder_step_listen),
                        stringResource(R.string.finder_step_choose),
                        stringResource(R.string.finder_step_none),
                    ),
                )
            }
        }
    }
}
