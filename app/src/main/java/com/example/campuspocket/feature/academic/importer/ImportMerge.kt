package com.example.campuspocket.feature.academic.importer

import com.example.campuspocket.core.designsystem.ColorPalette

/** Materia ya guardada, reducida a lo único que la fusión necesita conocer. */
data class ExistingCourseRef(
    val id: Long,
    val code: String,
    val section: String,
    val colorArgb: Int
)

/**
 * Resultado de fusionar la importación con lo ya guardado.
 * Es un plan de datos puro (sin Room ni Android): ImportScheduleUseCase solo lo ejecuta.
 */
data class ImportMergePlan(
    val updates: List<Update>,
    val inserts: List<Insert>,
    val absentIds: List<Long>
) {
    /** Actualizar conservando el id (y el color) de la materia guardada. */
    data class Update(val existingId: Long, val colorArgb: Int, val course: ImportedCourse)

    /** Insertar como materia nueva, con el color que le tocó en la paleta. */
    data class Insert(val colorArgb: Int, val course: ImportedCourse)

    /**
     * Ids que de verdad se borran: la intersección entre lo confirmado por el
     * usuario y los ausentes calculados. Cualquier id fuera de los ausentes se
     * descarta, así nunca se borra nada de más.
     */
    fun confirmedDeletions(deleteAbsentCourseIds: List<Long>): List<Long> {
        val absent = absentIds.toSet()
        return deleteAbsentCourseIds.distinct().filter { it in absent }
    }
}

/**
 * Fusión pura de la reimportación. Clave: (código, paralelo) dentro del semestre.
 * - Materia que ya existe -> `updates` con su id y color actuales (sus tareas sobreviven).
 * - Materia nueva -> `inserts` con color de paleta según su posición en el PDF.
 * - Materia guardada que el PDF ya no trae -> `absentIds` (no se borra por sí sola).
 */
fun planImportMerge(
    existing: List<ExistingCourseRef>,
    imported: List<ImportedCourse>
): ImportMergePlan {
    val existingByKey = existing.associateBy { it.code to it.section }
    val updates = mutableListOf<ImportMergePlan.Update>()
    val inserts = mutableListOf<ImportMergePlan.Insert>()

    imported.forEachIndexed { index, course ->
        val found = existingByKey[course.code to course.section]
        if (found == null) {
            inserts += ImportMergePlan.Insert(ColorPalette[index], course)
        } else {
            updates += ImportMergePlan.Update(found.id, found.colorArgb, course)
        }
    }

    val importedKeys = imported.mapTo(HashSet()) { it.code to it.section }
    val absentIds = existing.filter { (it.code to it.section) !in importedKeys }.map { it.id }

    return ImportMergePlan(updates, inserts, absentIds)
}
