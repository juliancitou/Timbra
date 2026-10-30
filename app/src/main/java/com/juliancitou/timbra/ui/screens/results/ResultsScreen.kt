package com.juliancitou.timbra.ui.screens.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.screens.SampleData
import com.juliancitou.timbra.ui.theme.BulletList
import com.juliancitou.timbra.ui.theme.FrequencyChart
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing

@Composable
fun ResultsScreen(onAdjustTaste: () -> Unit, onBack: () -> Unit) {
    GlassScreen(
        title = stringResource(R.string.results_title),
        lead = stringResource(R.string.results_lead),
        onBack = onBack,
        actions = {
            GlassButton(
                text = stringResource(R.string.action_adjust_taste),
                onClick = onAdjustTaste,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassCard {
            FrequencyChart(
                gainsDb = SampleData.curveDb,
                contentDescription = stringResource(R.string.results_chart_description),
                estimatedBandIndices = SampleData.estimatedBandIndices,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                GlassChip(label = stringResource(R.string.chip_reliable), selected = true)
                GlassChip(label = stringResource(R.string.chip_estimated))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            SectionLabel(stringResource(R.string.results_section_findings))
            BulletList(
                listOf(
                    stringResource(R.string.results_finding_sibilance),
                    stringResource(R.string.results_finding_mud),
                ),
            )
        }
    }
}
