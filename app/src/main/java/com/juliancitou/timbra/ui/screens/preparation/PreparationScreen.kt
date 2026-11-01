package com.juliancitou.timbra.ui.screens.preparation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.PowerampState
import com.juliancitou.timbra.audio.QUIET_ROOM_MAX_DBFS
import com.juliancitou.timbra.audio.measureAmbientNoiseDbfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.juliancitou.timbra.audio.DetectedOutput
import com.juliancitou.timbra.audio.OutputDetector
import com.juliancitou.timbra.export.BandLayout
import com.juliancitou.timbra.measure.OutputKind
import com.juliancitou.timbra.ui.screens.finder.labelRes
import com.juliancitou.timbra.ui.theme.CheckRow
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection
import com.juliancitou.timbra.ui.theme.TerminalSlider
import com.juliancitou.timbra.ui.theme.TerminalSliderSize
import com.juliancitou.timbra.ui.theme.TerminalTextField
import com.juliancitou.timbra.ui.theme.formatHertz
import com.juliancitou.timbra.ui.theme.staggeredEntrance
import kotlin.math.roundToInt

private const val LAST_FREQUENCY_STEP_HZ = 100

private fun checklistFor(output: OutputKind): List<Int> = when (output) {
    OutputKind.Bluetooth -> listOf(
        R.string.preparation_noise_cancelling,
        R.string.preparation_poweramp_off,
        R.string.preparation_phone_equalizer,
        R.string.preparation_quiet_place,
    )
    OutputKind.Wired -> listOf(
        R.string.preparation_poweramp_off,
        R.string.preparation_phone_equalizer,
        R.string.preparation_quiet_place,
    )
    OutputKind.Speaker -> listOf(
        R.string.preparation_speaker_position,
        R.string.preparation_speaker_uncovered,
        R.string.preparation_poweramp_off,
        R.string.preparation_phone_equalizer,
        R.string.preparation_quiet_place,
    )
}

/**
 * Primer paso: dónde se va a escuchar, cómo se llaman los audífonos y qué bandas usa Poweramp. La
 * salida sigue a la detectada hasta que el usuario elige otra. Un desacuerdo solo avisa y no bloquea,
 * porque hay accesorios que Android podría no reportar bien.
 */
