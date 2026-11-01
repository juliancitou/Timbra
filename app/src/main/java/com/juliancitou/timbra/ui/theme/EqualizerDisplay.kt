package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/** Rango de Poweramp: −8 a +3 dB. Cada celda vale lo que el paso de sus ganancias, 0.5 dB. */
private const val DISPLAY_MIN_DB = -8f
private const val DISPLAY_MAX_DB = 3f
private const val DB_PER_CELL = 0.5f
private val CellPitch = 7.dp
private val CellGap = 1.5.dp
private val ColumnGap = 1.5.dp
private val PresetMarkHeight = 2.dp
private val LabelGap = 6.dp
private const val EMPTY_CELL_ALPHA = 0.07f
private const val NEAREST_CELL_ALPHA = 0.55f
private const val SELECTED_COLUMN_ALPHA = 0.10f
private const val DIM_ZONE_ALPHA = 0.7f
private val GainScaleDb = listOf(3f, 0f, -3f, -6f)
private val FrequencyScaleHz = listOf(20, 100, 1000, 5000, 20_000)

/** Una etiqueta bajo la gráfica. [changed] la resalta: ahí se movió algo. */
data class DisplayZone(val label: String, val centerHz: Float, val changed: Boolean)

/**
 * Pantalla de ecualizador como la de un equipo de estudio, hecha de celdas de 0.5 dB: las columnas muestran
 * lo que suena ahora y la marca blanca, lo que pide el preset. Al apagar la ecualización las columnas se
 * vacían y la marca se queda: se ve de inmediato qué se está quitando. Tocar o arrastrar elige una banda.
 *
 * @param soundingDb ganancia que suena ahora en cada banda; `null` donde no se pudo medir.
 */
@Composable
fun EqualizerDisplay(
    presetDb: List<Float>,
    soundingDb: List<Float?>,
    frequenciesHz: List<Int>,
    zones: List<DisplayZone>,
    statusLabel: String,
    statusActive: Boolean,
    legend: String,
    readout: (frequency: String, preset: String, sounding: String?) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val labelStyle = typography.labelSmall.withTabularNumbers().copy(color = colors.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    val shownDb = rememberMorphingGains(soundingDb.map { it ?: 0f })
    val strongestBand = presetDb.indices.maxBy { abs(presetDb[it]) }
    var selectedBand by rememberSaveable(frequenciesHz) { mutableStateOf(strongestBand) }
    var plotLeft by remember { mutableStateOf(0f) }
    fun selectAt(x: Float, width: Float) {
        val columnWidth = (width - plotLeft) / frequenciesHz.size
        selectedBand = ((x - plotLeft) / columnWidth).toInt().coerceIn(frequenciesHz.indices)
    }

    val rows = ((DISPLAY_MAX_DB - DISPLAY_MIN_DB) / DB_PER_CELL).roundToInt()
    val labelHeight = with(LocalDensity.current) { labelStyle.lineHeight.toDp() }
    val height = CellPitch * rows + (labelHeight + LabelGap) * 2

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = (if (statusActive) "● " else "○ ") + statusLabel,
            style = typography.labelLarge,
            color = if (statusActive) colors.primary else colors.onSurfaceVariant,
        )
        Text(
            text = readout(
                formatHertz(frequenciesHz[selectedBand]),
                formatDecibels(presetDb[selectedBand]),
                soundingDb[selectedBand]?.let(::formatDecibels),
            ),
            style = typography.bodyMedium.withTabularNumbers(),
            color = colors.onBackground,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { this.contentDescription = contentDescription }
                .pointerInput(frequenciesHz) {
                    detectTapGestures { selectAt(it.x, size.width.toFloat()) }
                }
                .pointerInput(frequenciesHz) {
                    detectHorizontalDragGestures { change, _ -> selectAt(change.position.x, size.width.toFloat()) }
                },
        ) {
            val gutter = textMeasurer.measure("−6", labelStyle).size.width + LabelGap.toPx()
            plotLeft = gutter
            val plot = DisplayPlot(gutter, size.width, CellPitch.toPx(), rows, frequenciesHz.size)
            drawCells(plot, colors.onSurfaceVariant.copy(alpha = EMPTY_CELL_ALPHA))
            drawSelectedColumn(plot, selectedBand, colors.onBackground.copy(alpha = SELECTED_COLUMN_ALPHA))
            drawGainScale(plot, textMeasurer, labelStyle, colors.onBackground)
            drawBars(plot, shownDb(), colors.primary, colors.secondary)
            drawPresetMarks(plot, presetDb, colors.onBackground)
            drawFrequencyScale(plot, frequenciesHz, textMeasurer, labelStyle)
            drawZones(plot, frequenciesHz, zones, textMeasurer, labelStyle, colors.primary)
        }
        Text(text = legend, style = typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

private class DisplayPlot(val left: Float, val right: Float, val cellPitch: Float, val rows: Int, val columns: Int) {
    val bottom get() = rows * cellPitch
    val columnWidth get() = (right - left) / columns

    fun columnX(column: Int) = left + column * columnWidth

    /** Altura de la línea de una ganancia: 0 dB queda entre dos celdas. */
    fun gainY(gainDb: Float) = (DISPLAY_MAX_DB - gainDb) / DB_PER_CELL * cellPitch

    /** Posición horizontal de una frecuencia, en la escala de las bandas que se muestran. */
    fun frequencyX(hz: Float, frequenciesHz: List<Int>): Float {
        val first = frequenciesHz.first().toFloat()
        val last = frequenciesHz.last().toFloat()
        val fraction = log10(hz.coerceIn(first, last) / first) / log10(last / first)
        return left + (right - left) * fraction
    }
}

private fun DrawScope.drawCell(plot: DisplayPlot, column: Int, row: Int, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(plot.columnX(column), row * plot.cellPitch),
        size = Size(plot.columnWidth - ColumnGap.toPx(), plot.cellPitch - CellGap.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx()),
    )
}

private fun DrawScope.drawCells(plot: DisplayPlot, color: Color) {
    for (column in 0 until plot.columns) {
        for (row in 0 until plot.rows) drawCell(plot, column, row, color)
    }
}

private fun DrawScope.drawSelectedColumn(plot: DisplayPlot, band: Int, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(plot.columnX(band), 0f),
        size = Size(plot.columnWidth - ColumnGap.toPx(), plot.bottom),
    )
}

