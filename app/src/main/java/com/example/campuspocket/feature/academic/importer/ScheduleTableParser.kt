package com.example.campuspocket.feature.academic.importer

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/**
 * Parser del horario del SAEw impreso desde el navegador con "Guardar como PDF"
 * (texto real, una página): único formato soportado. Kotlin puro, sin android:
 * textos con posición -> filas del horario.
 *
 * Reglas (observadas en el PDF real HORARIO_2026B.pdf):
 * - El día se deduce del BORDE IZQUIERDO de la columna, nunca del contenido de la celda
 *   (en texto justificado la última palabra queda cerca del borde derecho y el centro
 *   del fragmento caería en la columna vecina).
 * - Las filas están centradas verticalmente en su número de la columna Nro.; cada una va
 *   del punto medio con la anterior al punto medio con la siguiente (la última es simétrica,
 *   así el pie queda fuera sin mirar su texto).
 * - "Estudiante:" se descarta siempre y nunca se guarda; "Fecha:" del encabezado tampoco.
 */
class ScheduleTableParser @Inject constructor() {

    private enum class ColumnKind {
        NRO, CODIGO, MATERIA, PARAL, AULA, CREDS, NMAT,
        LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO,
        FECHA_I, FECHA_F, CARRERA, OBS, COREQ, PROFESOR
    }

    fun parse(texts: List<PositionedText>): ParsedSchedule {
        if (texts.isEmpty()) {
            return ParsedSchedule(null, null, null, emptyList(), listOf(ParseWarning.NoText))
        }

        // Valores de privacidad: la línea "Estudiante:" se elimina antes de todo lo demás.
        val corpus = discardStudentLines(texts)
        val warnings = mutableListOf<ParseWarning>()
        val rows = mutableListOf<ParsedScheduleRow>()
        val metadata = parseMetadata(corpus.filter { it.page == 1 })

        var lastHeader: Header? = null
        var unsupported = false
        corpus.groupBy { it.page }.toSortedMap().forEach { (page, pageTexts) ->
            val detected = detectHeader(pageTexts)
            if (detected != null) lastHeader = detected
            val header = detected ?: lastHeader
            // Si una página detectada no cumple los encabezados obligatorios no es el SAEw.
            if (header == null || header.isIncomplete()) {
                unsupported = true
                return@forEach
            }

            val layout = ColumnLayout(header.columns)
            val dayKinds = layout.dayKinds()
            val tableTexts = pageTexts.filter { it.y > header.yEnd + LINE_EPS }

            groupRows(tableTexts, layout, header.yEnd + LINE_EPS).forEach { row ->
                val cells = row.texts
                    .groupBy { layout.kindOf(it) }
                    .mapNotNull { (kind, fragments) ->
                        kind?.let { k -> k to fragments.sortedWith(compareBy({ it.y }, { it.x })) }
                    }
                    .toMap()

                val code = joinCell(cells[ColumnKind.CODIGO]).uppercase(Locale.ROOT)
                if (!CODE_PATTERN.matches(code)) {
                    warnings += ParseWarning.UnexpectedCode(row.nro, code)
                }

                val name = stripCodeSuffix(joinCell(cells[ColumnKind.MATERIA]), code)
                val teacher = joinCell(cells[ColumnKind.PROFESOR])
                val slots = dayKinds.flatMap { (kind, day) ->
                    parseSlots(day, cells[kind].orEmpty())
                }
                if (slots.isEmpty()) {
                    warnings += ParseWarning.RowWithoutSchedule(row.nro)
                }

                rows += ParsedScheduleRow(
                    nro = row.nro,
                    code = code,
                    name = smartTitleCase(name),
                    section = joinCell(cells[ColumnKind.PARAL]),
                    room = joinCell(cells[ColumnKind.AULA]).ifBlank { null },
                    credits = joinCell(cells[ColumnKind.CREDS]).toIntOrNull(),
                    teacher = smartTitleCase(teacher),
                    slots = slots
                )
            }
        }

        if (unsupported) {
            warnings += ParseWarning.UnsupportedFormat
        }
        return ParsedSchedule(metadata.termLabel, metadata.start, metadata.end, rows, warnings)
    }

