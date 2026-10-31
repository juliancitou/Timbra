package com.juliancitou.timbra.ui.screens.comfort

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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

private const val AUDIO_MIME_TYPES = "audio/*"

/** Elegir la música del test y llegar a un volumen cómodo antes de empezar las rondas. */
@Composable
fun ComfortLevelScreen(
    state: FinderUiState,
    onPrepareMusic: () -> Unit,
    onSelectGenre: (Genre) -> Unit,
    onSelectSong: (Uri) -> Unit,
    onTogglePlayback: () -> Unit,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onPrepareMusic() }
    val pickSong = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onSelectSong(uri)
    }

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
                        selected = state.songName == null && state.genre == genre,
                        onClick = { onSelectGenre(genre) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            SectionLabel(stringResource(R.string.comfort_section_song))
            TerminalButton(
                text = stringResource(R.string.action_pick_song),
                onClick = {
                    if (state.isPlaying) onTogglePlayback()
                    pickSong.launch(arrayOf(AUDIO_MIME_TYPES))
                },
                style = TerminalButtonStyle.Secondary,
                enabled = !state.loadingSong,
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                state.loadingSong -> DetailText(stringResource(R.string.comfort_song_loading))
                state.songFailed -> DetailText(
                    stringResource(R.string.comfort_song_failed),
                    color = MaterialTheme.colorScheme.secondary,
                )
                state.songName != null -> DetailText(
                    stringResource(R.string.comfort_song_selected, state.songName),
                    color = MaterialTheme.colorScheme.primary,
                )
                else -> DetailText(stringResource(R.string.comfort_song_hint))
            }
            if (state.preparing && !state.loadingSong) DetailText(stringResource(R.string.audio_status_preparing))
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
