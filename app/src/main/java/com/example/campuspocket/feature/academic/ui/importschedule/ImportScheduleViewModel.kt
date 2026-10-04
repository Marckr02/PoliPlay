package com.example.campuspocket.feature.academic.ui.importschedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.domain.model.SemesterTerm
import com.example.campuspocket.feature.academic.importer.ImportScheduleUseCase
import com.example.campuspocket.feature.academic.importer.ImportedCourse
import com.example.campuspocket.feature.academic.importer.ImportedSession
import com.example.campuspocket.feature.academic.importer.ParseWarning
import com.example.campuspocket.feature.academic.importer.ParsedSchedule
import com.example.campuspocket.feature.academic.importer.PdfTextExtractor
import com.example.campuspocket.feature.academic.importer.ScheduleTableParser
import com.example.campuspocket.feature.academic.ui.TimeText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named

/** Sesión editable en la pantalla de revisión (horas como texto "H:mm"). */
data class ImportDraftSession(
    val day: DayOfWeek?,
    val start: String,
    val end: String,
    val room: String,
    val credits: Int?
) {
    val isValid: Boolean
        get() {
            val startTime = TimeText.parse(start)
            val endTime = TimeText.parse(end)
            return day != null && startTime != null && endTime != null && endTime.isAfter(startTime)
        }
}

/** Materia editable en la revisión (agrupa las filas del PDF con el mismo código y paralelo). */
data class ImportDraftCourse(
    val code: String,
    val name: String,
    val section: String,
    val teacher: String,
    val sessions: List<ImportDraftSession>
)

/** Materia guardada que el PDF ya no trae: solo se borra si el usuario lo confirma. */
data class AbsentCourse(val id: Long, val name: String)

sealed interface ImportUiState {
    /** Sin PDF seleccionado todavía. */
    data object Idle : ImportUiState

    data object Reading : ImportUiState

    /** PDF ilegible, sin texto, o formato no soportado: mensaje claro y la pantalla NO se cierra. */
    data class ReadError(val messageRes: Int) : ImportUiState

    /** Revisión obligatoria: todo editable antes de guardar. */
    data class Review(
        val termName: String,
        val startDate: LocalDate?,
        val endDate: LocalDate?,
        val courses: List<ImportDraftCourse>,
        val warnings: List<ParseWarning>,
        val termNameError: Boolean = false,
        /** Materias guardadas que el PDF ya no incluye; pendiente de confirmación para borrarlas. */
        val absentCourses: List<AbsentCourse>? = null,
        val isSaving: Boolean = false,
        val saveError: Int? = null,
        val saved: Boolean = false
    ) : ImportUiState {
        val canSave: Boolean
            get() = courses.isNotEmpty() && termName.isNotBlank() &&
                courses.all { course -> course.sessions.isNotEmpty() && course.sessions.all { it.isValid } }
    }
}

