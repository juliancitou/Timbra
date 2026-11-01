package com.juliancitou.timbra.ui.screens.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.juliancitou.timbra.R
import com.juliancitou.timbra.audio.PowerampIssue
import com.juliancitou.timbra.audio.PowerampSettings
import com.juliancitou.timbra.export.BandLayout
import com.juliancitou.timbra.export.PowerampExporter
import com.juliancitou.timbra.measure.OutputKind
import com.juliancitou.timbra.ui.screens.preparation.bandSummary
import com.juliancitou.timbra.ui.theme.CodeLines
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalButtonStyle
import com.juliancitou.timbra.ui.theme.TerminalChip
import com.juliancitou.timbra.ui.theme.TerminalChipTone
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalSection
import java.io.File

private enum class SaveResult { Saved, Failed }

/** Carpeta dentro de la caché desde la que se comparten los presets (ver `res/xml/file_paths.xml`). */
private const val SHARED_PRESETS_DIRECTORY = "presets"
private const val PRESET_MIME_TYPE = "application/json"

/**
 * Guardar o compartir el preset. Guardar usa el selector de archivos del sistema, así que la app
 * no necesita permisos de almacenamiento; compartir pasa una copia temporal por `FileProvider`.
 */
@Composable
fun ExportScreen(
    presetName: String,
    fileName: String,
    output: OutputKind,
    bandLayout: BandLayout,
    powerampSettings: PowerampSettings?,
    presetImported: Boolean,
    presetInUse: Boolean,
    presetInUseFailed: Boolean,
    onImportPreset: (json: String) -> Boolean,
    onUsePreset: () -> Unit,
    presetJson: () -> String,
    onGoHome: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var saveResult by rememberSaveable { mutableStateOf<SaveResult?>(null) }
    val createDocument = ActivityResultContracts.CreateDocument(PRESET_MIME_TYPE)
    val saveLauncher = rememberLauncherForActivityResult(createDocument) { uri ->
        if (uri != null) saveResult = if (writeTo(context, uri, presetJson())) SaveResult.Saved else SaveResult.Failed
    }
    val shareTitle = stringResource(R.string.export_share_chooser)
    var importFailed by rememberSaveable { mutableStateOf(false) }

    TerminalScreen(
        title = stringResource(R.string.export_title),
        lead = stringResource(R.string.export_lead),
        onBack = onBack,
        actions = {
            if (presetInUse) {
                TerminalButton(
                    text = stringResource(R.string.action_go_home),
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (presetImported) {
                TerminalButton(
                    text = stringResource(R.string.action_use_in_poweramp),
                    onClick = onUsePreset,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalButton(
                    text = stringResource(R.string.action_go_home),
                    onClick = onGoHome,
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TerminalButton(
                    text = stringResource(R.string.action_import_poweramp),
                    onClick = { importFailed = !onImportPreset(presetJson()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalButton(
                    text = stringResource(R.string.action_go_home),
                    onClick = onGoHome,
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.inSection)) {
                TerminalButton(
                    text = stringResource(R.string.action_save_file),
                    onClick = { saveLauncher.launch(fileName) },
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                TerminalButton(
                    text = stringResource(R.string.action_share),
                    onClick = { share(context, fileName, presetJson(), shareTitle) },
                    style = TerminalButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        TerminalSection(stringResource(R.string.export_section_name)) {
            Text(
                text = PowerampExporter.NAME_PREFIX + presetName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (presetInUse) {
            TerminalChip(label = stringResource(R.string.export_in_use), selected = true)
            DetailText(stringResource(R.string.export_in_use_detail, stringResource(output.assignTargetRes())))
        } else if (presetInUseFailed) {
            TerminalChip(
                label = stringResource(R.string.export_use_failed),
                selected = false,
                tone = TerminalChipTone.Warning,
            )
        } else if (presetImported) {
            TerminalChip(label = stringResource(R.string.export_imported), selected = true)
        } else if (importFailed) {
            TerminalChip(
                label = stringResource(R.string.export_import_failed),
                selected = false,
                tone = TerminalChipTone.Warning,
            )
        }
        saveResult?.let { result ->
            TerminalChip(
                label = stringResource(
                    if (result == SaveResult.Saved) R.string.export_saved else R.string.export_save_failed,
                ),
                selected = result == SaveResult.Saved,
                tone = if (result == SaveResult.Failed) TerminalChipTone.Warning else TerminalChipTone.Normal,
            )
        }
        TerminalSection(stringResource(R.string.export_section_poweramp)) {
            CodeLines(
                powerampAdvice(powerampSettings).map { stringResource(it) },
            )
        }
        TerminalSection(stringResource(R.string.export_section_steps)) {
            CodeLines(
                listOf(
                    stringResource(R.string.export_step_open),
                    stringResource(R.string.export_step_presets),
                    stringResource(R.string.export_step_select),
                ),
            )
        }
        TerminalSection(stringResource(R.string.export_section_assign)) {
            CodeLines(
                listOf(
                    stringResource(R.string.export_step_bands, bandSummary(bandLayout)),
                    stringResource(R.string.export_step_assign, stringResource(output.assignTargetRes())),
                ),
            )
        }
    }
}

@StringRes
internal fun PowerampIssue.adviceRes(): Int = when (this) {
    PowerampIssue.SmoothGains -> R.string.export_setting_smooth
    PowerampIssue.NoDvcReduction -> R.string.export_setting_preamp
}

/** Solo lo que hay que cambiar si se pudo leer Poweramp; la lista completa si no. El tono y el compresor no se pueden leer. */
private fun powerampAdvice(settings: PowerampSettings?): List<Int> {
    if (settings == null) {
        return listOf(
            R.string.export_setting_overlap,
            R.string.export_setting_smooth,
            R.string.export_setting_preamp,
            R.string.export_setting_clean,
        )
    }
    val fixes = settings.issues.map { it.adviceRes() }
    return fixes.ifEmpty { listOf(R.string.export_settings_good) } + R.string.export_setting_clean
}

@StringRes
private fun OutputKind.assignTargetRes(): Int = when (this) {
    OutputKind.Bluetooth -> R.string.export_assign_bluetooth
    OutputKind.Wired -> R.string.export_assign_wired
    OutputKind.Speaker -> R.string.export_assign_speaker
}

internal fun writeTo(context: Context, uri: Uri, json: String): Boolean = runCatching {
    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("No se pudo abrir el archivo")
    stream.use { it.write(json.toByteArray()) }
}.isSuccess

/** Escribe una copia en la caché y abre el menú de compartir del sistema con ella. */
private fun temporaryPresetUri(context: Context, fileName: String, json: String): Uri {
    val directory = File(context.cacheDir, SHARED_PRESETS_DIRECTORY).apply { mkdirs() }
    val file = File(directory, fileName).apply { writeText(json) }
    return FileProvider.getUriForFile(context, "${context.packageName}.presets", file)
}

private fun share(context: Context, fileName: String, json: String, chooserTitle: String) {
    val uri = temporaryPresetUri(context, fileName, json)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = PRESET_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, chooserTitle))
    } catch (_: ActivityNotFoundException) {
        // No hay ninguna app para compartir; "Guardar archivo" sigue disponible.
    }
}
