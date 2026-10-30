package com.juliancitou.timbra.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.screens.SampleData
import com.juliancitou.timbra.ui.screens.SampleMeasurement
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassScreen
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(onOpenMeasurement: () -> Unit, onBack: () -> Unit) {
    GlassScreen(
        title = stringResource(R.string.history_title),
        subtitle = stringResource(R.string.history_subtitle),
        onBack = onBack,
    ) {
        SampleData.measurements.forEach { measurement ->
            MeasurementCard(measurement = measurement, onClick = onOpenMeasurement)
        }
    }
}

@Composable
private fun MeasurementCard(measurement: SampleMeasurement, onClick: () -> Unit) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val codec = stringResource(if (measurement.usesLdac) R.string.history_codec_ldac else R.string.history_codec_aac)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(text = measurement.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.history_entry_date, measurement.date.format(dateFormatter), codec),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
