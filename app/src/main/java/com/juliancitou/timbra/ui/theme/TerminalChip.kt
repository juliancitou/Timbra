package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** [Warning] es solo para estados de seguridad (nivel alto, audífonos desconectados). */
enum class TerminalChipTone { Normal, Warning }

private val ChipMinHeight = 48.dp
private val ChipFrameWidth = 1.5.dp

/**
 * Opción tocable en su propia caja: línea gris sin seleccionar y relleno ámbar seleccionada, para que
 * no haya duda de dónde empieza cada una ni de cuál está elegida. Con [onClick] nulo no es un botón
 * sino una etiqueta de estado, y por eso va sin caja y entre corchetes: `[ Archivo guardado ]`. El
 * rojo es solo para avisos, y los lectores de pantalla leen solo la palabra.
 */
@Composable
fun TerminalChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tone: TerminalChipTone = TerminalChipTone.Normal,
    onClick: (() -> Unit)? = null,
) {
    if (onClick == null) {
        StatusLabel(label, selected, tone, modifier)
        return
    }
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(ButtonCornerRadius)
    val container by animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "chipContainer",
    )
    val frame by animateColorAsState(
        if (selected) colors.primary else colors.onSurfaceVariant,
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "chipFrame",
    )
    val textColor by animateColorAsState(
        if (selected) Ink else colors.onSurface,
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "chipLabel",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ChipMinHeight)
            .background(container, shape)
            .border(ChipFrameWidth, frame, shape)
            .clip(shape)
            .selectable(selected = selected, role = Role.Checkbox) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusLabel(label: String, selected: Boolean, tone: TerminalChipTone, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val color = when {
        tone == TerminalChipTone.Warning -> colors.error
        selected -> colors.primary
        else -> colors.onSurfaceVariant
    }
    Text(
        text = "[ $label ]",
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 2,
        modifier = modifier
            .padding(vertical = Spacing.small)
            .semantics { contentDescription = label },
    )
}
