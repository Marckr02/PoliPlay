package com.example.campuspocket.feature.search.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferencesRepository
import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.FakeTaskRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testTask
import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import com.example.campuspocket.feature.notes.FakeNoteRepository
import com.example.campuspocket.feature.notes.data.NoteEntity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun preferences(scope: CoroutineScope): UserPreferencesRepository =
        UserPreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(folder.root, "search-${System.nanoTime()}.preferences_pb") }
            )
        )

    @Test
    fun `la busqueda agrupa tareas, notas, materias y movimientos`() = runTest(testDispatcher) {
        val preferences = preferences(backgroundScope)
        val viewModel = SearchViewModel(
            courseRepository = FakeCourseRepository(
                listOf(testCourse(id = 1, name = "Cálculo"))
            ),
            taskRepository = FakeTaskRepository(
                listOf(testTask(id = 1, courseId = 1, title = "Examen"))
            ),
            noteRepository = FakeNoteRepository(
                listOf(
                    NoteEntity(
                        id = 1,
                        title = "Apuntes",
                        content = "Repasar cálculo antes del examen"
                    )
                )
            ),
            financeRepository = FakeFinanceRepository(
                accounts = listOf(testAccount(id = 1, name = "Efectivo")),
                categories = listOf(testCategory(id = 1, name = "Supermercado")),
                transactions = listOf(
                    testExpense(
                        id = 1,
                        accountId = 1,
                        categoryId = 1,
                        description = "Compra semanal"
                    )
                )
            ),
            preferencesRepository = preferences
        )
        val job = backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.setQuery("cálculo")
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.tasks.size)
        assertEquals(1, state.notes.size)
        assertEquals(1, state.courses.size)
        assertTrue(state.transactions.isEmpty())
        assertEquals(3, state.totalResults)

        viewModel.setQuery("supermercado")
        runCurrent()

        val financeState = viewModel.uiState.value
        assertEquals(1, financeState.transactions.size)
        assertEquals("Supermercado", financeState.transactions.single().categoryName)
        job.cancel()
    }

    @Test
    fun `el filtro limita los resultados y los modulos ocultos no se ofrecen`() =
        runTest(testDispatcher) {
            val preferences = preferences(backgroundScope)
            val viewModel = SearchViewModel(
                courseRepository = FakeCourseRepository(
                    listOf(testCourse(id = 1, name = "Cálculo"))
                ),
                taskRepository = FakeTaskRepository(
                    listOf(testTask(id = 1, courseId = 1, title = "Examen"))
                ),
                noteRepository = FakeNoteRepository(
                    listOf(NoteEntity(id = 1, title = "Apuntes", content = "Repasar cálculo"))
                ),
                financeRepository = FakeFinanceRepository(),
                preferencesRepository = preferences
            )
            val job = backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.setQuery("cálculo")
            viewModel.setFilter(SearchFilter.NOTES)
            runCurrent()

            val notesOnly = viewModel.uiState.value
            assertEquals(SearchFilter.NOTES, notesOnly.filter)
            assertTrue(notesOnly.tasks.isEmpty())
            assertTrue(notesOnly.courses.isEmpty())
            assertEquals(1, notesOnly.notes.size)

            preferences.setModuleEnabled(CampusModule.NOTES, false)
            runCurrent()

            val hidden = viewModel.uiState.value
            assertEquals(SearchFilter.ALL, hidden.filter)
            assertFalse(SearchFilter.NOTES in hidden.availableFilters)
            assertTrue(hidden.notes.isEmpty())
            job.cancel()
        }
}
