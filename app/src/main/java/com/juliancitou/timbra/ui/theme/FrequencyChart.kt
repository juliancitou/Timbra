package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.juliancitou.timbra.export.POWERAMP_BAND_FREQUENCIES_HZ

private val ChartHeight = 220.dp
private val LabelGap = 6.dp
private val CurveStrokeWidth = 3.dp
private val PointRadius = 4.dp
private val HighlightRadius = 9.dp

private val GainGridStepsDb = listOf(-12f, -6f, 0f, 6f, 12f)
private val FrequencyGridHz = listOf(20, 100, 1000, 10_000, 20_000)

/**
 * Curva de ganancia sobre las 25 bandas de Poweramp. [gainsDb] va en el mismo orden que
 * [frequenciesHz]. Las bandas de [estimatedBandIndices] se dibujan huecas (no se midieron con
 * confianza) y [highlightedBandIndex] marca la banda en curso.
 */
@Composable
fun FrequencyChart(
    gainsDb: List<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    frequenciesHz: List<Int> = POWERAMP_BAND_FREQUENCIES_HZ,
    minDb: Float = -12f,
    maxDb: Float = 12f,
    estimatedBandIndices: Set<Int> = emptySet(),
    highlightedBandIndex: Int? = null,
) {
    require(gainsDb.size == frequenciesHz.size) { "Cada frecuencia necesita una ganancia" }
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.withTabularNumbers().copy(color = colors.onSurfaceVariant)
    val chartColors = ChartColors(
        curve = colors.primary,
        grid = Color.White.copy(alpha = 0.10f),
        zeroLine = Color.White.copy(alpha = 0.32f),
        estimatedPoint = colors.onSurfaceVariant,
        highlight = colors.secondary,
        pointCenter = colors.surface,
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { this.contentDescription = contentDescription },
    ) {
        // Los márgenes salen de medir las etiquetas, así crecen con el tamaño de letra del sistema.
        val widestGainLabel = textMeasurer.measure(formatGainAxisLabel(minDb), labelStyle).size
        val frequencyLabelHeight = textMeasurer.measure(formatFrequencyAxisLabel(1000), labelStyle).size.height
        val plot = PlotArea(
            left = widestGainLabel.width + LabelGap.toPx(),
            top = widestGainLabel.height / 2f,
            right = size.width,
            bottom = size.height - frequencyLabelHeight - LabelGap.toPx(),
        )
        drawGrid(plot, minDb, maxDb, chartColors, textMeasurer, labelStyle)
        val points = frequenciesHz.zip(gainsDb) { hz, db ->
            Offset(
                x = plot.left + plot.width * frequencyToFraction(hz.toFloat()),
                y = plot.top + plot.height * gainToFraction(db, minDb, maxDb),
            )
        }
        drawCurve(points, plot, chartColors.curve)
        drawPoints(points, estimatedBandIndices, highlightedBandIndex, plot, chartColors)
    }
}

private class ChartColors(
    val curve: Color,
    val grid: Color,
    val zeroLine: Color,
    val estimatedPoint: Color,
    val highlight: Color,
    val pointCenter: Color,
)

private class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

private fun DrawScope.drawGrid(
    plot: PlotArea,
    minDb: Float,
    maxDb: Float,
    colors: ChartColors,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    GainGridStepsDb.filter { it in minDb..maxDb }.forEach { db ->
        val y = plot.top + plot.height * gainToFraction(db, minDb, maxDb)
        drawLine(
            color = if (db == 0f) colors.zeroLine else colors.grid,
            start = Offset(plot.left, y),
            end = Offset(plot.right, y),
            strokeWidth = 1.dp.toPx(),
        )
        val label = textMeasurer.measure(formatGainAxisLabel(db), labelStyle)
        drawText(
            textLayoutResult = label,
            topLeft = Offset(plot.left - label.size.width - LabelGap.toPx(), y - label.size.height / 2),
        )
    }
    FrequencyGridHz.forEach { hz ->
        val x = plot.left + plot.width * frequencyToFraction(hz.toFloat())
        drawLine(
            color = colors.grid,
            start = Offset(x, plot.top),
            end = Offset(x, plot.bottom),
            strokeWidth = 1.dp.toPx(),
        )
        val label = textMeasurer.measure(formatFrequencyAxisLabel(hz), labelStyle)
        val labelX = (x - label.size.width / 2).coerceIn(plot.left, plot.right - label.size.width)
        drawText(textLayoutResult = label, topLeft = Offset(labelX, plot.bottom + LabelGap.toPx()))
    }
}

/**
 * Curva suave que pasa por todos los puntos (Catmull-Rom convertida a Bézier cúbica). Debajo
 * lleva un relleno que se desvanece hacia la base de la gráfica.
 */
private fun DrawScope.drawCurve(points: List<Offset>, plot: PlotArea, color: Color) {
    if (points.size < 2) return
    val curve = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.lastIndex) {
            val previous = points[(i - 1).coerceAtLeast(0)]
            val start = points[i]
            val end = points[i + 1]
            val next = points[(i + 2).coerceAtMost(points.lastIndex)]
            cubicTo(
                start.x + (end.x - previous.x) / 6f, start.y + (end.y - previous.y) / 6f,
                end.x - (next.x - start.x) / 6f, end.y - (next.y - start.y) / 6f,
                end.x, end.y,
            )
        }
    }
    val fill = Path().apply {
        addPath(curve)
        lineTo(points.last().x, plot.bottom)
        lineTo(points.first().x, plot.bottom)
        close()
    }
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.28f), Color.Transparent),
            startY = plot.top,
            endY = plot.bottom,
        ),
    )
    drawPath(
        path = curve,
        color = color,
        style = Stroke(width = CurveStrokeWidth.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawPoints(
    points: List<Offset>,
    estimatedBandIndices: Set<Int>,
    highlightedBandIndex: Int?,
    plot: PlotArea,
    colors: ChartColors,
) {
    highlightedBandIndex?.let { index ->
        val point = points.getOrNull(index) ?: return@let
        drawLine(
            color = colors.highlight.copy(alpha = 0.5f),
            start = Offset(point.x, plot.top),
            end = Offset(point.x, plot.bottom),
            strokeWidth = 2.dp.toPx(),
        )
        drawCircle(color = colors.highlight, radius = HighlightRadius.toPx(), center = point)
    }
    points.forEachIndexed { index, point ->
        if (index in estimatedBandIndices) {
            drawCircle(color = colors.pointCenter, radius = PointRadius.toPx(), center = point)
            drawCircle(
                color = colors.estimatedPoint,
                radius = PointRadius.toPx(),
                center = point,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        } else {
            drawCircle(color = colors.curve, radius = PointRadius.toPx(), center = point)
        }
    }
}

private fun formatGainAxisLabel(gainDb: Float): String = when {
    gainDb > 0f -> "+${gainDb.toInt()}"
    gainDb < 0f -> "−${-gainDb.toInt()}"
    else -> "0"
}

private fun formatFrequencyAxisLabel(frequencyHz: Int): String =
    if (frequencyHz >= 1000) "${frequencyHz / 1000}k" else "$frequencyHz"
