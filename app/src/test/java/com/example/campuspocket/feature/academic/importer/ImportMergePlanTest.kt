package com.example.campuspocket.feature.academic.importer

import com.example.campuspocket.core.designsystem.ColorPalette

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas JVM del plan de fusión de la reimportación (función pura, sin Room).
 * Que el plan se ejecute bien contra la BD real se prueba en el ImportMergeTest
 * instrumentado.
 */
class ImportMergePlanTest {

    private fun session(day: Int = 1, start: Int = 420) = ImportedSession(
        dayOfWeek = day, startMinute = start, endMinute = start + 120, room = "E20/P3/E003", credits = 3
    )

    private fun imported(code: String, section: String = "GR1", name: String = "Materia $code") =
        ImportedCourse(code = code, name = name, section = section, teacher = "Docente", sessions = listOf(session()))

    private fun existing(id: Long, code: String, section: String = "GR1", color: Int = 111) =
        ExistingCourseRef(id = id, code = code, section = section, colorArgb = color)

    @Test
    fun `materia ya guardada se actualiza conservando id y color (las tareas ligadas sobreviven)`() {
        val plan = planImportMerge(
            existing = listOf(existing(id = 7, code = "ISWD713", color = 999)),
            imported = listOf(imported(code = "ISWD713", name = "Aplicaciones Móviles"))
        )

        assertEquals(1, plan.updates.size)
        assertEquals(emptyList<ImportMergePlan.Insert>(), plan.inserts)
        assertEquals(emptyList<Long>(), plan.absentIds)

        val update = plan.updates.single()
        assertEquals(7L, update.existingId) // la FK de las tareas apunta a este id
        assertEquals(999, update.colorArgb)
        assertEquals("Aplicaciones Móviles", update.course.name)
    }

    @Test
    fun `la actualizacion lleva las sesiones nuevas del PDF (las viejas se reemplazan)`() {
        val nuevas = listOf(session(day = 2, start = 660), session(day = 4, start = 540))
        val plan = planImportMerge(
            existing = listOf(existing(id = 3, code = "ISWD713")),
            imported = listOf(imported(code = "ISWD713").copy(sessions = nuevas))
        )

        assertEquals(nuevas, plan.updates.single().course.sessions)
    }

    @Test
    fun `materia nueva se inserta con el color de su posicion en el PDF`() {
        val plan = planImportMerge(
            existing = listOf(existing(id = 1, code = "ISWD713")),
            imported = listOf(imported("ISWD713"), imported("ADMD700"), imported("ISWD743"))
        )

        assertEquals(1, plan.updates.size)
        assertEquals(listOf("ADMD700", "ISWD743"), plan.inserts.map { it.course.code })
        assertEquals(ColorPalette[1], plan.inserts[0].colorArgb)
        assertEquals(ColorPalette[2], plan.inserts[1].colorArgb)
        assertEquals(emptyList<Long>(), plan.absentIds)
    }

    @Test
    fun `materia que el PDF ya no trae queda ausente pero no se borra sin confirmar`() {
        val plan = planImportMerge(
            existing = listOf(existing(id = 5, code = "VIEJ100")),
            imported = emptyList()
        )

        assertEquals(listOf(5L), plan.absentIds)
        assertEquals(emptyList<Long>(), plan.confirmedDeletions(emptyList()))
    }

    @Test
    fun `las bajas confirmadas se limitan a los ausentes calculados`() {
        val plan = planImportMerge(
            existing = listOf(existing(id = 5, code = "VIEJ100"), existing(id = 6, code = "OTRA101")),
            imported = listOf(imported("OTRA101"))
        )

        assertEquals(listOf(5L), plan.absentIds)
        val confirmadas = plan.confirmedDeletions(listOf(6L, 5L, 99L))
        assertEquals(listOf(5L), confirmadas)
    }

    @Test
    fun `misma clave con distinto paralelo no se mezcla`() {
        val plan = planImportMerge(
            existing = listOf(existing(id = 7, code = "ADMD700", section = "GR1")),
            imported = listOf(imported("ADMD700", section = "GR3"))
        )

        assertEquals(emptyList<ImportMergePlan.Update>(), plan.updates)
        assertEquals(1, plan.inserts.size)
        assertEquals("GR3", plan.inserts.single().course.section)
        assertEquals(listOf(7L), plan.absentIds)
    }
}
