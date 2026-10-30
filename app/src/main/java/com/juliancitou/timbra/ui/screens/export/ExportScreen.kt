package com.juliancitou.timbra.ui.screens.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.StepList

@Composable
fun ExportScreen(onGoHome: () -> Unit, onBack: () -> Unit) {
    GlassScreen(
        title = stringResource(R.string.export_title),
        lead = stringResource(R.string.export_lead),
        onBack = onBack,
        actions = {
            GlassButton(
                text = stringResource(R.string.action_save_file),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                GlassButton(
                    text = stringResource(R.string.action_share),
                    onClick = {},
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(R.string.action_go_home),
                    onClick = onGoHome,
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            SectionLabel(stringResource(R.string.export_section_name))
            Text(
                text = stringResource(R.string.export_preset_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            SectionLabel(stringResource(R.string.export_section_steps))
            StepList(
                listOf(
                    stringResource(R.string.export_step_open),
                    stringResource(R.string.export_step_presets),
                    stringResource(R.string.export_step_select),
                ),
            )
        }
    }
}
