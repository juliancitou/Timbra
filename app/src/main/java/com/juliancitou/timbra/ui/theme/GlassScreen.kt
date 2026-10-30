package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.juliancitou.timbra.R

/**
 * Estructura común de cada pantalla: encabezado con título (y botón de volver si [onBack] no es
 * nulo), contenido desplazable y acciones fijas abajo, al alcance del pulgar.
 */
@Composable
fun GlassScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.large, vertical = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            if (onBack != null) {
                GlassIconButton(
                    iconRes = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            if (subtitle != null) {
                GlassCard {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
                }
            }
            content()
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small), content = actions)
    }
}
