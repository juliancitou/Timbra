package com.juliancitou.timbra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeTokensTest {

    private fun literals(line: String, kind: CodeTokenKind) =
        codeTokens(line).filter { it.kind == kind }.map { it.text }

    @Test
    fun numbersAreNumberLiterals() {
        assertEquals(listOf("10", "15"), literals("Reserva de 10 a 15 minutos.", CodeTokenKind.NumberLiteral))
        assertEquals(listOf("8.4"), literals("Tienes un pico en 8.4 kHz.", CodeTokenKind.NumberLiteral))
    }

    @Test
    fun quotedTextIsAStringLiteral() {
        assertEquals(listOf("\"S\""), literals("Aquí vive la \"S\" de las voces.", CodeTokenKind.StringLiteral))
        assertEquals(listOf("“lodo”"), literals("Hay “lodo” en los graves.", CodeTokenKind.StringLiteral))
    }

    @Test
    fun numbersInsideQuotesStayInTheString() {
        val tokens = codeTokens("Elige \"Banda 7\" ahora")
        assertEquals(listOf("\"Banda 7\""), tokens.filter { it.kind == CodeTokenKind.StringLiteral }.map { it.text })
        assertEquals(emptyList<String>(), tokens.filter { it.kind == CodeTokenKind.NumberLiteral }.map { it.text })
    }

    @Test
    fun joiningTheTokensGivesBackTheLine() {
        val line = "Mueve el control hasta que suenen igual. Son 25 bandas y la \"S\" está en 8.4 kHz."
        assertEquals(line, codeTokens(line).joinToString("") { it.text })
    }

    @Test
    fun plainTextIsASingleToken() {
        assertEquals(listOf(CodeToken("Sin literales", CodeTokenKind.Plain)), codeTokens("Sin literales"))
    }
}
