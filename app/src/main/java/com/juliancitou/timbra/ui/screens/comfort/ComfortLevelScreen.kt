package com.juliancitou.timbra.ui.screens.comfort

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.music.Genre
import com.juliancitou.timbra.ui.screens.finder.FinderUiState
import com.juliancitou.timbra.ui.screens.label
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.PlayingIndicator
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalScreen

/** Elegir la música del test y llegar a un volumen cómodo antes de empezar las rondas. */
@Composable
fun ComfortLevelScreen(
    state: FinderUiState,
    onPrepareMusic: () -> Unit,
    onSelectGenre: (Genre) -> Unit,
    onTogglePlayback: () -> Unit,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onPrepareMusic() }

    TerminalScreen(
        title = stringResource(R.string.comfort_title),
        lead = stringResource(R.string.comfort_lead),
        onBack = onBack,
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
                    text = stringResource(R.string.action_ready),
                    onClick = onReady,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                SectionLabel(stringResource(R.string.comfort_section_genre))
                PlayingIndicator(playing = state.isPlaying)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Genre.entries.forEach { genre ->
                    TerminalChip(
                        label = stringResource(genre.label()),
                        selected = state.genre == genre,
                        onClick = { onSelectGenre(genre) },
                    )
                }
            }
            if (state.preparing) DetailText(stringResource(R.string.audio_status_preparing))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            SectionLabel(stringResource(R.string.comfort_section_safety))
            DetailText(stringResource(R.string.comfort_safety))
        }
    }
}
