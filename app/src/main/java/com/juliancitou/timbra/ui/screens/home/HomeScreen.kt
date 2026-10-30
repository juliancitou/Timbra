package com.juliancitou.timbra.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.IconMotion
import com.juliancitou.timbra.ui.theme.SETTINGS_TRANSFORM_KEY
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalIconButton
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.containerTransform

/** Las recomendaciones continúan la numeración de los pasos, como un solo bloque de código. */
private const val HOW_IT_WORKS_STEPS = 3

@Composable
fun HomeScreen(
    onCreateEqualization: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TerminalScreen(
        title = stringResource(R.string.app_name),
        lead = stringResource(R.string.home_lead),
        showCursor = true,
        headerAction = {
            TerminalIconButton(
                iconRes = R.drawable.ic_settings,
                contentDescription = stringResource(R.string.action_settings),
                onClick = onOpenSettings,
                modifier = Modifier.containerTransform(SETTINGS_TRANSFORM_KEY),
                motion = IconMotion.Spin,
            )
        },
        actions = {
            TerminalButton(
                text = stringResource(R.string.action_create_equalization),
                onClick = onCreateEqualization,
                modifier = Modifier.fillMaxWidth(),
            )
            TerminalButton(
                text = stringResource(R.string.action_my_measurements),
                onClick = onOpenHistory,
                style = TerminalButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            SectionLabel(stringResource(R.string.home_section_how))
            CodeLines(
                listOf(
                    stringResource(R.string.home_step_listen),
                    stringResource(R.string.home_step_match),
                    stringResource(R.string.home_step_export),
                ),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            SectionLabel(stringResource(R.string.home_section_tips))
            CodeLines(
                listOf(
                    stringResource(R.string.home_tip_time),
                    stringResource(R.string.home_tip_quiet),
                    stringResource(R.string.home_tip_volume),
                ),
                firstLine = HOW_IT_WORKS_STEPS + 1,
            )
        }
    }
}
