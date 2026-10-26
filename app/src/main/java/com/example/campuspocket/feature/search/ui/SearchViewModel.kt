package com.example.campuspocket.feature.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.core.preferences.UserPreferences
import com.example.campuspocket.core.preferences.UserPreferencesRepository
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tipos de resultado que se pueden filtrar en la búsqueda global. */
enum class SearchFilter { ALL, TASKS, NOTES, TRANSACTIONS, COURSES }

data class TaskSearchResult(
    val task: Task,
    val courseName: String?
)

data class TransactionSearchResult(
    val transaction: Transaction,
    val accountName: String?,
    val targetAccountName: String?,
    val categoryName: String?
)

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val availableFilters: List<SearchFilter> = listOf(SearchFilter.ALL),
    val tasks: List<TaskSearchResult> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val transactions: List<TransactionSearchResult> = emptyList(),
    val courses: List<Course> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = true
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val totalResults: Int
        get() = tasks.size + notes.size + transactions.size + courses.size
}

/**
 * Búsqueda global (v7 B4): agrega tareas, notas, materias y movimientos en
 * memoria. Solo consulta los módulos activos y guarda las últimas consultas
 * en el DataStore de preferencias.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    courseRepository: CourseRepository,
    taskRepository: TaskRepository,
    noteRepository: NoteRepository,
    financeRepository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchFilter.ALL)

    private val allTasksFlow: Flow<List<Task>> = combine(
        taskRepository.observePending(),
        taskRepository.observeCompleted()
    ) { pending, completed -> pending + completed }

    private val financeFlow: Flow<FinanceSearchData> = combine(
        financeRepository.observeTransactions(),
        financeRepository.observeActiveAccounts(),
        financeRepository.observeActiveCategories()
    ) { transactions, accounts, categories ->
        FinanceSearchData(
            transactions = transactions,
            accountNames = accounts.mapNotNull { account ->
                account.id?.let { id -> id to account.name }
            }.toMap(),
            categoryNames = categories.mapNotNull { category ->
                category.id?.let { id -> id to category.name }
            }.toMap()
        )
    }

    private val libraryFlow: Flow<SearchLibrary> = combine(
        courseRepository.observeCourses(),
        allTasksFlow,
        noteRepository.observeAllNotes(),
        financeFlow,
        preferencesRepository.preferences
    ) { courses, tasks, notes, finance, preferences ->
        SearchLibrary(
            courses = courses,
            tasks = tasks,
            notes = notes,
            finance = finance,
            preferences = preferences
        )
    }

    private val resultsFlow: Flow<SearchUiState> = combine(
        libraryFlow,
        query,
        filter
    ) { library, rawQuery, selectedFilter ->
        val q = rawQuery.trim()
        val academicEnabled = library.preferences.academicEnabled
        val notesEnabled = library.preferences.notesEnabled
        val financeEnabled = library.preferences.financeEnabled
        val availableFilters = buildList {
            add(SearchFilter.ALL)
            if (academicEnabled) add(SearchFilter.TASKS)
            if (notesEnabled) add(SearchFilter.NOTES)
            if (financeEnabled) add(SearchFilter.TRANSACTIONS)
            if (academicEnabled) add(SearchFilter.COURSES)
        }
        val effectiveFilter = selectedFilter
            .takeIf { it in availableFilters }
            ?: SearchFilter.ALL

        val courseNames = library.courses.mapNotNull { course ->
            course.id?.let { id -> id to course.name }
        }.toMap()

        val taskResults = if (academicEnabled && q.isNotEmpty()) {
            library.tasks
                .filter { task ->
                    task.title.contains(q, ignoreCase = true) ||
                        task.description?.contains(q, ignoreCase = true) == true ||
                        task.courseId
                            ?.let { courseNames[it] }
                            ?.contains(q, ignoreCase = true) == true
                }
                .sortedWith(
                    compareBy<Task>({ it.completedAt != null }, { it.dueAt })
                )
                .map { task ->
                    TaskSearchResult(
                        task = task,
                        courseName = task.courseId?.let(courseNames::get)
                    )
                }
        } else {
            emptyList()
        }

        val noteResults = if (notesEnabled && q.isNotEmpty()) {
            library.notes
                .filter { note ->
                    note.title.contains(q, ignoreCase = true) ||
                        note.content.contains(q, ignoreCase = true)
                }
                .sortedWith(
                    compareByDescending<NoteEntity> { it.pinned }
                        .thenByDescending { it.updatedAt }
                )
        } else {
            emptyList()
        }

        val transactionResults = if (financeEnabled && q.isNotEmpty()) {
            library.finance.transactions
                .filter { transaction ->
                    transaction.description?.contains(q, ignoreCase = true) == true ||
                        transaction.categoryId
                            ?.let { library.finance.categoryNames[it] }
                            ?.contains(q, ignoreCase = true) == true ||
                        library.finance.accountNames[transaction.accountId]
                            ?.contains(q, ignoreCase = true) == true ||
                        transaction.targetAccountId
                            ?.let { library.finance.accountNames[it] }
                            ?.contains(q, ignoreCase = true) == true
                }
                .sortedWith(
                    compareByDescending<Transaction> { it.date }
                        .thenByDescending { it.createdAt }
                )
                .map { transaction ->
                    TransactionSearchResult(
                        transaction = transaction,
                        accountName = library.finance.accountNames[transaction.accountId],
                        targetAccountName = transaction.targetAccountId
                            ?.let { library.finance.accountNames[it] },
                        categoryName = transaction.categoryId
                            ?.let { library.finance.categoryNames[it] }
                    )
                }
        } else {
            emptyList()
        }

        val courseResults = if (academicEnabled && q.isNotEmpty()) {
            library.courses
                .filter { course ->
                    course.name.contains(q, ignoreCase = true) ||
                        course.code.contains(q, ignoreCase = true) ||
                        course.teacher?.contains(q, ignoreCase = true) == true
                }
                .sortedBy { it.name }
        } else {
            emptyList()
        }

        SearchUiState(
            query = rawQuery,
            filter = effectiveFilter,
            availableFilters = availableFilters,
            tasks = if (
                effectiveFilter == SearchFilter.ALL ||
                effectiveFilter == SearchFilter.TASKS
            ) {
                taskResults
            } else {
                emptyList()
            },
            notes = if (
                effectiveFilter == SearchFilter.ALL ||
                effectiveFilter == SearchFilter.NOTES
            ) {
                noteResults
            } else {
                emptyList()
            },
            transactions = if (
                effectiveFilter == SearchFilter.ALL ||
                effectiveFilter == SearchFilter.TRANSACTIONS
            ) {
                transactionResults
            } else {
                emptyList()
            },
            courses = if (
                effectiveFilter == SearchFilter.ALL ||
                effectiveFilter == SearchFilter.COURSES
            ) {
                courseResults
            } else {
                emptyList()
            },
            isLoading = false
        )
    }

    val uiState: StateFlow<SearchUiState> = combine(
        resultsFlow,
        preferencesRepository.recentSearches
    ) { state, recentSearches ->
        state.copy(recentSearches = recentSearches)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: SearchFilter) {
        filter.value = value
    }

    /** Reutiliza una consulta reciente y la vuelve a poner al frente del historial. */
    fun useRecentSearch(value: String) {
        query.value = value
        commitSearch(value)
    }

    /** Guarda la consulta actual; se llama al pulsar Buscar en el teclado o un resultado. */
    fun commitSearch() {
        commitSearch(query.value)
    }

    private fun commitSearch(value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { preferencesRepository.addRecentSearch(clean) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { preferencesRepository.clearRecentSearches() }
    }

    private data class FinanceSearchData(
        val transactions: List<Transaction>,
        val accountNames: Map<Long, String>,
        val categoryNames: Map<Long, String>
    )

    private data class SearchLibrary(
        val courses: List<Course>,
        val tasks: List<Task>,
        val notes: List<NoteEntity>,
        val finance: FinanceSearchData,
        val preferences: UserPreferences
    )
}