@Composable
fun PreparationScreen(
    output: OutputKind,
    onOutputChange: (OutputKind) -> Unit,
    bandLayout: BandLayout,
    bandsFromPoweramp: Boolean,
    onBandLayoutChange: (BandLayout) -> Unit,
    headphonesName: String,
    onHeadphonesNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val detected by produceState<DetectedOutput?>(null, context) {
        OutputDetector(context.applicationContext).changes().collect { value = it }
    }
    var chosenByUser by rememberSaveable { mutableStateOf(false) }
    var filledName by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(detected) {
        val current = detected ?: return@LaunchedEffect
        if (!chosenByUser) onOutputChange(current.kind)
        // Un nombre escrito a mano nunca se reemplaza; uno detectado sí, al cambiar de accesorio.
        if (headphonesName.isBlank() || headphonesName == filledName) {
            filledName = current.name.orEmpty()
            onHeadphonesNameChange(filledName)
        }
    }

    TerminalScreen(
        title = stringResource(R.string.preparation_title),
        lead = stringResource(R.string.preparation_lead),
        onBack = onBack,
        actions = {
            TerminalButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        OutputSection(
            output = output,
            detected = detected,
            onSelect = {
                chosenByUser = true
                onOutputChange(it)
            },
        )
        if (output != OutputKind.Speaker) {
            TerminalTextField(
                value = headphonesName,
                onValueChange = onHeadphonesNameChange,
                label = stringResource(R.string.preparation_headphones_label),
                placeholder = stringResource(R.string.preparation_headphones_placeholder),
            )
        }
        BandsSection(bandLayout, bandsFromPoweramp, onBandLayoutChange)
    }
}

/** Segundo paso: lo que hay que dejar listo antes de que suene algo. */
@Composable
fun ChecklistScreen(
    output: OutputKind,
    poweramp: PowerampState,
    onTurnOffPoweramp: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val checklist = checklistFor(output)
    // Un bit por punto de la lista: sobrevive a rotaciones y se reinicia si cambia la salida.
    var checkedMask by rememberSaveable(output) { mutableIntStateOf(0) }
    val context = LocalContext.current
    var noiseDbfs by remember { mutableStateOf<Float?>(null) }
    var measuring by remember { mutableStateOf(false) }
    var measureRequests by remember { mutableIntStateOf(0) }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) measureRequests++
    }
    val canUseMicrophone = {
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(measureRequests) {
        if (!canUseMicrophone()) return@LaunchedEffect
        measuring = true
        noiseDbfs = withContext(Dispatchers.IO) { measureAmbientNoiseDbfs() }
        measuring = false
    }
    fun markChecklist(labelRes: Int, checked: Boolean) {
        val bit = 1 shl checklist.indexOf(labelRes)
        checkedMask = if (checked) checkedMask or bit else checkedMask and bit.inv()
    }
    LaunchedEffect(poweramp) {
        when (poweramp) {
            PowerampState.Off, PowerampState.NotInstalled -> markChecklist(R.string.preparation_poweramp_off, true)
            PowerampState.On -> markChecklist(R.string.preparation_poweramp_off, false)
            PowerampState.Unknown -> Unit
        }
    }
    LaunchedEffect(noiseDbfs) {
        noiseDbfs?.let { markChecklist(R.string.preparation_quiet_place, it <= QUIET_ROOM_MAX_DBFS) }
    }

    TerminalScreen(
        title = stringResource(R.string.checklist_title),
        lead = stringResource(R.string.checklist_lead),
        onBack = onBack,
        actions = {
            TerminalButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                enabled = checkedMask == (1 shl checklist.size) - 1,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            checklist.forEachIndexed { index, labelRes ->
                val bit = 1 shl index
                CheckRow(
                    label = stringResource(labelRes),
                    checked = checkedMask and bit != 0,
                    onToggle = { checkedMask = checkedMask xor bit },
                    modifier = Modifier.staggeredEntrance(index),
                )
                when (labelRes) {
                    R.string.preparation_poweramp_off -> PowerampStatus(poweramp, onTurnOffPoweramp)
                    R.string.preparation_quiet_place -> NoiseStatus(
                        noiseDbfs = noiseDbfs,
                        measuring = measuring,
                        onMeasure = {
                            if (canUseMicrophone()) {
                                measureRequests++
                            } else {
                                microphone.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    )
                }
            }
        }
        TerminalSection(stringResource(R.string.preparation_section_why)) {
            DetailText(
                stringResource(
                    if (output == OutputKind.Speaker) R.string.preparation_why_speaker else R.string.preparation_why,
                ),
            )
            DetailText(stringResource(R.string.preparation_why_phone_equalizer))
            val context = LocalContext.current
            TerminalButton(
                text = stringResource(R.string.preparation_open_sound_settings),
                onClick = { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) },
                style = TerminalButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PowerampStatus(state: PowerampState, onTurnOff: () -> Unit) {
    DetailText(
        stringResource(
            when (state) {
                PowerampState.NotInstalled -> R.string.preparation_poweramp_missing
                PowerampState.Off -> R.string.preparation_poweramp_detected_off
                PowerampState.On -> R.string.preparation_poweramp_detected_on
                PowerampState.Unknown -> R.string.preparation_poweramp_unknown
            },
        ),
    )
    if (state == PowerampState.On || state == PowerampState.Unknown) {
        TerminalChip(
            label = stringResource(R.string.preparation_poweramp_turn_off),
            selected = false,
            onClick = onTurnOff,
        )
    }
}

@Composable
private fun NoiseStatus(noiseDbfs: Float?, measuring: Boolean, onMeasure: () -> Unit) {
    when {
        measuring -> DetailText(stringResource(R.string.preparation_noise_measuring))
        noiseDbfs != null && noiseDbfs <= QUIET_ROOM_MAX_DBFS ->
            DetailText(stringResource(R.string.preparation_noise_quiet))
        noiseDbfs != null -> DetailText(stringResource(R.string.preparation_noise_loud))
    }
    if (!measuring && (noiseDbfs == null || noiseDbfs > QUIET_ROOM_MAX_DBFS)) {
        TerminalChip(
            label = stringResource(R.string.preparation_noise_measure),
            selected = false,
            onClick = onMeasure,
        )
    }
}

@Composable
private fun OutputSection(output: OutputKind, detected: DetectedOutput?, onSelect: (OutputKind) -> Unit) {
    TerminalSection(stringResource(R.string.preparation_section_output)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            OutputKind.entries.forEach { kind ->
                TerminalChip(
                    label = stringResource(kind.labelRes),
                    selected = kind == output,
                    onClick = { onSelect(kind) },
                )
            }
        }
        detected?.let { current ->
            if (current.kind == output) {
                DetailText(detectedText(current))
            } else {
                DetailText(stringResource(output.mismatchRes()), color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun detectedText(detected: DetectedOutput): String = when (detected.kind) {
    OutputKind.Bluetooth -> detected.name?.let { stringResource(R.string.preparation_detected_bluetooth_named, it) }
        ?: stringResource(R.string.preparation_detected_bluetooth)
    OutputKind.Wired -> stringResource(R.string.preparation_detected_wired)
    OutputKind.Speaker -> stringResource(R.string.preparation_detected_speaker)
}

@StringRes
private fun OutputKind.mismatchRes(): Int = when (this) {
    OutputKind.Bluetooth -> R.string.preparation_mismatch_bluetooth
    OutputKind.Wired -> R.string.preparation_mismatch_wired
    OutputKind.Speaker -> R.string.preparation_mismatch_speaker
}

/** "25 bandas, de 20 Hz a 20 kHz". */
@Composable
fun bandSummary(layout: BandLayout): String = stringResource(
    R.string.bands_summary,
    layout.count,
    formatHertz(layout.firstHz),
    formatHertz(layout.lastHz),
)

@Composable
private fun BandsSection(layout: BandLayout, fromPoweramp: Boolean, onChange: (BandLayout) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    TerminalSection(stringResource(R.string.preparation_section_bands)) {
        DetailText(stringResource(R.string.preparation_bands_summary, bandSummary(layout)))
        if (fromPoweramp) {
            DetailText(stringResource(R.string.preparation_bands_copied))
            return@TerminalSection
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            TerminalChip(
                label = stringResource(
                    if (editing) R.string.preparation_bands_done else R.string.preparation_bands_change,
                ),
                selected = editing,
                onClick = { editing = !editing },
            )
            if (editing && layout != BandLayout.Default) {
                TerminalChip(
                    label = stringResource(R.string.preparation_bands_reset),
                    onClick = { onChange(BandLayout.Default) },
                )
            }
        }
        if (editing) {
            DetailText(stringResource(R.string.preparation_bands_hint))
            BandsEditor(layout, onChange)
        }
    }
}

/** Los mismos tres valores que pide Poweramp en "bandas personalizadas", con sus mismos límites. */
@Composable
private fun BandsEditor(layout: BandLayout, onChange: (BandLayout) -> Unit) {
    IntegerSlider(
        label = stringResource(R.string.preparation_bands_count),
        value = layout.count,
        range = BandLayout.COUNT_RANGE,
        format = Int::toString,
        onChange = { onChange(layout.copy(count = it)) },
    )
    IntegerSlider(
        label = stringResource(R.string.preparation_bands_first),
        value = layout.firstHz,
        range = BandLayout.FIRST_HZ_RANGE,
        format = ::formatHertz,
        onChange = { onChange(layout.copy(firstHz = it)) },
    )
    IntegerSlider(
        label = stringResource(R.string.preparation_bands_last),
        value = layout.lastHz,
        range = BandLayout.LAST_HZ_RANGE,
        step = LAST_FREQUENCY_STEP_HZ,
        format = ::formatHertz,
        onChange = { onChange(layout.copy(lastHz = it)) },
    )
}

@Composable
private fun IntegerSlider(
    label: String,
    value: Int,
    range: IntRange,
    format: (Int) -> String,
    onChange: (Int) -> Unit,
    step: Int = 1,
) {
    fun snap(raw: Float) = range.first + ((raw - range.first) / step).roundToInt() * step
    TerminalSlider(
        valueDb = value.toFloat(),
        onValueChange = { raw -> snap(raw).takeIf { it != value }?.let(onChange) },
        label = label,
        valueRange = range.first.toFloat()..range.last.toFloat(),
        stepDb = step.toFloat(),
        size = TerminalSliderSize.Compact,
        formatValue = { format(it.roundToInt()) },
    )
}
