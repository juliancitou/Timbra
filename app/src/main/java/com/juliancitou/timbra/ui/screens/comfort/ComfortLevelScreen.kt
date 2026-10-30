package com.juliancitou.timbra.ui.screens.comfort

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassScreen

private const val SAMPLE_VOLUME_PERCENT = 40

@Composable
fun ComfortLevelScreen(onReady: () -> Unit, onBack: () -> Unit) {
    var playing by rememberSaveable { mutableStateOf(false) }

    GlassScreen(
        title = stringResource(R.string.comfort_title),
        subtitle = stringResource(R.string.comfort_subtitle),
        onBack = onBack,
        actions = {
            GlassButton(
                text = stringResource(if (playing) R.string.action_pause else R.string.action_play),
                onClick = { playing = !playing },
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.action_ready),
                onClick = onReady,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassCard {
            GlassChip(label = stringResource(R.string.comfort_volume, SAMPLE_VOLUME_PERCENT))
            Text(
                text = stringResource(R.string.comfort_safety),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.sample_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