    // ---- Encabezado (una sola línea en este formato) --------------------------------

    /** x = BORDE IZQUIERDO del encabezado (asignación de columna por borde, no por centro). */
    private class Header(val yStart: Float, val yEnd: Float, val columns: List<Pair<ColumnKind, Float>>) {
        fun isIncomplete(): Boolean {
            val kinds = columns.map { it.first }.toSet()
            val days = setOf(
                ColumnKind.LUNES, ColumnKind.MARTES, ColumnKind.MIERCOLES,
                ColumnKind.JUEVES, ColumnKind.VIERNES, ColumnKind.SABADO
            )
            return !kinds.containsAll(setOf(ColumnKind.NRO, ColumnKind.CODIGO, ColumnKind.MATERIA)) ||
                !kinds.containsAll(days)
        }
    }

    private fun detectHeader(pageTexts: List<PositionedText>): Header? {
        clusterByLineY(pageTexts).forEach { line ->
            val found = line.mapNotNull { text ->
                HEADER_TOKENS[normalize(text.text)]?.let { kind -> text to kind }
            }
            // Al pasar de fragmentos a palabras completas, el encabezado tiene una línea única:
            // se exige encontrar las básicas para no aceptar la cabecera del documento.
            if (found.map { it.second }.toSet().containsAll(
                    setOf(ColumnKind.NRO, ColumnKind.CODIGO, ColumnKind.MATERIA)
                )
            ) {
                return Header(
                    yStart = line.minOf { it.y },
                    yEnd = line.maxOf { it.y },
                    columns = found.map { (text, kind) -> kind to text.x }
                )
            }
        }
        return null
    }

    private class ColumnLayout(val columns: List<Pair<ColumnKind, Float>>) {

        private val dayMap = mapOf(
            ColumnKind.LUNES to 1, ColumnKind.MARTES to 2, ColumnKind.MIERCOLES to 3,
            ColumnKind.JUEVES to 4, ColumnKind.VIERNES to 5, ColumnKind.SABADO to 6
        )

        fun dayKinds(): Map<ColumnKind, Int> =
            columns.mapNotNull { (kind, _) -> dayMap[kind]?.let { kind to it } }.toMap()

        /** El fragmento pertenece a la columna cuyo borde X es el mayor <= fragment.x + 1pt. */
        fun kindOf(text: PositionedText): ColumnKind? =
            columns.filter { (_, leftX) -> leftX <= text.x + EDGE_TOLERANCE }
                .maxByOrNull { it.second }
                ?.first
    }

    // ---- Filas ---------------------------------------------------------------------------

    private data class Row(val nro: Int, val texts: List<PositionedText>)

    /**
     * Filas delimitadas por los puntos medios entre los fragmentos \d+ de la columna Nro.
     * La última fila se cierra simétricamente: el pie de página queda fuera por geometría.
     */
    private fun groupRows(
        tableTexts: List<PositionedText>,
        layout: ColumnLayout,
        tableStartY: Float
    ): List<Row> {
        val markers = tableTexts
            .filter { layout.kindOf(it) == ColumnKind.NRO && it.text.trim().matches(NRO_PATTERN) }
            .sortedBy { it.y }
        return markers.mapIndexed { index, marker ->
            val startY = if (index == 0) tableStartY else (markers[index - 1].y + marker.y) / 2f
            val endY = markers.getOrNull(index + 1)?.let { (marker.y + it.y) / 2f }
                ?: marker.y + (marker.y - startY)
            Row(
                nro = marker.text.trim().toInt(),
                texts = tableTexts.filter { it.y >= startY && it.y < endY }
            )
        }
    }

    // ---- Celdas ---------------------------------------------------------------------------

