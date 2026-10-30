package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val NodeSize = 60.dp
private val RingWidth = 2.dp
private val ActiveRingWidth = 3.dp
private const val DOT_FRACTION = 0.32f
private const val BREATH_MS = 1_400

/**
 * Opciones de una ronda del test como nodos grandes `( )` en fila: tocar uno lo escucha. El que
 * suena se ilumina en ámbar con un punto que crece con rebote y un anillo que "respira" mientras
 * hay sonido. Debajo de cada nodo va su valor y, en los extremos, qué significa cada lado.
 */
@Composable
fun OptionNodes(
    values: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    lowerLabel: String,
    upperLabel: String,
    nodeDescription: (index: Int) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    playing: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val breath = rememberBreath(active = playing && !isReducedMotionEnabled())
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            values.forEachIndexed { index, value ->
                Column(
                    modifier = Modifier
                        .selectable(selected = index == selectedIndex, enabled = enabled, role = Role.RadioButton) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(index)
                        }
                        .semantics { contentDescription = nodeDescription(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    Node(selected = index == selectedIndex, enabled = enabled, breath = breath)
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium.withTabularNumbers(),
                        color = if (index == selectedIndex) colors.primary else colors.onSurfaceVariant,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailText("← $lowerLabel")
            DetailText("$upperLabel →")
        }
    }
}

@Composable
private fun Node(selected: Boolean, enabled: Boolean, breath: State<Float>) {
    val colors = MaterialTheme.colorScheme
    val ring by animateColorAsState(
        when {
            !enabled -> colors.outline
            selected -> colors.primary
            else -> colors.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(MotionDuration.SHORT_MS),
        label = "nodeRing",
    )
    val dot by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "nodeDot",
    )
    Canvas(modifier = Modifier.size(NodeSize)) {
        val radius = size.minDimension / 2
        val ringWidth = (if (selected) ActiveRingWidth else RingWidth).toPx()
        if (selected) {
            drawCircle(color = ring.copy(alpha = 0.18f * breath.value), radius = radius - ringWidth / 2)
        }
        drawCircle(color = ring, radius = radius - ringWidth, style = Stroke(width = ringWidth))
        if (dot > 0f) {
            drawCircle(color = colors.primary, radius = radius * DOT_FRACTION * dot)
        }
    }
}

/** Pulso suave de 0 a 1 que marca que el nodo elegido está sonando. */
@Composable
private fun rememberBreath(active: Boolean): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MS), RepeatMode.Reverse),
        label = "breathValue",
    )
}
