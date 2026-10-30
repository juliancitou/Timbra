package com.juliancitou.timbra.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** [Warning] es solo para estados de seguridad (nivel alto, audífonos desconectados). */
enum class GlassChipTone { Normal, Warning }

private val ChipShape = RoundedCornerShape(20.dp)
private val ChipMinHeight = 40.dp
private const val SELECTED_FILL_ALPHA = 0.14f

/**
 * Opción seleccionable o, con [onClick] nulo, etiqueta de estado. El área táctil crece a 48 dp
 * aunque el chip se vea de 40 dp. Con letra muy grande el texto puede usar dos líneas.
 */
@Composable
fun GlassChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tone: GlassChipTone = GlassChipTone.Normal,
    onClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val accent = when (tone) {
        GlassChipTone.Normal -> MaterialTheme.colorScheme.primary
        GlassChipTone.Warning -> MaterialTheme.colorScheme.error
    }
    val highlighted = selected || tone == GlassChipTone.Warning
    val surface = if (highlighted) {
        Modifier
            .clip(ChipShape)
            .background(accent.copy(alpha = SELECTED_FILL_ALPHA))
            .border(1.dp, accent, ChipShape)
    } else {
        Modifier.glassSurface(shape = ChipShape, elevated = false)
    }
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
            .then(surface)
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
