package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val MinFitFontSize = 16.sp

/**
 * Valor grande en una sola línea ("+12.0 dB", "11.2 kHz"). Si con la letra grande del sistema no
 * cabe, reduce el tamaño en lugar de separar el número de su unidad.
 */
@Composable
fun FitText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = style.copy(color = color),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = MinFitFontSize, maxFontSize = style.fontSize),
        modifier = modifier,
    )
}
