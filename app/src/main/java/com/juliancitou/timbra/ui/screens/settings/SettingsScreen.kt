package com.juliancitou.timbra.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.BuildConfig
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing

@Composable
fun SettingsScreen(
    blurEnabled: Boolean,
    onBlurEnabledChange: (Boolean) -> Unit,
    onOpenAudioTest: () -> Unit,
    onBack: () -> Unit,
) {
    GlassScreen(title = stringResource(R.string.settings_title), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            SectionLabel(stringResource(R.string.settings_section_blur))
            DetailText(stringResource(R.string.settings_blur_body))
            GlassChip(
                label = stringResource(if (blurEnabled) R.string.settings_blur_on else R.string.settings_blur_off),
                selected = blurEnabled,
                onClick = { onBlurEnabledChange(!blurEnabled) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            SectionLabel(stringResource(R.string.settings_section_audio_test))
            DetailText(stringResource(R.string.settings_audio_test_body))
            GlassButton(
                text = stringResource(R.string.action_audio_test),
                onClick = onOpenAudioTest,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            SectionLabel(stringResource(R.string.settings_section_about))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            DetailText(stringResource(R.string.settings_font_credit))
        }
    }
}
