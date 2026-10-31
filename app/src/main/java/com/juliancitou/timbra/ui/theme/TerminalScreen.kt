package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R

/**
 * Estructura común de cada pantalla, sin tarjetas de más: barra superior con volver y una
 * acción opcional, título estilo terminal (`> Título`), la indicación principal ([lead]) en
 * letra grande, el contenido desplazable y las acciones fijas abajo, al alcance del pulgar.
 */
@Composable
fun TerminalScreen(
    title: String,
    modifier: Modifier = Modifier,
    lead: String? = null,
    showCursor: Boolean = false,
    onBack: (() -> Unit)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        if (onBack != null || headerAction != null) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    TerminalIconButton(
                        iconRes = R.drawable.ic_arrow_back,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                        motion = IconMotion.NudgeBack,
                    )
                }
                Spacer(Modifier.weight(1f))
                headerAction?.invoke()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.large)) {
                TerminalTitle(text = title, showCursor = showCursor)
                if (lead != null) LeadText(lead)
            }
            content()
            Spacer(Modifier.height(Spacing.small))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.inSection), content = actions)
    }
}
