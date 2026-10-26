package com.example.campuspocket.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import com.example.campuspocket.feature.academic.ui.TimeText
import com.example.campuspocket.feature.finance.data.BudgetEntity
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** Clase del día, ya resuelta para la tarjeta hero de Inicio. */
data class HomeClass(
    val courseId: Long,
    val courseName: String,
    val courseColorArgb: Int,
    val start: LocalTime,
    val end: LocalTime,
    val room: String?,
    val isCurrent: Boolean,
    val minutesUntil: Long,
    val progress: Float,
    val startLabel: String,
    val endLabel: String
)

data class GlobalSearchResults(
    val tasks: List<Task> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val courses: List<Course> = emptyList(),
    val transactions: List<Transaction> = emptyList()
)

/** Estado de solo lectura que alimenta Inicio. */
data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val nextClass: HomeClass? = null,
    val laterClass: HomeClass? = null,
    val pendingTasks: Int = 0,
    val tasksDueToday: Int = 0,
    val urgentTasks: List<Task> = emptyList(),
    val monthlyBudgetCents: Long? = null,
    val monthlySpentCents: Long = 0,
    val monthlyRemainingCents: Long? = null,
    val recentNotes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val searchResults: GlobalSearchResults = GlobalSearchResults(),
    val isLoading: Boolean = true
)

/**
 * Agrega las fuentes ya existentes (horario, tareas, finanzas y notas) en un
 * único estado de presentación para Inicio.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    financeRepository: FinanceRepository,
    noteRepository: NoteRepository,
    private val clock: Clock
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val todayFlow: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(60_000L)
        }
    }.distinctUntilChanged()

    private val financeFlow = combine(
        financeRepository.observeTransactions(),
        financeRepository.observeAllBudgets(),
        financeRepository.observeMonthlyBudgets(),
        financeRepository.observeActiveCategories(),
        todayFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val transactions = values[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val budgets = values[1] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val monthlyBudgets = values[2] as List<MonthlyBudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[3] as List<Category>
        val today = values[4] as LocalDate
        val month = YearMonth.from(today)
        val expenseCategories = categories
            .filter { it.kind == CategoryKind.EXPENSE }
            .mapNotNull { category -> category.id?.let { id -> id to category } }
            .toMap()
        val budgetMap = budgets
            .filter { it.yearMonth == month.toString() }
            .associate { it.categoryId to it.amountCents }
        val summary = GetMonthSummary.compute(
            yearMonth = month,
            transactions = transactions,
            budgets = budgetMap,
            categories = expenseCategories,
            monthStart = month.atDay(1),
            monthEnd = month.atEndOfMonth()
        )
        val monthlyBudgetCents = monthlyBudgets
            .firstOrNull { it.yearMonth == month.toString() }
            ?.amountCents
            ?.takeIf { it > 0L }
        val monthlySpentCents = summary.totalSpentCents.coerceAtLeast(0L)
        HomeFinance(
            monthlyBudgetCents = monthlyBudgetCents,
            monthlySpentCents = monthlySpentCents,
            monthlyRemainingCents = monthlyBudgetCents?.minus(monthlySpentCents),
            transactions = transactions
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        courseRepository.observeCourses(),
        taskRepository.observePending(),
        noteRepository.observeAllNotes(),
        financeFlow,
        todayFlow,
        searchQuery
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val courses = array[0] as List<Course>
        @Suppress("UNCHECKED_CAST")
        val tasks = array[1] as List<Task>
        @Suppress("UNCHECKED_CAST")
        val notes = array[2] as List<NoteEntity>
        val finance = array[3] as HomeFinance
        val today = array[4] as LocalDate
        val query = array[5] as String

        val classes = classesFor(courses, today)
        val now = LocalTime.now(clock)
        val nextIndex = classes.indexOfFirst { it.end.isAfter(now) }
        val next = classes.getOrNull(nextIndex)?.resolve(now)
        val later = if (nextIndex >= 0) classes.getOrNull(nextIndex + 1)?.resolve(now) else null
        val zone = ZoneId.systemDefault()

        val q = query.trim()
        val searchResults = if (q.isBlank()) {
            GlobalSearchResults()
        } else {
            GlobalSearchResults(
                tasks = tasks.filter { it.title.contains(q, ignoreCase = true) },
                notes = notes.filter { it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true) },
                courses = courses.filter { it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true) },
                transactions = finance.transactions.filter { it.description?.contains(q, ignoreCase = true) == true }
            )
        }

        HomeUiState(
            today = today,
            nextClass = next,
            laterClass = later,
            pendingTasks = tasks.size,
            tasksDueToday = tasks.count {
                it.dueAt.atZone(zone).toLocalDate() == today
            },
            urgentTasks = tasks.sortedBy { it.dueAt }.take(2),
            monthlyBudgetCents = finance.monthlyBudgetCents,
            monthlySpentCents = finance.monthlySpentCents,
            monthlyRemainingCents = finance.monthlyRemainingCents,
            recentNotes = notes
                .sortedWith(compareByDescending<NoteEntity> { it.pinned }.thenByDescending { it.updatedAt })
                .take(2),
            searchQuery = query,
            searchResults = searchResults,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun setTaskCompleted(task: Task, done: Boolean) {
        viewModelScope.launch {
            if (done) {
                taskRepository.complete(task)
            } else {
                taskRepository.update(task.copy(completedAt = null))
            }
        }
    }

    private fun classesFor(courses: List<Course>, today: LocalDate): List<HomeClass> =
        courses.flatMap { course ->
            course.sessions
                .filter { it.dayOfWeek == today.dayOfWeek }
                .map { session ->
                    HomeClass(
                        courseId = course.id ?: 0L,
                        courseName = course.name,
                        courseColorArgb = course.colorArgb,
                        start = session.startTime,
                        end = session.endTime,
                        room = session.room,
                        isCurrent = false,
                        minutesUntil = 0,
                        progress = 0f,
                        startLabel = TimeText.format(session.startTime),
                        endLabel = TimeText.format(session.endTime)
                    )
                }
        }.sortedBy { it.start }

    private fun HomeClass.resolve(now: LocalTime): HomeClass {
        val current = !now.isBefore(start) && now.isBefore(end)
        val minutes = if (now.isBefore(start)) {
            Duration.between(now, start).toMinutes()
        } else {
            0L
        }
        val progress = if (current) {
            val total = Duration.between(start, end).seconds.coerceAtLeast(1L)
            (Duration.between(start, now).seconds.toFloat() / total).coerceIn(0f, 1f)
        } else {
            0f
        }
        return copy(isCurrent = current, minutesUntil = minutes, progress = progress)
    }

    private data class HomeFinance(
        val monthlyBudgetCents: Long?,
        val monthlySpentCents: Long,
        val monthlyRemainingCents: Long?,
        val transactions: List<Transaction>
    )
}