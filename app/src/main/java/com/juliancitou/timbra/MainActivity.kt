package com.juliancitou.timbra

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.juliancitou.timbra.ui.theme.FrequencyChart
import com.juliancitou.timbra.ui.theme.GlassBackground
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassChipTone
import com.juliancitou.timbra.ui.theme.GlassSlider
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TimbraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            TimbraTheme {
                TimbraApp()
            }
        }
    }
}

private val SampleCurveDb = listOf(
    -1.5f, -1.5f, -1f, -0.5f, 0f, 0f, -1f, -2f, -2.5f, -2.5f, -2f, -1f, -0.5f,
    0f, 0.5f, 1f, 1.5f, 1f, 0f, -1.5f, -3.5f, -4.5f, -3.5f, -2.5f, -2.5f,
)

@Composable
fun TimbraApp() {
    var gainDb by remember { mutableFloatStateOf(0f) }
    var reliableSelected by remember { mutableStateOf(true) }
    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            GlassCard {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlassCard {
                Text(
                    text = stringResource(R.string.chart_sample_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FrequencyChart(
                    gainsDb = SampleCurveDb,
                    contentDescription = stringResource(R.string.chart_sample_description),
                    estimatedBandIndices = setOf(0, 1, 2),
                    highlightedBandIndex = 21,
                )
            }
            GlassCard {
                GlassSlider(
                    valueDb = gainDb,
                    onValueChange = { gainDb = it },
                    label = stringResource(R.string.slider_band_gain),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    GlassChip(
                        label = stringResource(R.string.chip_reliable),
                        selected = reliableSelected,
                        onClick = { reliableSelected = true },
                    )
                    GlassChip(
                        label = stringResource(R.string.chip_estimated),
                        selected = !reliableSelected,
                        onClick = { reliableSelected = false },
                    )
                    GlassChip(
                        label = stringResource(R.string.chip_volume_high),
                        tone = GlassChipTone.Warning,
                    )
                }
            }
            GlassButton(
                text = stringResource(R.string.action_create_equalization),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.action_my_measurements),
                onClick = {},
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1026)
@Composable
private fun TimbraAppPreview() {
    TimbraTheme {
        TimbraApp()
    }
}
