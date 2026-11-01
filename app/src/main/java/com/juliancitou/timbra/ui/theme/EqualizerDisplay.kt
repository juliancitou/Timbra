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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

/** Rango de Poweramp: −8 a +3 dB. */
private const val DISPLAY_MIN_DB = -8f
private const val DISPLAY_MAX_DB = 3f
private val PlotHeight = 180.dp
private val LabelGap = 6.dp
private val ActiveStrokeWidth = 3.dp
private val PresetStrokeWidth = 1.5.dp
private val CursorRadius = 5.dp
private const val FILL_ALPHA = 0.20f
private const val ZERO_LINE_ALPHA = 0.55f
private const val GRID_ALPHA = 0.12f
private const val CURSOR_ALPHA = 0.5f
private const val DIM_ZONE_ALPHA = 0.7f
private val GainScaleDb = listOf(3f, 0f, -3f, -6f)
private val FrequencyScaleHz = listOf(20, 100, 1000, 5000, 20_000)

/** Una etiqueta bajo la gráfica. [changed] la resalta: ahí se movió algo. */
data class DisplayZone(val label: String, val centerHz: Float, val changed: Boolean)

/**
 * Gráfica de ecualizador de estudio. La línea en 0 dB es el sonido original y la línea ámbar, lo que está
 * activo; el relleno entre las dos es lo que cambia. Al apagar la ecualización la línea ámbar baja a 0 y el
 * preset queda punteado, para ver qué se está quitando. Tocar o arrastrar elige una banda.
 *
 * @param soundingDb lo medido o leído en cada banda, para la lectura numérica; `null` donde no se pudo.
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
    val activeDb = rememberMorphingGains(if (statusActive) presetDb else presetDb.map { 0f })
    var selectedBand by rememberSaveable(frequenciesHz) {
        mutableStateOf(presetDb.indices.maxBy { abs(presetDb[it]) })
    }
    var plotLeft by remember { mutableStateOf(0f) }
    fun selectAt(x: Float, width: Float) {
        val plot = DisplayPlot(plotLeft, width, 0f)
        selectedBand = frequenciesHz.indices.minBy { abs(plot.frequencyX(frequenciesHz[it].toFloat(), frequenciesHz) - x) }
    }

    val labelHeight = with(LocalDensity.current) { labelStyle.lineHeight.toDp() }

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
                .height(PlotHeight + (labelHeight + LabelGap) * 2)
                .semantics { this.contentDescription = contentDescription }
                .pointerInput(frequenciesHz) {
                    detectTapGestures { selectAt(it.x, size.width.toFloat()) }
                }
                .pointerInput(frequenciesHz) {
                    detectHorizontalDragGestures { change, _ -> selectAt(change.position.x, size.width.toFloat()) }
                },
        ) {
            plotLeft = textMeasurer.measure("−6", labelStyle).size.width + LabelGap.toPx()
            val plot = DisplayPlot(plotLeft, size.width, PlotHeight.toPx())
            fun pointsOf(gains: List<Float>) = frequenciesHz.zip(gains) { hz, db ->
                Offset(plot.frequencyX(hz.toFloat(), frequenciesHz), plot.gainY(db.coerceIn(DISPLAY_MIN_DB, DISPLAY_MAX_DB)))
            }
            val preset = pointsOf(presetDb)
            val active = pointsOf(activeDb())
            drawGainScale(plot, textMeasurer, labelStyle, colors.onBackground)
            drawCursor(plot, active[selectedBand], colors.onBackground)
            if (!statusActive) drawPreset(preset, colors.onBackground)
            drawActive(plot, active, colors.primary)
            drawCircle(colors.primary, CursorRadius.toPx(), active[selectedBand])
            drawFrequencyScale(plot, frequenciesHz, textMeasurer, labelStyle)
            drawZones(plot, frequenciesHz, zones, textMeasurer, labelStyle, colors.primary)
        }
        Text(text = legend, style = typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

private class DisplayPlot(val left: Float, val right: Float, val bottom: Float) {
    fun gainY(gainDb: Float) = bottom * (DISPLAY_MAX_DB - gainDb) / (DISPLAY_MAX_DB - DISPLAY_MIN_DB)

    /** Posición horizontal de una frecuencia, en la escala de las bandas que se muestran. */
    fun frequencyX(hz: Float, frequenciesHz: List<Int>): Float {
        val first = frequenciesHz.first().toFloat()
        val last = frequenciesHz.last().toFloat()
        val fraction = log10(hz.coerceIn(first, last) / first) / log10(last / first)
        return left + (right - left) * fraction
    }
}

private fun DrawScope.drawGainScale(plot: DisplayPlot, textMeasurer: TextMeasurer, style: TextStyle, lineColor: Color) {
    GainScaleDb.forEach { db ->
        val y = plot.gainY(db)
        val text = if (db > 0f) "+${db.toInt()}" else if (db < 0f) "−${-db.toInt()}" else "0"
        val label = textMeasurer.measure(text, style)
        drawText(label, topLeft = Offset(0f, (y - label.size.height / 2f).coerceAtLeast(0f)))
        drawLine(
            color = lineColor.copy(alpha = if (db == 0f) ZERO_LINE_ALPHA else GRID_ALPHA),
            start = Offset(plot.left, y),
            end = Offset(plot.right, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = if (db == 0f) null else PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
    }
}

private fun DrawScope.drawCursor(plot: DisplayPlot, point: Offset, color: Color) {
    drawLine(
        color = color.copy(alpha = CURSOR_ALPHA),
        start = Offset(point.x, 0f),
        end = Offset(point.x, plot.bottom),
        strokeWidth = 1.dp.toPx(),
    )
}

private fun DrawScope.drawPreset(points: List<Offset>, color: Color) {
    drawPath(
        path = smoothCurvePath(points),
        color = color,
        style = Stroke(
            width = PresetStrokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        ),
    )
}

/** El relleno llega hasta la línea de 0 dB: su tamaño es cuánto cambia el sonido original. */
private fun DrawScope.drawActive(plot: DisplayPlot, points: List<Offset>, color: Color) {
    val curve = smoothCurvePath(points)
    val zeroY = plot.gainY(0f)
    val fill = Path().apply {
        addPath(curve)
        lineTo(points.last().x, zeroY)
        lineTo(points.first().x, zeroY)
        close()
    }
    drawPath(fill, color.copy(alpha = FILL_ALPHA))
    drawPath(curve, color, style = Stroke(width = ActiveStrokeWidth.toPx(), cap = StrokeCap.Round))
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
