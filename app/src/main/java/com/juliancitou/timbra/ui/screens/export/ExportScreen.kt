package com.juliancitou.timbra.ui.screens.export

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassScreen

@Composable
fun ExportScreen(onGoHome: () -> Unit, onBack: () -> Unit) {
    GlassScreen(
        title = stringResource(R.string.export_title),
        subtitle = stringResource(R.string.export_subtitle),
        onBack = onBack,
        actions = {
            GlassButton(
                text = stringResource(R.string.action_save_file),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.action_share),
                onClick = {},
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.action_go_home),
                onClick = onGoHome,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassCard {
            Text(
                text = stringResource(R.string.export_preset_name),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        GlassCard {
            Text(
                text = stringResource(R.string.export_steps_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.export_steps_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