    /** Une todos los fragmentos de la celda con UN espacio: en este formato son palabras completas. */
    private fun joinCell(fragments: List<PositionedText>?): String =
        clusterByLineY(fragments.orEmpty()).joinToString(" ") { line ->
            line.sortedBy { it.x }.joinToString(" ") { it.text }
        }.replace(MULTI_SPACE, " ").trim()

    /** Quita el sufijo "(CÓDIGO)" del texto de la materia (p. ej. "APLICACIONES MÓVILES (ISWD713)"). */
    private fun stripCodeSuffix(text: String, code: String): String {
        if (code.isNotBlank() && text.endsWith("($code)")) {
            return text.removeSuffix("($code)").trim()
        }
        return text.replace(CODE_SUFFIX, "").ifEmpty { text }.trim()
    }

    /** Rangos "7-9" con cualquier guion; los contiguos se fusionan en uno. */
    private fun parseSlots(day: Int, fragments: List<PositionedText>): List<ParsedSlot> {
        val perFragment = fragments.flatMap { fragment -> extractRanges(fragment.text) }
        val ranges = perFragment.ifEmpty { extractRanges(joinCell(fragments)) }
            .sortedBy { it.first }

        val merged = mutableListOf<Pair<Int, Int>>()
        ranges.forEach { range ->
            val last = merged.lastOrNull()
            if (last != null && range.first <= last.second) {
                merged[merged.lastIndex] = last.first to maxOf(last.second, range.second)
            } else {
                merged.add(range)
            }
        }
        return merged.map { (start, end) -> ParsedSlot(day, start * 60, end * 60) }
    }

    private fun extractRanges(text: String): List<Pair<Int, Int>> =
        SLOT_REGEX.findAll(text).mapNotNull { match ->
            val start = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val end = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (start in 0..23 && end in 1..24 && start < end) start to end else null
        }.toList()

    // ---- Metadatos y privacidad ------------------------------------------------------------

    private data class Metadata(val termLabel: String?, val start: LocalDate?, val end: LocalDate?)

    private fun parseMetadata(page1: List<PositionedText>): Metadata {
        var termLabel: String? = null
        var start: LocalDate? = null
        var end: LocalDate? = null

        clusterByLineY(page1).forEach { line ->
            val text = normalize(line.sortedBy { it.x }.joinToString(" ") { it.text })
            if (termLabel == null) {
                TERM_REGEX.find(text)?.let { termLabel = it.groupValues[1].replace(" ", "") }
            }
            if (start == null || end == null) {
                PERIOD_REGEX.find(text)?.let { match ->
                    val startMonth = MONTHS[match.groupValues[1]]
                    val endMonth = MONTHS[match.groupValues[3]]
                    if (startMonth != null && endMonth != null) {
                        start = LocalDate.of(match.groupValues[2].toInt(), startMonth, 1)
                        val endBase = LocalDate.of(match.groupValues[4].toInt(), endMonth, 1)
                        end = endBase.withDayOfMonth(endBase.lengthOfMonth())
                    }
                }
            }
        }
        return Metadata(termLabel, start, end)
    }

    /** Quita del corpus todas las líneas que empiezan por "Estudiante:". */
    private fun discardStudentLines(texts: List<PositionedText>): List<PositionedText> {
        val studentYs = clusterByLineY(texts)
            .filter { line ->
                normalize(line.sortedBy { it.x }.joinToString(" ") { it.text })
                    .startsWith("ESTUDIANTE:")
            }
            .map { line -> line.minOf { it.y } }
        if (studentYs.isEmpty()) return texts
        return texts.filter { text -> studentYs.none { kotlin.math.abs(it - text.y) <= LINE_EPS } }
    }

    // ---- Texto ------------------------------------------------------------------------------

    /** Mayúsculas, sin tildes y sin espacios sobrantes. */
    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .uppercase(Locale.ROOT)

