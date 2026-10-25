package com.example.campuspocket.feature.notes.ui

/** Rutas del grafo de notas (Fase 6). */
object NotesDestinations {
    const val NOTES_LIST = "notes/list"
    const val NOTE_ID_ARG = "noteId"
    const val COURSE_ID_ARG = "courseId"
    const val NOTE_EDITOR_ROUTE = "notes/editor/{$NOTE_ID_ARG}?$COURSE_ID_ARG={$COURSE_ID_ARG}"

    fun editor(noteId: Long? = null, courseId: Long? = null): String {
        val id = noteId ?: -1L
        return if (courseId == null) {
            "notes/editor/$id"
        } else {
            "notes/editor/$id?$COURSE_ID_ARG=$courseId"
        }
    }
}
