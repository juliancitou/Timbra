package com.juliancitou.timbra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val CURSOR_BLINK_MS = 1_100

/** El título se escribe en ~0.3 s en total, sin pasar de 35 ms por letra en títulos cortos. */
private const val TYPEWRITER_TOTAL_MS = 300L
private const val TYPEWRITER_MAX_CHAR_MS = 35L

/**
 * Título con forma de línea de terminal: `> Título`, y opcionalmente un cursor `_` que
 * parpadea (fijo si el sistema pide reducir movimiento). El `>` y el cursor son decorativos:
 * los lectores de pantalla solo leen el título.
 */
@Composable
fun TerminalTitle(text: String, modifier: Modifier = Modifier, showCursor: Boolean = false) {
    val style = MaterialTheme.typography.headlineMedium
    val accent = MaterialTheme.colorScheme.primary
    val animated = !isReducedMotionEnabled()
    val cursorAlpha = rememberCursorAlpha(blinking = showCursor && animated)
    val typed = rememberTypedCharacters(text, animated)
    val textColor = MaterialTheme.colorScheme.onBackground
    Row(modifier = modifier.semantics(mergeDescendants = true) { heading() }) {
        Text(text = "> ", style = style, color = accent, modifier = Modifier.clearAndSetSemantics {})
        Text(
            text = typewriterText(text, typed.value, textColor),
            style = style,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { contentDescription = text },
        )
        if (showCursor) {
            Text(
                text = "_",
                style = style,
                color = accent,
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .graphicsLayer { alpha = cursorAlpha.value },
            )
        }
    }
}

/** Cuántas letras del título ya se "escribieron": todas, de golpe, si se reduce el movimiento. */
@Composable
private fun rememberTypedCharacters(text: String, animated: Boolean): State<Int> {
    val typed = remember(text) { mutableIntStateOf(if (animated) 0 else text.length) }
    LaunchedEffect(text) {
        if (!animated) return@LaunchedEffect
        val perCharacter = (TYPEWRITER_TOTAL_MS / text.length.coerceAtLeast(1)).coerceAtMost(TYPEWRITER_MAX_CHAR_MS)
        while (typed.intValue < text.length) {
            delay(perCharacter)
            typed.intValue++
        }
    }
    return typed
}

/**
 * El título completo, con las letras aún no escritas transparentes: así ocupa su lugar desde el
 * inicio y nada se mueve mientras aparece.
 */
private fun typewriterText(text: String, typed: Int, color: Color) = buildAnnotatedString {
    withStyle(SpanStyle(color = color)) { append(text.take(typed)) }
    withStyle(SpanStyle(color = Color.Transparent)) { append(text.drop(typed)) }
}

@Composable
private fun rememberCursorAlpha(blinking: Boolean): State<Float> {
    if (!blinking) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CURSOR_BLINK_MS
                1f at 0
                1f at CURSOR_BLINK_MS / 2
                0f at CURSOR_BLINK_MS / 2 + 1
            },
        ),
        label = "cursorAlpha",
    )
}

/** La indicación principal de la pantalla: grande y en blanco, lo primero que se lee. */
@Composable
fun LeadText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 27.sp),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

/** Encabezado de sección con forma de comentario de código: `// cómo funciona` (se lee sin `//`). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = "// $text",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.clearAndSetSemantics {
            heading()
            contentDescription = text
        },
    )
}

/** Texto de apoyo: explicaciones secundarias en gris. */
@Composable
fun DetailText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color,
        modifier = modifier,
    )
}

/** Pasos numerados `01`, `02`… con el número en ámbar. */
@Composable
fun StepList(steps: List<String>, modifier: Modifier = Modifier) {
    MarkedList(items = steps, marker = { index -> (index + 1).toString().padStart(2, '0') }, modifier = modifier)
}

/** Lista con viñeta `›`, para hallazgos o recomendaciones sin orden. */
@Composable
fun BulletList(items: List<String>, modifier: Modifier = Modifier) {
    MarkedList(items = items, marker = { "›" }, modifier = modifier)
}

@Composable
private fun MarkedList(items: List<String>, marker: (Int) -> String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.staggeredEntrance(index),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(
                    text = marker(index),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

/** Casilla con forma de terminal: `[x]` marcada en ámbar, `[ ]` sin marcar en gris. */
@Composable
fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .toggleable(value = checked, role = Role.Checkbox) {
                haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onToggle()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        val markColor by animateColorAsState(
            if (checked) colors.primary else colors.onSurfaceVariant,
            label = "mark",
        )
        val labelColor by animateColorAsState(
            if (checked) colors.onBackground else colors.onSurfaceVariant,
            label = "label",
        )
        val markScale by animateFloatAsState(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
            label = "markScale",
        )
        val style = MaterialTheme.typography.titleMedium
        Row(modifier = Modifier.clearAndSetSemantics {}) {
            Text(text = "[", style = style, color = markColor)
            Box(contentAlignment = Alignment.Center) {
                Text(text = " ", style = style)
                Text(
                    text = "x",
                    style = style,
                    color = markColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = markScale
                        scaleY = markScale
                        alpha = markScale.coerceIn(0f, 1f)
                    },
                )
            }
            Text(text = "]", style = style, color = markColor)
        }
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
    }
}

/** Línea divisoria tenue para separar elementos sin usar tarjetas. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}

/**
 * Enlace que se abre en el navegador del teléfono: [label] arriba y la dirección en ámbar con una
 * flecha `→`. La app no necesita permiso de internet: quien abre la página es el navegador.
 */
@Composable
fun LinkRow(label: String, url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .clickable(role = Role.Button, onClickLabel = label) { runCatching { uriHandler.openUri(url) } }
            .padding(vertical = Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = "→ " + url.removePrefix("https://"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
