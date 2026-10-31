package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** [Warning] es solo para estados de seguridad (nivel alto, audífonos desconectados). */
enum class TerminalChipTone { Normal, Warning }

private val ChipMinHeight = 40.dp

/**
 * Opción con estilo de terminal: solo la palabra entre corchetes, `[ Confiable ]`, sin fondo ni
 * contorno. Gris sin seleccionar, ámbar seleccionada y rojo para avisos; el color se desliza al
 * cambiar. Con [onClick] nulo es una etiqueta de estado. El área táctil es de al menos 48 dp, y
 * los lectores de pantalla leen solo la palabra, sin los corchetes.
 */
@Composable
fun TerminalChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tone: TerminalChipTone = TerminalChipTone.Normal,
    onClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val accent = when (tone) {
        TerminalChipTone.Normal -> colors.primary
        TerminalChipTone.Warning -> colors.error
    }
    val highlighted = selected || tone == TerminalChipTone.Warning
    val bracketColor by animateColorAsState(
        if (highlighted) accent else colors.onSurfaceVariant.copy(alpha = UNSELECTED_BRACKET_ALPHA),
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "chipBrackets",
    )
    val labelColor by animateColorAsState(
        if (highlighted) accent else colors.onSurfaceVariant,
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "chipLabel",
    )
    val interaction = if (onClick != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, role = Role.Checkbox) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(interaction)
            .defaultMinSize(minHeight = ChipMinHeight)
            .padding(horizontal = Spacing.extraSmall, vertical = Spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = bracketColor)) { append("[ ") }
                withStyle(SpanStyle(color = labelColor)) { append(label) }
                withStyle(SpanStyle(color = bracketColor)) { append(" ]") }
            },
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

private const val UNSELECTED_BRACKET_ALPHA = 0.6f

/**
 * Se ve como una opción, pero actúa solo mientras se mantiene presionada: sirve para comparar y volver
 * sin perder lo que se estaba escuchando.
 */
@Composable
fun TerminalHoldChip(label: String, onHoldChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var holding by remember { mutableStateOf(false) }
    val color by animateColorAsState(
        if (holding) colors.primary else colors.onSurfaceVariant,
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "holdChip",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Spacing.minTouchTarget)
            .semantics { role = Role.Button }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        holding = true
                        onHoldChange(true)
                        tryAwaitRelease()
                        holding = false
                        onHoldChange(false)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "[ $label ]",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
