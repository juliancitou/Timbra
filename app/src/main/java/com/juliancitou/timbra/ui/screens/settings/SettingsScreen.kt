package com.juliancitou.timbra.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.BuildConfig
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassScreen

@Composable
fun SettingsScreen(blurEnabled: Boolean, onBlurEnabledChange: (Boolean) -> Unit, onBack: () -> Unit) {
    GlassScreen(title = stringResource(R.string.settings_title), onBack = onBack) {
        GlassCard {
            Text(
                text = stringResource(R.string.settings_blur_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_blur_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GlassChip(
                label = stringResource(if (blurEnabled) R.string.settings_blur_on else R.string.settings_blur_off),
                selected = blurEnabled,
                onClick = { onBlurEnabledChange(!blurEnabled) },
            )
        }
        GlassCard {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}
