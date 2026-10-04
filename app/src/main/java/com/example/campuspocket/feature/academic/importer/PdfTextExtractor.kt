package com.example.campuspocket.feature.academic.importer

import android.content.Context
import com.example.campuspocket.core.util.LogUtil
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capa 1 del importador (sección 8.2 del spec): PDF -> fragmentos de texto con posición.
 * Solo esta clase conoce PdfBox; el parseo propiamente dicho está en [ScheduleTableParser].
 */
interface PdfTextExtractor {
    fun extract(input: InputStream): List<PositionedText>
}

@Singleton
class PdfBoxTextExtractor @Inject constructor(
    @ApplicationContext context: Context
) : PdfTextExtractor {

    init {
        // PdfBox-Android lo requiere una vez por proceso (antes de cargar documentos).
        ensurePdfBoxInitialized(context)
    }

    override fun extract(input: InputStream): List<PositionedText> {
        val collector = GlyphCollector()
        PDDocument.load(input).use { document ->
            val stripper = GlyphStripper(collector)
            stripper.sortByPosition = true
            for (page in 1..document.numberOfPages) {
                collector.beginPage(page)
                stripper.startPage = page
                stripper.endPage = page
                stripper.getText(document) // invoca processTextPosition por cada glifo
            }
        }
        return collector.fragments()
    }

    /** Glifo tal como llega de PdfBox (posición ya ajustada por el cropbox). */
    private data class RawGlyph(val page: Int, val x: Float, val y: Float, val width: Float, val text: String)

    /** Agrupa glifos consecutivos en fragmentos (misma línea, sin hueco grande). */
    private class GlyphCollector {
        private val fragments = mutableListOf<MutableList<RawGlyph>>()
        private var currentPage = 0

        fun beginPage(page: Int) {
            currentPage = page
        }

        fun add(x: Float, y: Float, width: Float, text: String) {
            if (text.isBlank()) return
            val lastLine = fragments.lastOrNull()
            val last = lastLine?.lastOrNull()
            val sameLine = last != null && last.page == currentPage &&
                kotlin.math.abs(y - last.y) <= LINE_Y_TOLERANCE
            val gap = if (last == null) 0f else x - (last.x + last.width)
            if (!sameLine || gap > FRAGMENT_GAP) {
                fragments.add(mutableListOf(RawGlyph(currentPage, x, y, width, text)))
            } else {
                lastLine.add(RawGlyph(currentPage, x, y, width, text))
            }
        }

        fun fragments(): List<PositionedText> = fragments.map { glyphs ->
            val first = glyphs.first()
            val endX = glyphs.maxOf { it.x + it.width }
            PositionedText(
                page = first.page,
                x = glyphs.minOf { it.x },
                y = glyphs.minOf { it.y },
                width = endX - glyphs.minOf { it.x },
                text = glyphs.joinToString("") { it.text }
            )
        }
    }

    private inner class GlyphStripper(
        private val collector: GlyphCollector
    ) : PDFTextStripper() {
        override fun processTextPosition(text: TextPosition) {
            super.processTextPosition(text)
            val unicode = text.unicode
            if (unicode.isNotBlank()) {
                collector.add(text.xDirAdj, text.yDirAdj, text.widthDirAdj, unicode)
            }
        }
    }

    companion object {
        /** Dos glifos de la misma línea se separan de fragmento si el hueco supera esto (espacios ≈ 3 pt). */
        private const val FRAGMENT_GAP = 1.5f
        private const val LINE_Y_TOLERANCE = 2.0f

        @Volatile
        private var pdfBoxReady = false

        private fun ensurePdfBoxInitialized(context: Context) {
            if (!pdfBoxReady) {
                synchronized(this) {
                    if (!pdfBoxReady) {
                        PDFBoxResourceLoader.init(context.applicationContext)
                        pdfBoxReady = true
                        LogUtil.d("PDFBox-Android inicializado", tag = "Import")
                    }
                }
            }
        }
    }
}
