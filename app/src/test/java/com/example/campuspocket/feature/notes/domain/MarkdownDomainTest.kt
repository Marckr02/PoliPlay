package com.example.campuspocket.feature.notes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas puras del editor Markdown: barra, parser, casillas y escape LIKE. */
class MarkdownDomainTest {

    @Test
    fun `wrap envuelve la seleccion`() {
        val r = MarkdownActions.wrap("hola mundo", 5, 10, "**")
        assertEquals("hola **mundo**", r.text)
        assertEquals(7, r.selectionStart)
        assertEquals(12, r.selectionEnd)
    }

    @Test
    fun `wrap sin seleccion inserta el par de marcadores`() {
        val r = MarkdownActions.wrap("hola", 4, 4, "**")
        assertEquals("hola****", r.text)
        assertEquals(6, r.selectionStart)
    }

    @Test
    fun `prefixLines antepone a cada linea del rango`() {
        val r = MarkdownActions.prefixLines("a\nb\nc", 0, 5, "-")
        assertEquals("- a\n- b\n- c", r.text)
        assertEquals(1, r.selectionStart)
    }

    @Test
    fun `parser trocea encabezados listas codigo y casillas`() {
        val text = "# Titulo\n**negrita** y *cursiva*\n- uno\n1. dos\n- [ ] pendiente\n- [x] hecha\n`inline`\n```\nbloque\ncodigo\n```"
        val blocks = MarkdownParser.parse(text)

        assertEquals(MarkdownParser.Heading(1, "Titulo", 0, 8), blocks[0])
        assertTrue(blocks.any { it is MarkdownParser.UnorderedListItem && it.text == "uno" })
        assertTrue(blocks.any { it is MarkdownParser.OrderedListItem && it.number == 1 })
        assertTrue(blocks.any { it is MarkdownParser.Checkbox && !it.checked })
        assertTrue(blocks.any { it is MarkdownParser.Checkbox && it.checked })
        assertTrue(blocks.any { it is MarkdownParser.CodeBlock && it.text == "bloque\ncodigo" })
    }

    @Test
    fun `casilla se alterna en el texto`() {
        val text = "una\n- [ ] pendiente\notra"
        val block = MarkdownParser.parse(text).single { it is MarkdownParser.Checkbox }
        val toggled = MarkdownParser.toggleCheckbox(text, block.rawStart)
        assertTrue(toggled.contains("- [x] pendiente"))
        val back = MarkdownParser.toggleCheckbox(toggled, block.rawStart)
        assertTrue(back.contains("- [ ] pendiente"))
    }

    @Test
    fun `escapeLikePattern escapa por ciento guion y barra`() {
        assertEquals("100\\%", escapeLikePattern("100%"))
        assertEquals("a\\_b", escapeLikePattern("a_b"))
        assertEquals("c\\\\d", escapeLikePattern("c\\d"))
        assertEquals("50\\% y 1\\_2", escapeLikePattern("50% y 1_2"))
    }
}
