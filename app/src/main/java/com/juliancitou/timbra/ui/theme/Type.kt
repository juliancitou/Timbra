package com.juliancitou.timbra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.juliancitou.timbra.R

/**
 * Cascadia Mono (Microsoft, licencia OFL), sucesora libre de Consolas. Es una fuente variable:
 * un solo archivo da todos los grosores ajustando el eje `wght`.
 */
private fun cascadiaMono(weight: FontWeight) = Font(
    resId = R.font.cascadia_mono,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val CascadiaMono = FontFamily(
    cascadiaMono(FontWeight.Light),
    cascadiaMono(FontWeight.Normal),
    cascadiaMono(FontWeight.Medium),
    cascadiaMono(FontWeight.SemiBold),
    cascadiaMono(FontWeight.Bold),
)

private val Base = Typography()

/** En monoespaciada cada letra ocupa lo mismo, así que se quita el espaciado extra de Material. */
private fun TextStyle.mono(weight: FontWeight = FontWeight.Normal) =
    copy(fontFamily = CascadiaMono, fontWeight = weight, letterSpacing = 0.sp)

val Typography = Typography(
    displayLarge = Base.displayLarge.mono(FontWeight.SemiBold),
    displayMedium = Base.displayMedium.mono(FontWeight.SemiBold).copy(fontSize = 36.sp, lineHeight = 44.sp),
    displaySmall = Base.displaySmall.mono(FontWeight.SemiBold).copy(fontSize = 30.sp, lineHeight = 38.sp),
    headlineLarge = Base.headlineLarge.mono(FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.mono(FontWeight.SemiBold).copy(fontSize = 23.sp, lineHeight = 30.sp),
    headlineSmall = Base.headlineSmall.mono(FontWeight.SemiBold).copy(fontSize = 20.sp),
    titleLarge = Base.titleLarge.mono(FontWeight.SemiBold).copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = Base.titleMedium.mono(FontWeight.SemiBold).copy(fontSize = 15.sp),
    titleSmall = Base.titleSmall.mono(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.mono().copy(fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium = Base.bodyMedium.mono().copy(fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = Base.bodySmall.mono(),
    labelLarge = Base.labelLarge.mono(FontWeight.SemiBold).copy(fontSize = 13.sp),
    labelMedium = Base.labelMedium.mono(FontWeight.Medium),
    labelSmall = Base.labelSmall.mono(FontWeight.Medium),
)

/**
 * Dígitos de ancho fijo (`tnum`) para valores en dB y Hz. Con Cascadia Mono ya lo son; se
 * conserva por si algún texto usa otra fuente.
 */
fun TextStyle.withTabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")
