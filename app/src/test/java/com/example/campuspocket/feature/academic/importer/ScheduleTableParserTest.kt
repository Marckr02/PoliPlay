package com.example.campuspocket.feature.academic.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas del parser del formato SAEw ("Guardar como PDF" del navegador).
 * El fixture es la extracción REAL del HORARIO_2026B.pdf, sanitizada (sin la
 * línea "Estudiante:") y comprometida a disco en app/src/test/resources/fixtures/.
 * Lo sintético que hay abajo SOLO cubre casos de rechazo (no reproduce el PDF viejo).
 */
class ScheduleTableParserTest {

    private val parser = ScheduleTableParser()

    private fun fixture(): List<PositionedText> =
        FixtureIo.loadResource("fixtures/saew-horario-2026b.tsv")

    // ---- Fixture real ---------------------------------------------------------------

    @Test
    fun `el fixture real produce 11 filas, 7 materias y 13 sesiones con 0 avisos`() {
        val parsed = parser.parse(fixture())

        assertEquals(emptyList<ParseWarning>(), parsed.warnings)
        assertEquals(11, parsed.rows.size)
        assertEquals(7, parsed.rows.distinctBy { it.code to it.section }.size)
        assertEquals(13, parsed.rows.sumOf { it.slots.size })
        assertEquals("2026-B", parsed.termLabel)
        assertEquals(LocalDate.of(2026, 8, 1), parsed.startDate)
        assertEquals(LocalDate.of(2027, 2, 28), parsed.endDate)
    }

    @Test
    fun `reproduce exactamente la tabla del spec 8_5`() {
        val parsed = parser.parse(fixture())

        fun slotsOf(code: String) = parsed.rows.filter { it.code == code }
            .flatMap { it.slots }
            .sortedWith(compareBy({ it.dayOfWeek }, { it.startMinute }))
        fun rowOf(nro: Int) = parsed.rows.first { it.nro == nro }

        assertEquals(
            listOf(ParsedSlot(1, 420, 540), ParsedSlot(2, 660, 780)),
            slotsOf("ISWD713")
        )
        assertEquals(listOf(ParsedSlot(1, 660, 780)), slotsOf("ADMD700"))
        assertEquals(
            listOf(ParsedSlot(2, 420, 540), ParsedSlot(5, 420, 540)),
            slotsOf("ISWD743")
        )
        assertEquals(
            listOf(ParsedSlot(2, 540, 660), ParsedSlot(4, 540, 660)),
            slotsOf("ISWD732")
        )
        assertEquals(
            listOf(ParsedSlot(3, 420, 540), ParsedSlot(4, 420, 540)),
            slotsOf("ISWD762")
        )
        assertEquals(
            listOf(ParsedSlot(3, 540, 660), ParsedSlot(5, 540, 660)),
            slotsOf("ISWD723")
        )
        assertEquals(
            listOf(ParsedSlot(3, 660, 780), ParsedSlot(5, 660, 780)),
            slotsOf("ISWD752")
        )

        // detalles de fila (aula y créditos según §8.5)
        rowOf(1).let { assertEquals("E20/P3/E003", it.room); assertEquals(3, it.credits) }
        rowOf(2).let { assertEquals("E20/P3/E005", it.room); assertEquals(2, it.credits) }
        rowOf(3).let { assertEquals("E39/PB/E028", it.room); assertEquals(0, it.credits) }
        rowOf(4).let { assertEquals("E20/P3/E003", it.room); assertEquals(3, it.credits) }
        rowOf(5).let { assertEquals("E21/PB2/E039", it.room); assertNull(it.credits) }
        rowOf(6).let { assertEquals("E20/P3/E004", it.room); assertEquals(0, it.credits) }
        rowOf(7).let { assertEquals("E20/P3/E010", it.room); assertEquals(3, it.credits) }
        rowOf(8).let { assertEquals("E20/P3/E010", it.room); assertEquals(0, it.credits) }
        rowOf(9).let { assertEquals("E20/P5/E008", it.room); assertEquals(2, it.credits) }
        rowOf(10).let { assertEquals("E20/P5/E004", it.room); assertEquals(0, it.credits) }
        rowOf(11).let { assertEquals("E20/P5/E010", it.room); assertEquals(2, it.credits) }
    }

