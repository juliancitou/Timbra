package com.juliancitou.timbra.ui.screens.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.screens.SampleData
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.FrequencyChart
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalPanel
import com.juliancitou.timbra.ui.theme.TerminalScreen

@Composable
fun ResultsScreen(onAdjustTaste: () -> Unit, onBack: () -> Unit) {
    TerminalScreen(
        title = stringResource(R.string.results_title),
        lead = stringResource(R.string.results_lead),
        onBack = onBack,
        actions = {
            TerminalButton(
                text = stringResource(R.string.action_adjust_taste),
                onClick = onAdjustTaste,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        TerminalPanel {
            FrequencyChart(
                gainsDb = SampleData.curveDb,
                contentDescription = stringResource(R.string.results_chart_description),
                estimatedBandIndices = SampleData.estimatedBandIndices,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                TerminalChip(label = stringResource(R.string.chip_reliable), selected = true)
                TerminalChip(label = stringResource(R.string.chip_estimated))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            SectionLabel(stringResource(R.string.results_section_findings))
            CodeLines(
                listOf(
                    stringResource(R.string.results_finding_sibilance),
                    stringResource(R.string.results_finding_mud),
                ),
            )
        }
    }
}