    /**
     * Formato título: de/del/la/el/en/los/las/y en minúscula; primera palabra siempre
     * capitalizada; números romanos (I..X) se respetan en mayúscula ("Física II").
     */
    private fun smartTitleCase(text: String): String {
        val locale = Locale.forLanguageTag("es")
        return text.lowercase(locale).split(" ").joinToString(" ") { word ->
            when {
                word.uppercase(Locale.ROOT) in ROMAN_NUMERALS -> word.uppercase(Locale.ROOT)
                word in SMALL_WORDS -> word
                else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            }
        }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun clusterByLineY(texts: List<PositionedText>): List<List<PositionedText>> {
        val sorted = texts.sortedBy { it.y }
        val lines = mutableListOf<MutableList<PositionedText>>()
        sorted.forEach { text ->
            val current = lines.lastOrNull()
            if (current == null || kotlin.math.abs(text.y - current.last().y) > LINE_EPS) {
                lines.add(mutableListOf(text))
            } else {
                current.add(text)
            }
        }
        return lines
    }

    companion object {
        private const val LINE_EPS = 2.0f

        /** Holgura para la asignación por borde izquierdo (decimales del PDF). */
        private const val EDGE_TOLERANCE = 1.0f

        private val MULTI_SPACE = Regex("""\s+""")
        private val DIACRITICS = Regex("""\p{Mn}+""")
        private val NRO_PATTERN = Regex("""^\d+$""")
        private val CODE_PATTERN = Regex("""^[A-ZÁÉÍÓÚÑ]{3,4}\d{3}$""")
        private val CODE_SUFFIX = Regex("""\s*\([A-ZÁÉÍÓÚÑ0-9]+\)$""")
        // Guiones de rango: - (guion), ‐ (U+2010), ‑ (U+2011), – (U+2013), — (U+2014), − (U+2212).
        private val SLOT_REGEX = Regex("""(\d{1,2})[\-‐‑–—−](\d{1,2})""")
        private val TERM_REGEX = Regex("""REGISTRO\s+DE\s+HORARIOS\s+(\d{4}\s*-\s*[AB])""")
        private val PERIOD_REGEX = Regex(
            """PERIODO\s+ACTUAL\s*:\s*([A-Z]+)\s*/\s*(\d{4})\s*-\s*([A-Z]+)\s*/\s*(\d{4})"""
        )

        private val SMALL_WORDS = setOf("de", "del", "la", "el", "en", "los", "las", "y")

        private val ROMAN_NUMERALS = setOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

        private val MONTHS = mapOf(
            "ENERO" to 1, "FEBRERO" to 2, "MARZO" to 3, "ABRIL" to 4,
            "MAYO" to 5, "JUNIO" to 6, "JULIO" to 7, "AGOSTO" to 8,
            "SEPTIEMBRE" to 9, "SETIEMBRE" to 9, "OCTUBRE" to 10,
            "NOVIEMBRE" to 11, "DICIEMBRE" to 12
        )

        /** Encabezados del SAEw como palabra completa normalizada (sin tildes). */
        private val HEADER_TOKENS: Map<String, ColumnKind> = mapOf(
            "NRO." to ColumnKind.NRO,
            "CODIGO" to ColumnKind.CODIGO,
            "MATERIA" to ColumnKind.MATERIA,
            "PARAL" to ColumnKind.PARAL,
            "AULA" to ColumnKind.AULA,
            "CREDS" to ColumnKind.CREDS,
            "N.MAT" to ColumnKind.NMAT,
            "LUNES" to ColumnKind.LUNES,
            "MARTES" to ColumnKind.MARTES,
            "MIERCOLES" to ColumnKind.MIERCOLES,
            "JUEVES" to ColumnKind.JUEVES,
            "VIERNES" to ColumnKind.VIERNES,
            "SABADO" to ColumnKind.SABADO,
            "FECHAI" to ColumnKind.FECHA_I,
            "FECHAF" to ColumnKind.FECHA_F,
            "CARRERA" to ColumnKind.CARRERA,
            "OBS" to ColumnKind.OBS,
            "COREQ" to ColumnKind.COREQ,
            "PROFESOR" to ColumnKind.PROFESOR
        )
    }
}
