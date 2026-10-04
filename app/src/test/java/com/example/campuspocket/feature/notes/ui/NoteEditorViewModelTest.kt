package com.example.campuspocket.feature.notes.ui

import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeNoteRepo : NoteRepository {
        val notes = mutableListOf<NoteEntity>()
        val inserted = mutableListOf<NoteEntity>()
        val updatedFlat = mutableListOf<NoteEntity>()
        val deleted = mutableListOf<Long>()
        suspend fun seed(note: NoteEntity): Long {
            val id = (notes.map { it.id }.maxOrNull() ?: 0L) + 1
            notes += note.copy(id = id)
            return id
        }

        override fun observeAllNotes() = flowOf(notes.toList())
        override fun observeRootFolders() = flowOf(emptyList<com.example.campuspocket.feature.notes.data.FolderEntity>())
        override fun observeChildFolders(parentId: Long) = flowOf(emptyList<com.example.campuspocket.feature.notes.data.FolderEntity>())
        override fun observeNotesByFolder(folderId: Long) = flowOf(notes.filter { it.folderId == folderId })
        override fun observeNotesWithoutFolder() = flowOf(notes.filter { it.folderId == null })
        override fun observeNotesByCourse(courseId: Long) = flowOf(notes.filter { it.courseId == courseId })
        override fun observeNotesByTask(taskId: Long) = flowOf(notes.filter { it.taskId == taskId })
        override fun observePinnedNotes() = flowOf(notes.filter { it.pinned })
        override fun searchNotes(escapedPattern: String) = flowOf(notes)
        override suspend fun getNote(id: Long) = notes.firstOrNull { it.id == id }
        override suspend fun insertNote(note: NoteEntity): Long {
            inserted += note
            val id = (notes.map { it.id }.maxOrNull() ?: 0L) + 1
            notes += note.copy(id = id)
            return id
        }
        override suspend fun updateNote(note: NoteEntity) {
            updatedFlat += note
            val idx = notes.indexOfFirst { it.id == note.id }
            if (idx >= 0) {
                val prev = notes[idx]
                if (prev.title != note.title || prev.content != note.content) {
                    notes[idx] = note.copy(updatedAt = System.currentTimeMillis())
                } else {
                    notes[idx] = note // updatedAt NO cambia si nada cambió
                }
            }
        }
        override suspend fun deleteNote(id: Long) { deleted += id; notes.removeAll { it.id == id } }
        override suspend fun togglePin(id: Long, pinned: Boolean) {
            notes.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { notes[it] = notes[it].copy(pinned = pinned) }
        }
        override suspend fun getFolder(id: Long) = null
        override suspend fun insertFolder(folder: com.example.campuspocket.feature.notes.data.FolderEntity) = 0L
        override suspend fun updateFolder(folder: com.example.campuspocket.feature.notes.data.FolderEntity) = Unit
        override suspend fun deleteFolder(id: Long) = Unit
    }

    @Test
    fun `autosave con debounce una nota vacia no se guarda y updatedAt solo si cambio`() = runTest(testDispatcher) {
        val repo = FakeNoteRepo()
        val vm = NoteEditorViewModel(
            SavedStateHandle(mapOf("noteId" to -1L)),
            repo,
            testDispatcher
        )

        // sigue vacía aunque pase el debounce
        vm.onTitleChange("")
        vm.onContentChange("")
        advanceTimeBy(1_000)
        advanceUntilIdle()
        assertTrue(repo.inserted.isEmpty())

        // escribe título pero cuerpo vacío debería guardarse (título solo)
        vm.onTitleChange("Apunte")
        advanceTimeBy(600)
        advanceUntilIdle()
        assertEquals(1, repo.inserted.size)
        assertEquals("Apunte", repo.inserted.single().title)

        // editar sin cambiar contenido NO toca updatedAt
        val created = repo.notes.single().updatedAt
        val sameBody = repo.notes.single().content
        vm.onContentChange(sameBody) // sin cambio real
        advanceTimeBy(600)
        advanceUntilIdle()
        assertEquals(created, repo.notes.single().updatedAt)

        // sí cambia con contenido nuevo (y re-comparando contra el estado actual)
        vm.onContentChange("nuevo cuerpo distinto")
        advanceTimeBy(600)
        advanceUntilIdle()
        // espera un ciclo más para el distinctUntilChanged(nuevo contenido) + saveIfDirty
        advanceTimeBy(600)
        advanceUntilIdle()
        assertTrue(repo.notes.single().updatedAt > created)
    }

    @Test
    fun `sobrevive a cargar una nota existente`() = runTest(testDispatcher) {
        val repo = FakeNoteRepo()
        val id = repo.seed(NoteEntity(id = 5, title = "Vieja", content = "cuerpo", folderId = null, pinned = true))
        val vm = NoteEditorViewModel(SavedStateHandle(mapOf("noteId" to id)), repo, testDispatcher)
        advanceUntilIdle()

        assertEquals("Vieja", vm.uiState.value.title)
        assertEquals("cuerpo", vm.uiState.value.content)
        assertTrue(vm.uiState.value.pinned)
    }
}
