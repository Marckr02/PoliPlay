package com.example.campuspocket.feature.academic.importer

import java.util.Locale

/** Lee/escribe fixtures TSV de textos posicionados: `página<TAB>x<TAB>y<TAB>ancho<TAB>texto`. */
object FixtureIo {

    fun fromTsv(content: String): List<PositionedText> =
        content.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split("\t")
                PositionedText(
                    page = parts[0].toInt(),
                    x = parts[1].toFloat(),
                    y = parts[2].toFloat(),
                    width = parts[3].toFloat(),
                    text = parts[4]
                )
            }
            .toList()

    fun loadResource(resourceName: String): List<PositionedText> {
        val stream = requireNotNull(
            Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)
        ) { "Fixture no encontrado en test resources: $resourceName" }
        return stream.bufferedReader().use { fromTsv(it.readText()) }
    }

    fun toTsv(texts: List<PositionedText>): String {
        val sb = StringBuilder()
        texts.forEach { t ->
            val text = t.text.replace("\t", " ").replace("\n", " ")
            sb.append(
                String.format(
                    Locale.ROOT,
                    "%d\t%.2f\t%.2f\t%.2f\t%s%n",
                    t.page, t.x, t.y, t.width, text
                )
            )
        }
        return sb.toString()
    }
}
