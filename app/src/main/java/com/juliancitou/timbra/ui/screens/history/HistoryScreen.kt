package com.juliancitou.timbra.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.screens.SampleData
import com.juliancitou.timbra.ui.screens.SampleMeasurement
import com.juliancitou.timbra.ui.theme.Hairline
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalScreen
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(onOpenMeasurement: () -> Unit, onBack: () -> Unit) {
    TerminalScreen(
        title = stringResource(R.string.history_title),
        lead = stringResource(R.string.history_lead),
        onBack = onBack,
    ) {
        Column {
            Hairline()
            SampleData.measurements.forEach { measurement ->
                MeasurementRow(measurement = measurement, onClick = onOpenMeasurement)
                Hairline()
            }
        }
    }
}

@Composable
private fun MeasurementRow(measurement: SampleMeasurement, onClick: () -> Unit) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val codec = stringResource(if (measurement.usesLdac) R.string.history_codec_ldac else R.string.history_codec_aac)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        Text(
            text = measurement.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.history_entry_date, measurement.date.format(dateFormatter), codec),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
