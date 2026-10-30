package com.juliancitou.timbra.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.SETTINGS_TRANSFORM_KEY
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.containerTransform

@Composable
fun SettingsScreen(
    onOpenAudioTest: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
) {
    GlassScreen(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        modifier = Modifier.containerTransform(SETTINGS_TRANSFORM_KEY),
    ) {
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
        MoreInfoRow(onClick = onOpenAbout)
    }
}

/** Entrada discreta a créditos y contacto: una sola línea al final de Ajustes. */
@Composable
private fun MoreInfoRow(onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.settings_more_info) + "  ›",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .wrapContentHeight(),
    )
}
