package com.example.campuspocket.feature.notes.domain

/**
 * Acciones puras de la barra de formato del editor: cada una recibe el texto completo
 * y el rango de selección, y devuelve texto nuevo + nueva selección. Sin Android.
 */
object MarkdownActions {

    data class EditResult(val text: String, val selectionStart: Int, val selectionEnd: Int)

    /** Envuelve la selección (o la palabra actual si no hay selección) con `wrapper`. */
    fun wrap(text: String, start: Int, end: Int, wrapper: String): EditResult {
        if (start == end) {
            val inserted = text.substring(0, start) + wrapper + wrapper + text.substring(end)
            return EditResult(inserted, start + wrapper.length, start + wrapper.length)
        }
        val newText = text.substring(0, start) + wrapper + text.substring(start, end) + wrapper + text.substring(end)
        val newStart = start + wrapper.length
        return EditResult(newText, newStart, newStart + (end - start))
    }

    /** Añade un prefijo al inicio de cada línea dentro de la selección (o línea actual). */
    fun prefixLines(text: String, start: Int, end: Int, prefix: String): EditResult {
        val realStart = if (start > text.length) text.length else start.coerceAtLeast(0)
        val realEnd = if (end > text.length) text.length else end.coerceAtLeast(realStart)

        val lineStart = text.lastIndexOf('\n', (realStart - 1).coerceIn(-1, text.length - 1))
            .takeIf { it >= 0 }?.let { it + 1 } ?: 0
        val lineEnd = text.indexOf('\n', realEnd).takeIf { it >= 0 } ?: text.length

        val block = text.substring(lineStart, lineEnd)
        val lines = block.split("\n").toMutableList()
        lines.replaceAll { if (it.isEmpty()) prefix else prefix + " " + it }
        val newBlock = lines.joinToString("\n")
        val newText = text.substring(0, lineStart) + newBlock + text.substring(lineEnd)
        return EditResult(newText, lineStart + 1, lineStart + 1 + newBlock.length)
    }
}

/**
 * Parser mínimo de Markdown de la app. Devuelve una lista plana de bloques
 * ya listos para renderizar (título, negrita, cursiva, código, casillas, listas).
 */
object MarkdownParser {

    sealed interface Block {
        val rawStart: Int
        val rawEnd: Int
    }

    data class Heading(val level: Int, val text: String, override val rawStart: Int, override val rawEnd: Int) : Block
    data class Paragraph(val text: String, override val rawStart: Int, override val rawEnd: Int) : Block
    data class UnorderedListItem(val text: String, override val rawStart: Int, override val rawEnd: Int) : Block
    data class OrderedListItem(val number: Int, val text: String, override val rawStart: Int, override val rawEnd: Int) : Block
    data class CodeBlock(val text: String, override val rawStart: Int, override val rawEnd: Int) : Block
    data class Checkbox(val checked: Boolean, val text: String, override val rawStart: Int, override val rawEnd: Int) : Block

    /** Parsea el Markdown a bloques. Los offsets son índices del texto fuente. */
    fun parse(markdown: String): List<Block> {
        val out = mutableListOf<Block>()
        var i = 0
        while (i < markdown.length) {
            val lineEnd = markdown.indexOf('\n', i).takeIf { it >= 0 } ?: markdown.length
            val line = markdown.substring(i, lineEnd)

            // Bloque de código ``` ... ```
            if (line.startsWith("```")) {
                var j = lineEnd + 1
                val content = StringBuilder()
                while (j < markdown.length) {
                    val eol = markdown.indexOf('\n', j).takeIf { it >= 0 } ?: markdown.length
                    val inner = markdown.substring(j, eol)
                    if (inner.startsWith("```")) {
                        out += CodeBlock(content.toString().trimEnd('\n'), i, eol + inner.length.coerceAtLeast(0))
                        i = eol + 1
                        break
                    }
                    content.append(inner).append('\n')
                    j = eol + 1
                }
                if (i < lineEnd + 1) i = lineEnd + 1
                continue
            }

            when {
                line.startsWith("### ") -> out += Heading(3, line.removePrefix("### ").trim(), i, lineEnd)
                line.startsWith("## ") -> out += Heading(2, line.removePrefix("## ").trim(), i, lineEnd)
                line.startsWith("# ") -> out += Heading(1, line.removePrefix("# ").trim(), i, lineEnd)
                line.trim().matches(Regex("""^- \[[ xX]\] .*""")) -> {
                    val checked = line.contains("[x", ignoreCase = true)
                    out += Checkbox(checked, line.substringAfter(']').trim(), i, lineEnd)
                }
                line.trim().startsWith("- ") || line.trim().startsWith("* ") ->
                    out += UnorderedListItem(line.trim().drop(2), i, lineEnd)
                line.trim().matches(Regex("""^\d+\. .*""")) -> {
                    val num = line.trim().substringBefore('.').toInt()
                    out += OrderedListItem(num, line.trim().substringAfter('.').trim(), i, lineEnd)
                }
                line.isBlank() -> { /* salta */ }
                else -> out += Paragraph(line, i, lineEnd)
            }
            i = lineEnd + 1
        }
        return out
    }

    /** Cambia el estado de la casilla cuya línea contiene el índice `blockRawStart` del texto fuente. */
    fun toggleCheckbox(markdown: String, blockRawStart: Int): String {
        val start = markdown.lastIndexOf('\n', (blockRawStart - 1).coerceIn(-1, markdown.length - 1))
            .takeIf { it >= 0 }?.let { it + 1 } ?: 0
        val end = markdown.indexOf('\n', blockRawStart).takeIf { it >= 0 } ?: markdown.length
        val line = markdown.substring(start, end)
        return if (line.contains("[x", ignoreCase = true)) {
            markdown.substring(0, start) + line.replaceFirst(Regex("""\[[xX]\]"""), "[ ]") + markdown.substring(end)
        } else {
            markdown.substring(0, start) + line.replaceFirst(Regex("""\[ \]"""), "[x]") + markdown.substring(end)
        }
    }
}

/** Búsqueda por LIKE sin romper la consulta: escapa %, _ y el propio '\' con ESCAPE '\'. */
fun escapeLikePattern(userText: String): String {
    val out = StringBuilder()
    for (c in userText) {
        if (c == '%' || c == '_' || c == '\\') out.append('\\')
        out.append(c)
    }
    return out.toString()
}
