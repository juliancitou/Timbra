package com.juliancitou.timbra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Contraste WCAG 2.1 de cada pareja texto/fondo de la app. AA pide 4.5:1 para texto normal y
 * 3:1 para elementos gráficos (curva, bordes de selección). Los fondos de vidrio se calculan
 * como blanco translúcido sobre el fondo más claro de la app, que es el peor caso.
 */
class ContrastTest {

    private val worstBackground = Charcoal
    private val glass = Color.White.copy(alpha = GLASS_FILL_ALPHA).over(worstBackground)
    private val glassFallback = Color.White.copy(alpha = GLASS_FALLBACK_FILL_ALPHA).over(worstBackground)

    @Test
    fun textOnBackgroundMeetsAa() {
        listOf(Ink, Graphite, Charcoal).forEach { background ->
            assertText(TextPrimary, background, "texto principal")
            assertText(TextSecondary, background, "texto secundario")
            assertText(Amber, background, "etiqueta // ámbar")
            assertText(Orange, background, "explicación naranja")
            assertText(DangerRed, background, "aviso rojo")
        }
    }

    @Test
    fun textOnGlassMeetsAa() {
        listOf(glass, glassFallback).forEach { background ->
            assertText(TextPrimary, background, "texto sobre vidrio")
            assertText(TextSecondary, background, "ejes de la gráfica")
            assertText(Amber, background, "valor en ámbar")
        }
    }

    @Test
    fun primaryButtonTextMeetsAa() {
        assertText(AmberDeep, Amber, "texto del botón principal")
    }

    @Test
    fun selectedChipTextMeetsAa() {
        listOf(Amber, DangerRed).forEach { accent ->
            val chipFill = accent.copy(alpha = 0.14f).over(glassFallback)
            assertText(TextPrimary, chipFill, "texto de chip seleccionado")
            assertGraphic(accent, glassFallback, "borde de chip seleccionado")
        }
    }

    @Test
    fun chartCurveMeetsGraphicContrast() {
        assertGraphic(Amber, glassFallback, "curva de la gráfica")
        assertGraphic(Orange, glassFallback, "banda resaltada")
    }

    private fun assertText(foreground: Color, background: Color, name: String) =
        assertRatio(foreground, background, minimum = 4.5, name = name)

    private fun assertGraphic(foreground: Color, background: Color, name: String) =
        assertRatio(foreground, background, minimum = 3.0, name = name)

    private fun assertRatio(foreground: Color, background: Color, minimum: Double, name: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$name: contraste ${"%.2f".format(ratio)} < $minimum", ratio >= minimum)
    }
}

/** Compone un color translúcido sobre un fondo opaco (mezcla "source over"). */
private fun Color.over(background: Color): Color = Color(
    red = red * alpha + background.red * (1 - alpha),
    green = green * alpha + background.green * (1 - alpha),
    blue = blue * alpha + background.blue * (1 - alpha),
)

private fun contrastRatio(first: Color, second: Color): Double {
    val lighter = maxOf(relativeLuminance(first), relativeLuminance(second))
    val darker = minOf(relativeLuminance(first), relativeLuminance(second))
    return (lighter + 0.05) / (darker + 0.05)
}

/** Luminancia relativa de WCAG 2.1 a partir de los canales sRGB de 8 bits. */
private fun relativeLuminance(color: Color): Double {
    val argb = color.toArgb()
    fun channel(shift: Int): Double {
        val value = ((argb shr shift) and 0xFF) / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}
