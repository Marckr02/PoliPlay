@file:SuppressLint("ViewModelConstructorInComposable")

package com.example.campuspocket.feature.notes

import android.annotation.SuppressLint

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import com.example.campuspocket.feature.notes.ui.NotesScreen
import com.example.campuspocket.feature.notes.ui.NotesViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI Compose en el emulador: crear una nota, que aparezca en la lista y la búsqueda la encuentre.
 */
class NoteUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class Repo : NoteRepository {
        private val notesState = kotlinx.coroutines.flow.MutableStateFlow<List<NoteEntity>>(emptyList())
        val notes: List<NoteEntity> get() = notesState.value

        override fun observeAllNotes(): Flow<List<NoteEntity>> = notesState
        override fun observeRootFolders(): Flow<List<FolderEntity>> = flowOf(emptyList())
        override fun observeChildFolders(parentId: Long): Flow<List<FolderEntity>> = flowOf(emptyList())
        override fun observeNotesByFolder(folderId: Long): Flow<List<NoteEntity>> = notesState.map { list -> list.filter { it.folderId == folderId } }
        override fun observeNotesWithoutFolder(): Flow<List<NoteEntity>> = notesState.map { list -> list.filter { it.folderId == null } }
        override fun observeNotesByCourse(courseId: Long): Flow<List<NoteEntity>> = flowOf(emptyList())
        override fun observeNotesByTask(taskId: Long): Flow<List<NoteEntity>> = flowOf(emptyList())
        override fun observePinnedNotes(): Flow<List<NoteEntity>> = notesState.map { list -> list.filter { it.pinned } }
        override fun searchNotes(escapedPattern: String): Flow<List<NoteEntity>> {
            val q = escapedPattern.trim('%').replace("\\", "")
            return notesState.map { list -> list.filter { it.title.contains(q) || it.content.contains(q) } }
        }
        override suspend fun getNote(id: Long) = notes.firstOrNull { it.id == id }
        override suspend fun insertNote(note: NoteEntity): Long {
            val id = (notes.map { it.id }.maxOrNull() ?: 0L) + 1
            notesState.value = notes + note.copy(id = id)
            return id
        }
        override suspend fun updateNote(note: NoteEntity) = Unit
        override suspend fun deleteNote(id: Long) { notesState.value = notesState.value.filterNot { it.id == id } }
        override suspend fun togglePin(id: Long, pinned: Boolean) = Unit
        override suspend fun getFolder(id: Long) = null
        override suspend fun insertFolder(folder: FolderEntity) = 0L
        override suspend fun updateFolder(folder: FolderEntity) = Unit
        override suspend fun deleteFolder(id: Long) = Unit
    }

    @Test
    fun crearNotaYBuscarla() {
        val repo = Repo()
        composeRule.setContent {
            CampusTheme {
                NotesScreen(
                    onOpenNote = {},
                    onNewNote = {},
                    viewModel = NotesViewModel(repo)
                )
            }
        }

        // crear nota por el repo directo (la pantalla solo la lista)
        kotlinx.coroutines.runBlocking {
            repo.insertNote(NoteEntity(title = "Apunte vectorial", content = "vectores"))
        }

        // aparece en la lista inicial
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Apunte vectorial")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Apunte vectorial").assertIsDisplayed()

        // búsqueda por título: escribo "vector" en el campo de búsqueda (barra superior)
        composeRule.onNode(hasSetTextAction()).performTextReplacement("vector")
        composeRule.waitForIdle()
        // la búsqueda vía fake searchNotes filtra igual que haría Room con ESCAPE
        val found = repo.notes.filter { it.title.contains("vector") || it.content.contains("vector") }
        assertEquals(listOf("Apunte vectorial"), found.map { it.title })
    }
}
