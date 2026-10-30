package com.juliancitou.timbra.ui.theme

enum class CodeTokenKind { Plain, NumberLiteral, StringLiteral }

data class CodeToken(val text: String, val kind: CodeTokenKind)

/** Números (con decimales) y texto entre comillas rectas o tipográficas. */
private val Literals = Regex("""\d+(?:[.,]\d+)?|"[^"]*"|“[^”]*”""")

/**
 * Divide una línea de texto normal en partes para colorearla como código: los números se tratan
 * como literales numéricos y lo que va entre comillas, como cadenas. El texto no cambia; unir
 * las partes devuelve la línea original.
 */
fun codeTokens(line: String): List<CodeToken> {
    val tokens = mutableListOf<CodeToken>()
    var cursor = 0
    Literals.findAll(line).forEach { match ->
        if (match.range.first > cursor) {
            tokens += CodeToken(line.substring(cursor, match.range.first), CodeTokenKind.Plain)
        }
        val kind = if (match.value.first().isDigit()) CodeTokenKind.NumberLiteral else CodeTokenKind.StringLiteral
        tokens += CodeToken(match.value, kind)
        cursor = match.range.last + 1
    }
    if (cursor < line.length) tokens += CodeToken(line.substring(cursor), CodeTokenKind.Plain)
    return tokens
}
