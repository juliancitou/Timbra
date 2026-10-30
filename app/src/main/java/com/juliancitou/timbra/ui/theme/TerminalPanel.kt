package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val PANEL_FRAME_ALPHA = 0.35f

/** Recuadro ASCII sin fondo (┌ - - ┐) para agrupar contenido, como la gráfica. */
@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.small),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .asciiFrame(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PANEL_FRAME_ALPHA))
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