    @Test
    fun `nombres y profesores en formato titulo, paralelo intacto`() {
        val parsed = parser.parse(fixture())

        val nombrePorCodigo = parsed.rows.associate { it.code to it.name }
        assertEquals("Aplicaciones Móviles", nombrePorCodigo["ISWD713"])
        assertEquals("Automatización de Procesos", nombrePorCodigo["ISWD762"])
        assertEquals("Business Intelligence", nombrePorCodigo["ISWD743"])
        assertEquals("Emprendimiento", nombrePorCodigo["ADMD700"])
        assertEquals("Interacción Humano Computador", nombrePorCodigo["ISWD723"])
        assertEquals("Usabilidad y Accesibilidad", nombrePorCodigo["ISWD732"])
        assertEquals("Verificación y Validación de Software", nombrePorCodigo["ISWD752"])

        val profesorPorNro = { nro: Int -> parsed.rows.first { it.nro == nro }.teacher }
        assertEquals("Eguez Sarzosa Vicente Adrian", profesorPorNro(1))
        assertEquals("Navarrete Arroyo Pablo Steve", profesorPorNro(2))
        assertEquals("Martinez Mosquera Silvia Diana", profesorPorNro(4))
        assertEquals("Mancheno Vaca Carlos Alberto", profesorPorNro(5))
        assertEquals("Galindo Losada Julian Andres", profesorPorNro(6))
        assertEquals("Calle Jimenez Tania Elizabeth", profesorPorNro(8))
        assertEquals("Anchundia Valencia Carlos Eduardo", profesorPorNro(10))

        // El paralelo no sufre aunque el nombre use "Y"/"DE" pegado al borde (formato justificado).
        listOf(8, 9, 10, 11).forEach { nro ->
            assertEquals("GR2SW", parsed.rows.first { it.nro == nro }.section)
        }
    }

    @Test
    fun `la linea Estudiante se descarta y nunca llega a la salida`() {
        val contaminated = fixture() + listOf(
            PositionedText(1, 84.8f, 221.17f, 26.93f, "Estudiante:"),
            PositionedText(1, 113.4f, 221.17f, 41.0f, "000000000-ALUMNO"),
            PositionedText(1, 156.0f, 221.17f, 20.6f, "INVEN"),
            PositionedText(1, 178.3f, 221.17f, 17.3f, "TADO")
        )
        val parsed = parser.parse(contaminated)
        val salida = parsed.rows.joinToString("|") { "${it.name};${it.teacher}" }
        assertFalse(salida.contains("ALUMNO"))
        assertFalse(salida.contains("INVEN"))
        assertFalse(salida.contains("TADO"))
        assertFalse(salida.contains("Estudiante", ignoreCase = true))
    }

    @Test
    fun `un pie con cualquier texto queda fuera de la ultima fila`() {
        val withFooter = fixture() + listOf(
            PositionedText(1, 84.8f, 545.0f, 18.4f, "TOTALES"),
            PositionedText(1, 124.0f, 545.0f, 25.9f, "REVISION"),
            PositionedText(1, 498.2f, 545.0f, 21.4f, "DEPARTAMENTO")
        )
        val parsed = parser.parse(withFooter)

        assertEquals(11, parsed.rows.size)
        val last = parsed.rows.last()
        assertEquals("ISWD752", last.code)
        assertEquals("Verificación y Validación de Software", last.name)
        assertEquals("Anchundia Valencia Carlos Eduardo", last.teacher)
        assertEquals(listOf(ParsedSlot(3, 660, 780)), last.slots)
    }

    // ---- Casos sintéticos mínimos (rechazo y tolerancias) ---------------------------

