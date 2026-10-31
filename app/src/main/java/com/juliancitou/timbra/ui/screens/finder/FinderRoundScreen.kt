package com.juliancitou.timbra.ui.screens.finder

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalScreen

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
 * reproducir, elegir o dejarlo como estaba. Atrás vuelve a la ronda anterior; en la primera, a la
 * pantalla anterior. Los nodos no muestran dB: quien no sabe de audio elige de oído.
 */
@Composable
fun FinderRoundScreen(
    state: FinderUiState,
    onAudition: (Int) -> Unit,
    onChoose: () -> Unit,
    onKeepCurrent: () -> Unit,
    onTogglePlayback: () -> Unit,
    onPreviousRound: () -> Boolean,
    onExit: () -> Unit,
) {
    val finder = state.finder
    val round = finder.round
    val texts = round.dimension.texts()
    val options = finder.options()
    val currentIndex = finder.currentOptionIndex
    val goBack = { if (!onPreviousRound()) onExit() }
    BackHandler(onBack = goBack)

    TerminalScreen(
        title = stringResource(texts.title),
        lead = stringResource(texts.question),
        onBack = goBack,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                TerminalButton(
                    text = stringResource(if (state.isPlaying) R.string.action_pause else R.string.action_play),
                    onClick = onTogglePlayback,
                    style = TerminalButtonStyle.Secondary,
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f),
                )
                TerminalButton(
                    text = stringResource(R.string.action_choose),
                    onClick = onChoose,
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f),
                )
            }
            TerminalButton(
                text = stringResource(R.string.action_none),
                onClick = onKeepCurrent,
                style = TerminalButtonStyle.Secondary,
                enabled = !state.preparing,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            val progress = if (round.fine) R.string.finder_progress_fine else R.string.finder_progress_coarse
            SectionLabel(stringResource(progress, finder.roundIndex + 1, finder.rounds.size))
            CodeLines(
                listOf(
                    stringResource(R.string.finder_step_listen),
                    stringResource(R.string.finder_step_choose),
                    stringResource(R.string.finder_step_none),
                ),
            )
        }
        val currentLabel = stringResource(R.string.finder_node_current)
        val labels = options.indices.map { index ->
            if (index == currentIndex) currentLabel else ('A' + index).toString()
        }
        val descriptions = options.indices.map { index ->
            val position = stringResource(R.string.finder_node_description, index + 1, options.size)
            if (index == currentIndex) "$position, $currentLabel" else position
        }
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
        DetailText(
            stringResource(
                when {
                    state.preparing -> R.string.finder_preparing
                    round.fine -> R.string.finder_fine_hint
                    else -> texts.hint
                },
            ),
        )
    }
}