@HiltViewModel
class ImportScheduleViewModel @Inject constructor(
    private val extractor: PdfTextExtractor,
    private val parser: ScheduleTableParser,
    private val importSchedule: ImportScheduleUseCase,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    /** Lee, extrae y analiza el PDF elegido. El caller cierra el stream (lo hace con use{}). */
    fun onPdfPicked(stream: InputStream) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Reading
            try {
                val texts = withContext(ioDispatcher) { stream.use { extractor.extract(it) } }
                if (texts.isEmpty()) {
                    // Sin capa de texto: muy probablemente es imagen (Microsoft Print to PDF).
                    _uiState.value = ImportUiState.ReadError(R.string.import_pdf_no_text)
                    return@launch
                }
                val parsed = parser.parse(texts)
                if (parsed.rows.isEmpty()) {
                    _uiState.value = ImportUiState.ReadError(R.string.import_unsupported_format)
                    return@launch
                }
                _uiState.value = buildReview(parsed)
            } catch (e: Exception) {
                _uiState.value = ImportUiState.ReadError(R.string.import_pdf_invalid)
            }
        }
    }

    fun onStreamError() {
        _uiState.value = ImportUiState.ReadError(R.string.import_pdf_invalid)
    }

    fun onPickAgain() {
        _uiState.value = ImportUiState.Idle
    }

    // ---- Edición de la revisión -------------------------------------------------

    fun onTermNameChange(value: String) = updateReview {
        it.copy(termName = value, termNameError = false)
    }

    fun onCourseChange(index: Int, course: ImportDraftCourse) = updateReview {
        it.copy(courses = it.courses.toMutableList().apply { set(index, course) })
    }

    fun onRemoveCourse(index: Int) = updateReview {
        it.copy(courses = it.courses.toMutableList().apply { removeAt(index) })
    }

    fun onAddSession(courseIndex: Int) = updateReview {
        it.copy(
            courses = it.courses.toMutableList().apply {
                val course = get(courseIndex)
                set(
                    courseIndex,
                    course.copy(
                        sessions = course.sessions + ImportDraftSession(
                            day = DayOfWeek.MONDAY,
                            start = "7:00",
                            end = "9:00",
                            room = "",
                            credits = null
                        )
                    )
                )
            }
        )
    }

    fun onSessionChange(courseIndex: Int, sessionIndex: Int, session: ImportDraftSession) =
        updateReview {
            it.copy(
                courses = it.courses.toMutableList().apply {
                    val course = get(courseIndex)
                    set(
                        courseIndex,
                        course.copy(
                            sessions = course.sessions.toMutableList()
                                .apply { set(sessionIndex, session) }
                        )
                    )
                }
            )
        }

    fun onRemoveSession(courseIndex: Int, sessionIndex: Int) = updateReview {
        it.copy(
            courses = it.courses.toMutableList().apply {
                val course = get(courseIndex)
                set(
                    courseIndex,
                    course.copy(sessions = course.sessions.toMutableList().apply { removeAt(sessionIndex) })
                )
            }
        )
    }

    // ---- Confirmación con fusión -----------------------------------------------

    /**
     * Si el semestre ya existe y el PDF ya no incluye algunas de sus materias,
     * se pide confirmación para borrarlas; si no, se guarda directamente.
     */
    fun confirm() {
        val review = _uiState.value as? ImportUiState.Review ?: return
        if (!review.canSave || review.isSaving) return
        if (review.termName.isBlank()) {
            updateReview { it.copy(termNameError = true) }
            return
        }
        viewModelScope.launch {
            val existing = withContext(ioDispatcher) {
                importSchedule.semesterCourses(review.termName.trim())
            }
            val pdfKeys = review.courses.map { it.code.trim() to it.section.trim() }.toSet()
            val absent = existing
                .filter { (it.code to it.section) !in pdfKeys }
                .map { AbsentCourse(it.id, it.name) }
            if (absent.isEmpty()) {
                saveNow(emptyList())
            } else {
                updateReview { it.copy(absentCourses = absent) }
            }
        }
    }

    /** Borrar las ausentes y guardar. */
    fun onAbsentDeleteConfirmed() {
        val review = _uiState.value as? ImportUiState.Review ?: return
        val ids = review.absentCourses.orEmpty().map { it.id }
        saveNow(ids)
    }

    fun onAbsentDeleteDismissed() = updateReview { it.copy(absentCourses = null) }

    private fun saveNow(deleteAbsentCourseIds: List<Long>) {
        viewModelScope.launch {
            val review = _uiState.value as? ImportUiState.Review ?: return@launch
            updateReview { it.copy(isSaving = true, absentCourses = null, saveError = null) }
            try {
                val fallback = SemesterTerm.forDate(LocalDate.now())
                val courses = review.courses.map { course ->
                    ImportedCourse(
                        code = course.code.trim(),
                        name = course.name.trim(),
                        section = course.section.trim(),
                        teacher = course.teacher.trim().ifBlank { null },
                        sessions = course.sessions.map { session ->
                            ImportedSession(
                                dayOfWeek = session.day!!.value,
                                startMinute = session.start.toMinutes(),
                                endMinute = session.end.toMinutes(),
                                room = session.room.trim().ifBlank { null },
                                credits = session.credits
                            )
                        }
                    )
                }
                withContext(ioDispatcher) {
                    importSchedule.save(
                        termName = review.termName.trim(),
                        startDate = review.startDate ?: fallback.start,
                        endDate = review.endDate ?: fallback.end,
                        courses = courses,
                        deleteAbsentCourseIds = deleteAbsentCourseIds
                    )
                }
                updateReview { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                updateReview { it.copy(isSaving = false, saveError = R.string.error_generic) }
            }
        }
    }

    // ---------------------------------------------------------------------------

    private fun buildReview(parsed: ParsedSchedule): ImportUiState.Review {
        // Agrupa las filas por (código, paralelo): cada grupo es una materia con sus sesiones.
        val grouped = parsed.rows.groupBy { it.code to it.section }.values.map { groupRows ->
            val first = groupRows.first()
            ImportDraftCourse(
                code = first.code,
                name = first.name,
                section = first.section,
                teacher = first.teacher,
                sessions = groupRows.flatMap { row ->
                    row.slots.map { slot ->
                        ImportDraftSession(
                            day = DayOfWeek.of(slot.dayOfWeek),
                            start = TimeText.format(LocalTime.of(slot.startMinute / 60, slot.startMinute % 60)),
                            end = TimeText.format(LocalTime.of(slot.endMinute / 60, slot.endMinute % 60)),
                            room = row.room.orEmpty(),
                            credits = row.credits
                        )
                    }
                }.sortedWith(compareBy({ it.day?.value ?: 8 }, { it.start }))
            )
        }
        return ImportUiState.Review(
            termName = parsed.termLabel ?: SemesterTerm.forDate(LocalDate.now()).name,
            startDate = parsed.startDate,
            endDate = parsed.endDate,
            courses = grouped,
            warnings = parsed.warnings
        )
    }

    private fun updateReview(transform: (ImportUiState.Review) -> ImportUiState.Review) {
        _uiState.update { current ->
            (current as? ImportUiState.Review)?.let(transform) ?: current
        }
    }

    private fun String.toMinutes(): Int {
        val time = TimeText.parse(this)!!
        return time.hour * 60 + time.minute
    }
}