    @Test
    fun `sin texto sale la advertencia NoText`() {
        val parsed = parser.parse(emptyList())
        assertTrue(parsed.rows.isEmpty())
        assertEquals(listOf(ParseWarning.NoText), parsed.warnings)
    }

    @Test
    fun `cabeceras truncadas (formato viejo) salen como formato no soportado`() {
        // Así se veía el PDF viejo: cabeceras partidas por el ancho de la celda.
        val texts = listOf(
            PositionedText(1, 40f, 20f, 36f, "REGISTRO DE HORARIOS 2026-B"),
            PositionedText(1, 40f, 36f, 36f, "PERÍODO ACTUAL: AGOSTO/2026 - FEBRERO/2027"),
            PositionedText(1, 10f, 100f, 20f, "Nro."),
            PositionedText(1, 40f, 100f, 20f, "Códig"),
            PositionedText(1, 40f, 106f, 4f, "o"),
            PositionedText(1, 75f, 100f, 24f, "Materi"),
            PositionedText(1, 75f, 106f, 4f, "a"),
            PositionedText(1, 110f, 100f, 20f, "Lunes"),
            PositionedText(1, 10f, 140f, 4f, "1"),
            PositionedText(1, 40f, 140f, 20f, "ISWD"),
            PositionedText(1, 40f, 146f, 13f, "713")
        )
        val parsed = parser.parse(texts)
        assertTrue(parsed.rows.isEmpty())
        assertTrue(parsed.warnings.any { it == ParseWarning.UnsupportedFormat })
    }

    @Test
    fun `los rangos aceptan cualquier guion y fusionan contiguos`() {
        listOf("-", "‐", "‑", "–", "—", "−").forEach { dash ->
            val texts = listOf(
                PositionedText(1, 40f, 100f, 20f, "Nro."),
                PositionedText(1, 80f, 100f, 28f, "Código"),
                PositionedText(1, 124f, 100f, 28f, "Materia"),
                PositionedText(1, 258f, 100f, 15f, "Lunes"),
                PositionedText(1, 280f, 100f, 15f, "Martes"),
                PositionedText(1, 295.5f, 100f, 24.9f, "Miercoles"),
                PositionedText(1, 322.2f, 100f, 18f, "Jueves"),
                PositionedText(1, 341.9f, 100f, 19.8f, "Viernes"),
                PositionedText(1, 363.5f, 100f, 19.3f, "Sabado"),
                PositionedText(1, 40f, 140f, 4f, "1"),
                PositionedText(1, 80f, 140f, 22f, "ISWD713"),
                PositionedText(1, 124f, 128f, 35f, "APLICACIONES"),
                PositionedText(1, 124f, 134f, 21f, "MÓVILES"),
                PositionedText(1, 258f, 136f, 8f, "7${dash}9"),
                PositionedText(1, 258f, 142f, 8f, "9${dash}11")
            )
            val parsed = parser.parse(texts)
            assertEquals(
                "guion U+%04X".format(dash.first().code),
                listOf(ParsedSlot(1, 420, 660)),
                parsed.rows.first().slots
            )
        }
    }

