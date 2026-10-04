package com.example.campuspocket.feature.academic.importer

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.junit.Test
import java.io.File

/**
 * Arnés manual de diagnóstico del importador. NO es parte de la suite permanente:
 * solo corre si se define la propiedad `-Dcampuspocket.pdf.fixture=<ruta al PDF real>`.
 *
 * Escribe en tools/pdf-dumps/ (ignorado por git) y NUNCA escribe líneas con
 * "Estudiante:" (privacidad; el parser también las descarta antes de procesar).
 */
class PdfDebugHarnessTest {

    @Test
    fun dumpDelPdfDeclaradoEnPropiedad() {
        val path = System.getProperty("campuspocket.pdf.fixture") ?: return
        val file = File(path)
        if (!file.exists()) {
            println("pdf no encontrado, se omite: ${file.absolutePath}")
            return
        }

        val texts = extract(file)
        val dumped = texts.filterNot { isStudentFragment(texts, it) }
        val parsed = ScheduleTableParser().parse(dumped)

        val outDir = File("tools/pdf-dumps").also(File::mkdirs)
        File(outDir, file.nameWithoutExtension + ".dump.txt").writeText(buildDump(dumped, parsed))
        File(outDir, file.nameWithoutExtension + ".tsv").writeText(FixtureIo.toTsv(dumped))
        println("volcado escrito en ${outDir.absolutePath}")
    }

    // ---- Extracción (pdfbox de escritorio: el tom-roush de Android no corre en JVM) ----

    private data class Glyph(val page: Int, val x: Float, val y: Float, val w: Float, val text: String)

    private fun extract(file: File): List<PositionedText> {
        val glyphs = mutableListOf<Glyph>()
        PDDocument.load(file).use { doc ->
            val stripper = object : PDFTextStripper() {
                override fun processTextPosition(text: TextPosition) {
                    super.processTextPosition(text)
                    if (!text.unicode.isNullOrBlank()) {
                        glyphs.add(Glyph(0, text.xDirAdj, text.yDirAdj, text.widthDirAdj, text.unicode))
                    }
                }
            }
            stripper.sortByPosition = true
            for (page in 1..doc.numberOfPages) {
                val pageStart = glyphs.size
                stripper.startPage = page
                stripper.endPage = page
                stripper.getText(doc)
                for (i in pageStart until glyphs.size) {
                    glyphs[i] = glyphs[i].copy(page = page)
                }
            }
        }
        return group(glyphs)
    }

    /** Misma agrupación que GlyphCollector en la app (hueco > 1.5 pt separa fragmento). */
    private fun group(glyphs: List<Glyph>): List<PositionedText> {
        val fragments = mutableListOf<MutableList<Glyph>>()
        glyphs.forEach { g ->
            val lastGroup = fragments.lastOrNull()
            val last = lastGroup?.lastOrNull()
            val sameLine = last != null && last.page == g.page && kotlin.math.abs(g.y - last.y) <= 2.0f
            val gap = if (last == null) 0f else g.x - (last.x + last.w)
            if (!sameLine || gap > 1.5f) {
                fragments.add(mutableListOf(g))
            } else {
                lastGroup.add(g)
            }
        }
        return fragments.map { gs ->
            PositionedText(
                page = gs.first().page,
                x = gs.minOf { it.x },
                y = gs.minOf { it.y },
                width = gs.maxOf { it.x + it.w } - gs.minOf { it.x },
                text = gs.joinToString("") { it.text }
            )
        }
    }

    /** El fragmento cae EN una línea que empieza por "Estudiante:". */
    private fun isStudentFragment(texts: List<PositionedText>, target: PositionedText): Boolean {
        // Líneas por página y luego localizadas por textos con "Estudiante:".
        val studentLineYs = texts.groupBy { it.page }.values.flatMap { pageTexts ->
            pageTexts.groupBy { it.y }.values
                .filter { line ->
                    line.sortedBy { it.x }.joinToString(" ") { it.text }.trim().startsWith("Estudiante:")
                }
                .map { line -> line.first().y }
        }
        return studentLineYs.any { it == target.y }
    }

    private fun buildDump(texts: List<PositionedText>, parsed: ParsedSchedule): String {
        val sb = StringBuilder()
        dump@ for (page in texts.map { it.page }.distinct().sorted()) {
            sb.appendLine("=== PAGINA $page ===")
            texts.filter { it.page == page }
                .sortedWith(compareBy({ it.y }, { it.x }))
                .forEach { sb.appendLine("y=${it.y} x=${it.x} w=${it.width} | ${it.text}") }
        }
        sb.appendLine("=== PARSER ===")
        sb.appendLine("termLabel=${parsed.termLabel} start=${parsed.startDate} end=${parsed.endDate}")
        parsed.warnings.forEach { sb.appendLine("warn: $it") }
        sb.appendLine("rows=${parsed.rows.size}")
        parsed.rows.forEach { r -> sb.appendLine("${r.nro} ${r.code} ${r.name} ${r.teacher} ${r.slots}") }
        return sb.toString()
    }
}
