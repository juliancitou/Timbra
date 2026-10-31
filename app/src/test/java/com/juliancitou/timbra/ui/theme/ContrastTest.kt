package com.juliancitou.timbra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Contraste WCAG 2.1 de cada pareja texto/fondo de la app. AA pide 4.5:1 para texto normal y
 * 3:1 para elementos gráficos (curva, marcos). Todo va directo sobre el fondo, así que se revisan
 * sus tres tonos; el más claro ([Charcoal]) es el peor caso.
 */
class ContrastTest {

    private val backgrounds = listOf(Ink, Graphite, Charcoal)

    @Test
    fun textOnBackgroundMeetsAa() {
        backgrounds.forEach { background ->
            assertText(TextPrimary, background, "texto principal")
            assertText(TextSecondary, background, "texto secundario")
            assertText(Amber, background, "texto ámbar (títulos, chips, botón principal)")
            assertText(Orange, background, "texto naranja (cadenas en el código)")
            assertText(DangerRed, background, "aviso rojo")
        }
    }

    @Test
    fun darkTextOnAmberMeetsAa() {
        assertText(Ink, Amber, "letra del botón principal y de la opción elegida")
    }

    @Test
    fun graphicsMeetGraphicContrast() {
        backgrounds.forEach { background ->
            assertGraphic(Amber, background, "curva de la gráfica y marco del botón principal")
            assertGraphic(Orange, background, "banda resaltada")
            assertGraphic(TextSecondary, background, "marco de botones y opciones")
        }
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