private fun DrawScope.drawGainScale(plot: DisplayPlot, textMeasurer: TextMeasurer, style: TextStyle, lineColor: Color) {
    GainScaleDb.forEach { db ->
        val y = plot.gainY(db)
        val label = textMeasurer.measure(if (db > 0f) "+${db.toInt()}" else if (db < 0f) "−${-db.toInt()}" else "0", style)
        drawText(label, topLeft = Offset(0f, (y - label.size.height / 2f).coerceAtLeast(0f)))
        drawLine(
            color = lineColor.copy(alpha = if (db == 0f) 0.45f else 0.12f),
            start = Offset(plot.left, y),
            end = Offset(plot.right, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = if (db == 0f) null else PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
    }
}

/** Cada columna crece desde 0 dB y se enciende más lejos de él, como un medidor de estudio. */
private fun DrawScope.drawBars(plot: DisplayPlot, gainsDb: List<Float>, boost: Color, cut: Color) {
    val zeroRow = (DISPLAY_MAX_DB / DB_PER_CELL).roundToInt()
    gainsDb.forEachIndexed { column, gainDb ->
        val cells = (abs(gainDb.coerceIn(DISPLAY_MIN_DB, DISPLAY_MAX_DB)) / DB_PER_CELL).roundToInt()
        for (cell in 0 until cells) {
            val strength = NEAREST_CELL_ALPHA + (1 - NEAREST_CELL_ALPHA) * (cell + 1f) / cells
            val row = if (gainDb > 0f) zeroRow - 1 - cell else zeroRow + cell
            drawCell(plot, column, row, (if (gainDb > 0f) boost else cut).copy(alpha = strength))
        }
    }
}

private fun DrawScope.drawPresetMarks(plot: DisplayPlot, presetDb: List<Float>, color: Color) {
    presetDb.forEachIndexed { column, gainDb ->
        if (gainDb == 0f) return@forEachIndexed
        val y = plot.gainY(gainDb.coerceIn(DISPLAY_MIN_DB, DISPLAY_MAX_DB))
        drawRect(
            color = color,
            topLeft = Offset(plot.columnX(column), y - PresetMarkHeight.toPx() / 2),
            size = Size(plot.columnWidth - ColumnGap.toPx(), PresetMarkHeight.toPx()),
        )
    }
}

private fun DrawScope.drawFrequencyScale(
    plot: DisplayPlot,
    frequenciesHz: List<Int>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    FrequencyScaleHz.filter { it in frequenciesHz.first()..frequenciesHz.last() }.forEach { hz ->
        val label = textMeasurer.measure(if (hz >= 1000) "${hz / 1000}k" else "$hz", style)
        val x = (plot.frequencyX(hz.toFloat(), frequenciesHz) - label.size.width / 2f)
            .coerceIn(plot.left, plot.right - label.size.width)
        drawText(label, topLeft = Offset(x, plot.bottom + LabelGap.toPx()))
    }
}

private fun DrawScope.drawZones(
    plot: DisplayPlot,
    frequenciesHz: List<Int>,
    zones: List<DisplayZone>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    changedColor: Color,
) {
    zones.forEach { zone ->
        val color = if (zone.changed) changedColor else style.color.copy(alpha = DIM_ZONE_ALPHA)
        val label = textMeasurer.measure(zone.label, style.copy(color = color))
        val x = (plot.frequencyX(zone.centerHz, frequenciesHz) - label.size.width / 2f)
            .coerceIn(plot.left, plot.right - label.size.width)
        drawText(label, topLeft = Offset(x, size.height - label.size.height))
    }
}
