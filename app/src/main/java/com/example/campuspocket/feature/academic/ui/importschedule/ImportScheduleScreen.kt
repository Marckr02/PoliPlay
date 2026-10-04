package com.example.campuspocket.feature.academic.ui.importschedule

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.domain.model.SchoolDays
import com.example.campuspocket.feature.academic.importer.ParseWarning
import com.example.campuspocket.feature.academic.ui.TimeField
import com.example.campuspocket.feature.academic.ui.TimeText
import com.example.campuspocket.feature.academic.ui.shortLabelRes
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScheduleScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // SAF: lectura puntual del documento elegido; no se piden permisos de red.
        val stream = context.contentResolver.openInputStream(uri)
        if (stream == null) viewModel.onStreamError() else viewModel.onPdfPicked(stream)
    }

    LaunchedEffect((state as? ImportUiState.Review)?.saved) {
        if ((state as? ImportUiState.Review)?.saved == true) onDone()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_pdf_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val current = state) {
            ImportUiState.Idle -> PickPdfView(
                errorRes = null,
                onPick = { launcher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.padding(innerPadding)
            )

            ImportUiState.Reading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            is ImportUiState.ReadError -> PickPdfView(
                errorRes = current.messageRes,
                onPick = { launcher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.padding(innerPadding)
            )

            is ImportUiState.Review -> ReviewContent(
                state = current,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/** Vista de selección: también sirve de mensaje claro cuando el PDF falla (sin cerrar la pantalla). */
@Composable
private fun PickPdfView(
    errorRes: Int?,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.import_pdf_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Button(onClick = onPick) {
            Text(stringResource(R.string.import_pdf_select))
        }
    }
}

@Composable
private fun ReviewContent(
    state: ImportUiState.Review,
    viewModel: ImportScheduleViewModel,
    modifier: Modifier = Modifier
) {
    var editingSession by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                state.saveError?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = viewModel::confirm,
                    enabled = state.canSave && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.import_pdf_confirm))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SemesterCard(
                    state = state,
                    onTermNameChange = viewModel::onTermNameChange
                )
            }

            if (state.warnings.isNotEmpty()) {
                item {
                    WarningsBanner(state.warnings)
                }
            }

            itemsIndexed(state.courses) { index, course ->
                CourseReviewCard(
                    course = course,
                    onChange = { viewModel.onCourseChange(index, it) },
                    onRemove = { viewModel.onRemoveCourse(index) },
                    onEditSession = { sessionIndex -> editingSession = index to sessionIndex },
                    onRemoveSession = { sessionIndex -> viewModel.onRemoveSession(index, sessionIndex) },
                    onAddSession = { viewModel.onAddSession(index) }
                )
            }
        }
    }

    editingSession?.let { (courseIndex, sessionIndex) ->
        val session = state.courses.getOrNull(courseIndex)?.sessions?.getOrNull(sessionIndex)
        if (session != null) {
            SessionEditDialog(
                initial = session,
                onDismiss = { editingSession = null },
                onApply = { updated ->
                    viewModel.onSessionChange(courseIndex, sessionIndex, updated)
                    editingSession = null
                }
            )
        } else {
            editingSession = null
        }
    }

    if (state.absentCourses != null) {
        AlertDialog(
            onDismissRequest = viewModel::onAbsentDeleteDismissed,
            confirmButton = {
                TextButton(onClick = viewModel::onAbsentDeleteConfirmed) {
                    Text(stringResource(R.string.import_merge_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onAbsentDeleteDismissed) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(state.termName) },
            text = {
                Text(
                    stringResource(
                        R.string.import_merge_absent_warning,
                        state.absentCourses.size,
                        state.absentCourses.joinToString(", ") { it.name }
                    )
                )
            }
        )
    }
}

@Composable
private fun SemesterCard(
    state: ImportUiState.Review,
    onTermNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.termName,
                onValueChange = onTermNameChange,
                label = { Text(stringResource(R.string.semester_name)) },
                isError = state.termNameError,
                supportingText = if (state.termNameError) {
                    { Text(stringResource(R.string.error_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.startDate != null && state.endDate != null) {
                val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es"))
                Text(
                    text = state.startDate.format(formatter) + " – " + state.endDate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WarningsBanner(warnings: List<ParseWarning>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.import_pdf_warnings),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            warnings.forEach { warning ->
                Text(
                    text = warningText(warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun warningText(warning: ParseWarning): String = when (warning) {
    ParseWarning.NoText -> stringResource(R.string.import_pdf_no_text)
    ParseWarning.UnsupportedFormat -> stringResource(R.string.import_unsupported_format)
    is ParseWarning.RowWithoutSchedule ->
        stringResource(R.string.import_warn_row_without_schedule, warning.nro)
    is ParseWarning.UnexpectedCode ->
        stringResource(R.string.import_warn_unexpected_code, warning.code, warning.nro)
}

@Composable
private fun CourseReviewCard(
    course: ImportDraftCourse,
    onChange: (ImportDraftCourse) -> Unit,
    onRemove: () -> Unit,
    onEditSession: (Int) -> Unit,
    onRemoveSession: (Int) -> Unit,
    onAddSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = course.code,
                    onValueChange = { onChange(course.copy(code = it)) },
                    label = { Text(stringResource(R.string.course_code)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = course.section,
                    onValueChange = { onChange(course.copy(section = it)) },
                    label = { Text(stringResource(R.string.course_section)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = course.name,
                onValueChange = { onChange(course.copy(name = it)) },
                label = { Text(stringResource(R.string.course_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = course.teacher,
                onValueChange = { onChange(course.copy(teacher = it)) },
                label = { Text(stringResource(R.string.course_teacher)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            course.sessions.forEachIndexed { index, session ->
                SessionReviewRow(
                    session = session,
                    onEdit = { onEditSession(index) },
                    onRemove = { onRemoveSession(index) }
                )
            }
            TextButton(onClick = onAddSession) {
                Text(stringResource(R.string.course_add_session))
            }
        }
    }
}

@Composable
private fun SessionReviewRow(
    session: ImportDraftSession,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = listOfNotNull(
                session.day?.let { stringResource(it.shortLabelRes()) },
                session.start + " – " + session.end,
                session.room.ifBlank { null }
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = if (session.isValid) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.import_session_edit)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.delete)
            )
        }
    }
}

/** Editor de una sesión de la revisión: día, horas (con TimePicker de 24 h) y aula. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionEditDialog(
    initial: ImportDraftSession,
    onDismiss: () -> Unit,
    onApply: (ImportDraftSession) -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    val startTime = TimeText.parse(draft.start)
    val endTime = TimeText.parse(draft.end)
    val orderError = startTime != null && endTime != null && !endTime.isAfter(startTime)
    val valid = draft.day != null && startTime != null && endTime != null && !orderError

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(draft) }, enabled = valid) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.import_session_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SchoolDays.withSaturday.forEach { day ->
                        FilterChip(
                            selected = draft.day == day,
                            onClick = { draft = draft.copy(day = day) },
                            label = { Text(stringResource(day.shortLabelRes())) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(
                        labelRes = R.string.session_start,
                        value = draft.start,
                        errorRes = if (draft.start.isNotBlank() && startTime == null) {
                            R.string.error_time_format
                        } else {
                            null
                        },
                        onTimeSelected = { time -> draft = draft.copy(start = TimeText.format(time)) },
                        modifier = Modifier.weight(1f)
                    )
                    TimeField(
                        labelRes = R.string.session_end,
                        value = draft.end,
                        errorRes = when {
                            orderError -> R.string.error_time_order
                            draft.end.isNotBlank() && endTime == null -> R.string.error_time_format
                            else -> null
                        },
                        onTimeSelected = { time -> draft = draft.copy(end = TimeText.format(time)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = draft.room,
                    onValueChange = { draft = draft.copy(room = it) },
                    label = { Text(stringResource(R.string.session_room)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
