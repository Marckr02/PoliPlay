package com.example.campuspocket.feature.academic.ui.courses

import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.testCourse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoursesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga las materias del semestre activo`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(
            listOf(testCourse(id = 1, name = "Cálculo"), testCourse(id = 2, name = "Física"))
        )
        val viewModel = CoursesViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Cálculo", "Física"), state.courses.map { it.name })
    }

    @Test
    fun `borrar una materia llama al repositorio`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(listOf(testCourse(id = 3), testCourse(id = 4)))
        val viewModel = CoursesViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteCourse(3L)
        advanceUntilIdle()

        assertEquals(listOf(3L), repository.deleted)
        assertEquals(listOf(4L), viewModel.uiState.value.courses.map { it.id })
    }
}
