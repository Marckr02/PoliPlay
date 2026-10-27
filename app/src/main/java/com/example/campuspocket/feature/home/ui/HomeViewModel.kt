package com.example.campuspocket.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.campuspocket.BuildConfig
import com.example.campuspocket.core.preferences.PreferenceKeys
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import com.example.campuspocket.feature.academic.ui.TimeText
import com.example.campuspocket.feature.finance.data.BudgetEntity
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

/** Tipo de elemento de la línea de tiempo del día. */
enum class HomeTimelineKind { CLASS, TASK, PAYMENT }

/**
 * Elemento ya resuelto de la línea de tiempo de Inicio. [time] es null para
 * los pagos programados: no tienen hora y se muestran como "Todo el día".
 */
data class HomeTimelineEntry(
    val key: String,
    val kind: HomeTimelineKind,
    val time: LocalTime?,
    val title: String,
    val subtitle: String? = null,
    val amountCents: Long? = null,
    val courseId: Long? = null
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
    val timeline: List<HomeTimelineEntry> = emptyList(),
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
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock
) : ViewModel() {

    private val _autoWhatsNew = MutableStateFlow(false)
    val autoWhatsNew: StateFlow<Boolean> = _autoWhatsNew.asStateFlow()

    init {
        // Aviso de novedades una sola vez tras actualizar, al arrancar la app.
        // Instalación nueva: se marca la versión actual sin mostrar nada.
        viewModelScope.launch {
            val last = dataStore.data.first()[PreferenceKeys.LAST_SEEN_VERSION]
            when {
                last == null -> dataStore.edit {
                    it[PreferenceKeys.LAST_SEEN_VERSION] = BuildConfig.VERSION_NAME
                }
                last != BuildConfig.VERSION_NAME -> _autoWhatsNew.value = true
            }
        }
    }

    fun onWhatsNewSeen() {
        _autoWhatsNew.value = false
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.LAST_SEEN_VERSION] = BuildConfig.VERSION_NAME }
        }
    }

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
        financeRepository.observeActiveScheduledPayments(),
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
        @Suppress("UNCHECKED_CAST")
        val payments = values[4] as List<ScheduledPaymentEntity>
        val today = values[5] as LocalDate
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
            transactions = transactions,
            payments = payments
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        courseRepository.observeCourses(),
        taskRepository.observePending(),
        noteRepository.observeAllNotes(),
        financeFlow,
        todayFlow
    ) { courses, tasks, notes, finance, today ->
        val classes = classesFor(courses, today)
        val now = LocalTime.now(clock)
        val nextIndex = classes.indexOfFirst { it.end.isAfter(now) }
        val next = classes.getOrNull(nextIndex)?.resolve(now)
        val later = if (nextIndex >= 0) classes.getOrNull(nextIndex + 1)?.resolve(now) else null
        val zone = clock.zone

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
            timeline = timelineFor(
                courses = courses,
                classes = classes,
                tasks = tasks,
                payments = finance.payments,
                today = today,
                zone = zone
            ),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

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

    /**
     * Une clases, entregas y pagos del día en una sola secuencia. Los pagos no
     * tienen hora: se ordenan primero como "Todo el día".
     */
    private fun timelineFor(
        courses: List<Course>,
        classes: List<HomeClass>,
        tasks: List<Task>,
        payments: List<ScheduledPaymentEntity>,
        today: LocalDate,
        zone: ZoneId
    ): List<HomeTimelineEntry> {
        val courseNames = courses.mapNotNull { course -> course.id?.let { it to course.name } }.toMap()
        val classEntries = classes.map { item ->
            HomeTimelineEntry(
                key = "class-${item.courseId}-${item.start}",
                kind = HomeTimelineKind.CLASS,
                time = item.start,
                title = item.courseName,
                subtitle = item.room?.takeIf { it.isNotBlank() },
                courseId = item.courseId.takeIf { it > 0L }
            )
        }
        val taskEntries = tasks
            .filter { it.dueAt.atZone(zone).toLocalDate() == today }
            .map { task ->
                val due = task.dueAt.atZone(zone)
                HomeTimelineEntry(
                    key = "task-${task.id ?: "${task.title}-${task.dueAt}"}",
                    kind = HomeTimelineKind.TASK,
                    time = due.toLocalTime(),
                    title = task.title,
                    subtitle = task.courseId?.let(courseNames::get)
                )
            }
        val paymentEntries = payments
            .filter { it.nextDueDate == today }
            .map { payment ->
                HomeTimelineEntry(
                    key = "payment-${payment.id}",
                    kind = HomeTimelineKind.PAYMENT,
                    time = null,
                    title = payment.name,
                    amountCents = payment.amountCents
                )
            }
        return (classEntries + taskEntries + paymentEntries).sortedWith(
            compareBy<HomeTimelineEntry> { it.time ?: LocalTime.MIN }
                .thenBy { it.kind.ordinal }
                .thenBy { it.title }
        )
    }

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
        val transactions: List<Transaction>,
        val payments: List<ScheduledPaymentEntity>
    )
}