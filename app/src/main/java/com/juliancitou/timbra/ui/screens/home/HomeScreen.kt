package com.juliancitou.timbra.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.BulletList
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassIconButton
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.StepList

@Composable
fun HomeScreen(
    onCreateEqualization: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    GlassScreen(
        title = stringResource(R.string.app_name),
        lead = stringResource(R.string.home_lead),
        headerAction = {
            GlassIconButton(
                iconRes = R.drawable.ic_settings,
                contentDescription = stringResource(R.string.action_settings),
                onClick = onOpenSettings,
            )
        },
        actions = {
            GlassButton(
                text = stringResource(R.string.action_create_equalization),
                onClick = onCreateEqualization,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.action_my_measurements),
                onClick = onOpenHistory,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            SectionLabel(stringResource(R.string.home_section_how))
            StepList(
                listOf(
                    stringResource(R.string.home_step_listen),
                    stringResource(R.string.home_step_match),
                    stringResource(R.string.home_step_export),
                ),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            SectionLabel(stringResource(R.string.home_section_tips))
            BulletList(
                listOf(
                    stringResource(R.string.home_tip_time),
                    stringResource(R.string.home_tip_quiet),
                    stringResource(R.string.home_tip_volume),
                ),
            )
        }
    }
}
