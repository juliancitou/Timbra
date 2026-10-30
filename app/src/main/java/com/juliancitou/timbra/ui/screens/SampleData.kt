package com.juliancitou.timbra.ui.screens

import java.time.LocalDate

/** Datos de ejemplo de la Fase 1; se reemplazan por mediciones reales en las Fases 3 y 4. */
object SampleData {
    val curveDb = listOf(
        -1.5f, -1.5f, -1f, -0.5f, 0f, 0f, -1f, -2f, -2.5f, -2.5f, -2f, -1f, -0.5f,
        0f, 0.5f, 1f, 1.5f, 1f, 0f, -1.5f, -3.5f, -4.5f, -3.5f, -2.5f, -2.5f,
    )

    val estimatedBandIndices = setOf(0, 1, 2)

    val measurements = listOf(
        SampleMeasurement(name = "Mi ecualización", date = LocalDate.of(2026, 10, 29), usesLdac = false),
        SampleMeasurement(name = "Con LDAC", date = LocalDate.of(2026, 10, 20), usesLdac = true),
    )
}

data class SampleMeasurement(val name: String, val date: LocalDate, val usesLdac: Boolean)
