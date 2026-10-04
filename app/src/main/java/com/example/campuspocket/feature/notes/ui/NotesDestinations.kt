package com.example.campuspocket.feature.notes.ui

/** Rutas del grafo de notas (Fase 6). */
object NotesDestinations {
    const val NOTES_LIST = "notes/list"
    const val NOTE_ID_ARG = "noteId"
    const val NOTE_EDITOR_ROUTE = "notes/editor/{$NOTE_ID_ARG}"

    fun editor(noteId: Long? = null): String =
        if (noteId == null) "notes/editor/-1" else "notes/editor/$noteId"
}