    @Test
    fun `nombres inventados conservan los espacios entre palabras y celdas partidas por lineas`() {
        val texts = listOf(
            PositionedText(1, 84.8f, 250.4f, 11.2f, "Nro."),
            PositionedText(1, 98.9f, 250.4f, 18f, "Código"),
            PositionedText(1, 124f, 250.4f, 19.9f, "Materia"),
            PositionedText(1, 169.6f, 250.4f, 13.6f, "Paral"),
            PositionedText(1, 189f, 250.4f, 11.7f, "Aula"),
            PositionedText(1, 224.8f, 250.4f, 14.9f, "Creds"),
            PositionedText(1, 241.4f, 250.4f, 15.3f, "N.Mat"),
            PositionedText(1, 258.5f, 250.4f, 15.5f, "Lunes"),
            PositionedText(1, 275.8f, 250.4f, 17.9f, "Martes"),
            PositionedText(1, 295.5f, 250.4f, 24.9f, "Miercoles"),
            PositionedText(1, 322.2f, 250.4f, 18f, "Jueves"),
            PositionedText(1, 341.9f, 250.4f, 19.8f, "Viernes"),
            PositionedText(1, 363.5f, 250.4f, 19.3f, "Sabado"),
            PositionedText(1, 384.6f, 250.4f, 17.9f, "FechaI"),
            PositionedText(1, 404.3f, 250.4f, 18.4f, "FechaF"),
            PositionedText(1, 424.4f, 250.4f, 19.7f, "Carrera"),
            PositionedText(1, 469.3f, 250.4f, 10f, "Obs"),
            PositionedText(1, 481.1f, 250.4f, 15.3f, "Coreq"),
            PositionedText(1, 498.2f, 250.4f, 22.4f, "Profesor"),
            // fila 1: materia inventada partida en líneas + profesor inventado por palabras
            PositionedText(1, 498.2f, 258f, 24f, "MARTIAN"),
            PositionedText(1, 124f, 261f, 24f, "FILOSOFÍA"),
            PositionedText(1, 124f, 267f, 26f, "INTERPLANETARIA"),
            PositionedText(1, 124f, 273f, 26.6f, "(MART101)"),
            PositionedText(1, 498.2f, 263.9f, 22.8f, "QUICKSILVER"),
            PositionedText(1, 86f, 266.8f, 3f, "1"),
            PositionedText(1, 98.9f, 266.8f, 22.3f, "MART101"),
            PositionedText(1, 498.2f, 275.6f, 18.7f, "FLASH")
        )
        val parsed = parser.parse(texts)

        assertEquals(1, parsed.rows.size)
        assertEquals("Filosofía Interplanetaria", parsed.rows.first().name)
        assertEquals("Martian Quicksilver Flash", parsed.rows.first().teacher)
    }

    @Test
    fun `los numeros romanos del nombre quedan en mayuscula`() {
        val texts = listOf(
            PositionedText(1, 84.8f, 250.4f, 11.2f, "Nro."),
            PositionedText(1, 98.9f, 250.4f, 18f, "Código"),
            PositionedText(1, 124f, 250.4f, 19.9f, "Materia"),
            PositionedText(1, 169.6f, 250.4f, 13.6f, "Paral"),
            PositionedText(1, 189f, 250.4f, 11.7f, "Aula"),
            PositionedText(1, 224.8f, 250.4f, 14.9f, "Creds"),
            PositionedText(1, 241.4f, 250.4f, 15.3f, "N.Mat"),
            PositionedText(1, 258.5f, 250.4f, 15.5f, "Lunes"),
            PositionedText(1, 275.8f, 250.4f, 17.9f, "Martes"),
            PositionedText(1, 295.5f, 250.4f, 24.9f, "Miercoles"),
            PositionedText(1, 322.2f, 250.4f, 18f, "Jueves"),
            PositionedText(1, 341.9f, 250.4f, 19.8f, "Viernes"),
            PositionedText(1, 363.5f, 250.4f, 19.3f, "Sabado"),
            PositionedText(1, 384.6f, 250.4f, 17.9f, "FechaI"),
            PositionedText(1, 404.3f, 250.4f, 18.4f, "FechaF"),
            PositionedText(1, 424.4f, 250.4f, 19.7f, "Carrera"),
            PositionedText(1, 469.3f, 250.4f, 10f, "Obs"),
            PositionedText(1, 481.1f, 250.4f, 15.3f, "Coreq"),
            PositionedText(1, 498.2f, 250.4f, 22.4f, "Profesor"),
            // fila 1: nombre inventado con número romano al final
            PositionedText(1, 86f, 261f, 3f, "1"),
            PositionedText(1, 98.9f, 261f, 22.3f, "FICT101"),
            PositionedText(1, 124f, 258f, 24f, "FÍSICA"),
            PositionedText(1, 124f, 264f, 9f, "II")
        )
        val parsed = parser.parse(texts)

        assertEquals(1, parsed.rows.size)
        assertEquals("Física II", parsed.rows.first().name)
    }
}
