package com.example.campuspocket.feature.home.ui

import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.FakeTaskRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testSession
import com.example.campuspocket.feature.academic.testTask
import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import com.example.campuspocket.feature.notes.FakeNoteRepository
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.example.campuspocket.BuildConfig
import com.example.campuspocket.core.preferences.PreferenceKeys
import java.io.File
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-10-21T12:00:00Z"),
        ZoneOffset.UTC
    )

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

    @Test
    fun `la tarjeta usa el presupuesto mensual aunque no haya presupuestos por categoria`() =
        runTest(testDispatcher) {
            val repo = FakeFinanceRepository(
                accounts = listOf(testAccount(id = 1)),
                categories = listOf(testCategory(id = 1, kind = CategoryKind.EXPENSE)),
                transactions = listOf(
                    testExpense(
                        id = 1,
                        accountId = 1,
                        categoryId = 1,
                        amountCents = 5_000,
                        date = LocalDate.of(2026, 10, 10)
                    )
                )
            )
            repo.upsertMonthlyBudget("2026-10", 16_000)

            val viewModel = HomeViewModel(
                courseRepository = FakeCourseRepository(),
                taskRepository = FakeTaskRepository(),
                financeRepository = repo,
                noteRepository = FakeNoteRepository(),
                dataStore = PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { File(folder.root, "home.preferences_pb") }
                ),
                clock = fixedClock
            )

            val job = backgroundScope.launch(testDispatcher) {
                viewModel.uiState.collect {}
            }
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(16_000L, state.monthlyBudgetCents)
            assertEquals(5_000L, state.monthlySpentCents)
            assertEquals(11_000L, state.monthlyRemainingCents ?: 0L)
            job.cancel()
        }

    @Test
    fun `la linea de tiempo ordena pagos clases y tareas del dia`() = runTest(testDispatcher) {
        val course = testCourse(
            id = 1,
            name = "Cálculo",
            sessions = listOf(
                testSession(
                    id = 1,
                    dayOfWeek = DayOfWeek.WEDNESDAY,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(11, 0),
                    room = "Aula 3"
                )
            )
        )
        val tasks = listOf(
            testTask(
                id = 1,
                title = "Entregar ensayo",
                courseId = 1,
                dueAt = Instant.parse("2026-10-21T15:00:00Z")
            ),
            testTask(
                id = 2,
                title = "Mañana",
                dueAt = Instant.parse("2026-10-22T15:00:00Z")
            ),
            testTask(
                id = 3,
                title = "Ayer",
                dueAt = Instant.parse("2026-10-20T15:00:00Z")
            )
        )
        val payments = listOf(
            ScheduledPaymentEntity(
                id = 1,
                name = "Alquiler",
                amountCents = 50_000,
                accountId = 1,
                nextDueDate = LocalDate.of(2026, 10, 21)
            ),
            ScheduledPaymentEntity(
                id = 2,
                name = "Internet",
                amountCents = 2_000,
                accountId = 1,
                nextDueDate = LocalDate.of(2026, 10, 22)
            )
        )
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(testCategory(id = 1, kind = CategoryKind.EXPENSE)),
            payments = payments
        )

        val viewModel = HomeViewModel(
            courseRepository = FakeCourseRepository(listOf(course)),
            taskRepository = FakeTaskRepository(tasks),
            financeRepository = repo,
            noteRepository = FakeNoteRepository(),
            dataStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { File(folder.root, "timeline.preferences_pb") }
            ),
            clock = fixedClock
        )

        val job = backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        val timeline = viewModel.uiState.value.timeline
        assertEquals(
            listOf(HomeTimelineKind.PAYMENT, HomeTimelineKind.CLASS, HomeTimelineKind.TASK),
            timeline.map { it.kind }
        )
        assertEquals(listOf("Alquiler", "Cálculo", "Entregar ensayo"), timeline.map { it.title })
        assertNull(timeline[0].time)
        assertEquals(50_000L, timeline[0].amountCents)
        assertEquals(LocalTime.of(10, 0), timeline[1].time)
        assertEquals("Aula 3", timeline[1].subtitle)
        assertEquals(1L, timeline[1].courseId)
        assertEquals(LocalTime.of(15, 0), timeline[2].time)
        job.cancel()
    }

    @Test
    fun `novedades se muestran una vez tras actualizar y se marcan como vistas`() =
        runTest(testDispatcher) {
            val store = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { File(folder.root, "whatsnew.preferences_pb") }
            )
            store.edit { it[PreferenceKeys.LAST_SEEN_VERSION] = "0.8.0" }

            val viewModel = HomeViewModel(
                courseRepository = FakeCourseRepository(),
                taskRepository = FakeTaskRepository(),
                financeRepository = FakeFinanceRepository(),
                noteRepository = FakeNoteRepository(),
                dataStore = store,
                clock = fixedClock
            )
            runCurrent()

            assertTrue(viewModel.autoWhatsNew.value)
            viewModel.onWhatsNewSeen()
            runCurrent()
            assertFalse(viewModel.autoWhatsNew.value)
            assertEquals(
                BuildConfig.VERSION_NAME,
                store.data.first()[PreferenceKeys.LAST_SEEN_VERSION]
            )
        }

    @Test
    fun `instalacion nueva no muestra novedades y marca la version actual`() =
        runTest(testDispatcher) {
            val store = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { File(folder.root, "fresh.preferences_pb") }
            )

            val viewModel = HomeViewModel(
                courseRepository = FakeCourseRepository(),
                taskRepository = FakeTaskRepository(),
                financeRepository = FakeFinanceRepository(),
                noteRepository = FakeNoteRepository(),
                dataStore = store,
                clock = fixedClock
            )
            runCurrent()

            assertFalse(viewModel.autoWhatsNew.value)
            assertEquals(
                BuildConfig.VERSION_NAME,
                store.data.first()[PreferenceKeys.LAST_SEEN_VERSION]
            )
        }
}
