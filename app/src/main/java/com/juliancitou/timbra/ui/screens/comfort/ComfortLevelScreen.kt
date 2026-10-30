package com.juliancitou.timbra.ui.screens.comfort

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing

private const val SAMPLE_VOLUME_PERCENT = 40

@Composable
fun ComfortLevelScreen(onReady: () -> Unit, onBack: () -> Unit) {
    var playing by rememberSaveable { mutableStateOf(false) }

    GlassScreen(
        title = stringResource(R.string.comfort_title),
        lead = stringResource(R.string.comfort_lead),
        onBack = onBack,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                GlassButton(
                    text = stringResource(if (playing) R.string.action_pause else R.string.action_play),
                    onClick = { playing = !playing },
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(R.string.action_ready),
                    onClick = onReady,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        GlassChip(label = stringResource(R.string.comfort_volume, SAMPLE_VOLUME_PERCENT))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            SectionLabel(stringResource(R.string.comfort_section_safety))
            DetailText(stringResource(R.string.comfort_safety))
        }
        DetailText(stringResource(R.string.sample_notice), color = MaterialTheme.colorScheme.secondary)
    }
}
