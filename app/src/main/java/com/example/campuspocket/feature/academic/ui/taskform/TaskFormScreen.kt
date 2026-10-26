package com.example.campuspocket.feature.academic.ui.taskform

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusButtonStyle
import com.example.campuspocket.core.designsystem.components.CampusCheckbox
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.CampusDatePickerDialog
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSelectField
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.campusDatePickerColors
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
import com.example.campuspocket.core.designsystem.components.SegmentedControl
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.feature.academic.domain.model.Priority
import com.example.campuspocket.feature.academic.domain.model.RepeatRule
import com.example.campuspocket.feature.academic.ui.TimePickerDialog
import com.example.campuspocket.feature.academic.ui.TimeText
import java.time.LocalDate
import androidx.compose.ui.tooling.preview.Preview

private val Priority.resourceString: Int
    get() = when (this) {
        Priority.HIGH -> R.string.task_priority_high
        Priority.MEDIUM -> R.string.task_priority_medium
        Priority.LOW -> R.string.task_priority_low
    }

private val REMINDER_LABELS = mapOf(
    0 to R.string.reminder_at_time,
    10 to R.string.reminder_10min,
    60 to R.string.reminder_1h,
    24 * 60 to R.string.reminder_1day
)

@Composable
fun TaskFormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val linkedNotes by viewModel.linkedNotes.collectAsStateWithLifecycle(emptyList<com.example.campuspocket.feature.notes.data.NoteEntity>())
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    TaskFormContent(
        state = state,
        courses = courses,
        linkedNotes = linkedNotes,
        onBack = onDone,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onCourseChange = viewModel::onCourseChange,
        onDateChange = viewModel::onDateChange,
        onTimeChange = viewModel::onTimeChange,
        onPriorityChange = viewModel::onPriorityChange,
        onToggleReminder = viewModel::toggleReminder,
        onSubtaskDraftChange = viewModel::onSubtaskDraftChange,
        onAddSubtask = viewModel::addSubtask,
        onRemoveSubtask = viewModel::removeSubtask,
        onToggleSubtaskDone = viewModel::toggleSubtaskDone,
        onRepeatRuleChange = viewModel::onRepeatRuleChange,
        onRepeatUntilChange = viewModel::onRepeatUntilChange,
        onUseSemesterEnd = viewModel::useSemesterEnd,
        onSave = viewModel::save,
        onToggleCompleted = viewModel::setCompleted,
        onDeleteRequest = { showDeleteDialog = true },
        modifier = modifier
    )

    if (showDeleteDialog) {
        CampusDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = state.title,
            message = stringResource(R.string.delete_task_confirm),
            icon = Icons.Filled.Warning,
            destructive = true,
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormContent(
    state: TaskFormUiState,
    courses: List<com.example.campuspocket.feature.academic.domain.model.Course>,
    linkedNotes: List<com.example.campuspocket.feature.notes.data.NoteEntity>,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCourseChange: (Long?) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (java.time.LocalTime) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onToggleReminder: (Int) -> Unit,
    onSubtaskDraftChange: (String) -> Unit,
    onAddSubtask: () -> Unit,
    onRemoveSubtask: (Int) -> Unit,
    onToggleSubtaskDone: (Int) -> Unit,
    onRepeatRuleChange: (RepeatRule) -> Unit,
    onRepeatUntilChange: (LocalDate) -> Unit,
    onUseSemesterEnd: () -> Unit,
    onSave: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRepeatEndPicker by remember { mutableStateOf(false) }

    // Estado de permisos, releído al volver de los ajustes o del diálogo del sistema.
    var notificationsGranted by remember { mutableStateOf(areNotificationsGranted(context)) }
    var exactAlarmsGranted by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = areNotificationsGranted(context)
                exactAlarmsGranted = canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showPermissionDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(
                if (state.isEditing) R.string.task_edit else R.string.task_new
            ),
            onBack = onBack,
            actions = {
                if (state.isEditing && !state.notFound) {
                    RoundIconButton(
                        icon = Icons.Filled.Check,
                        contentDescription = stringResource(
                            if (state.completedAt == null) {
                                R.string.task_mark_completed
                            } else {
                                R.string.task_mark_pending
                            }
                        ),
                        onClick = { onToggleCompleted(state.completedAt == null) },
                        containerColor = MaterialTheme.extended.accent,
                        contentColor = MaterialTheme.extended.onAccent
                    )
                }
            }
        )
        if (!state.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampusTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = stringResource(R.string.task_title),
                isError = state.titleError,
                supportingText = {
                    if (state.titleError) Text(stringResource(R.string.error_required))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            CampusTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.task_description),
                modifier = Modifier.fillMaxWidth()
            )

            CoursePicker(
                courses = courses,
                selectedCourseId = state.courseId,
                onCourseChange = onCourseChange
            )

            // Fecha (DatePicker del sistema Material) y hora (TimePicker 24 h, como en materias).
            Column {
                Text(
                    text = stringResource(R.string.task_due_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryPillButton(
                    text = java.time.format.DateTimeFormatter.ofPattern(
                        "EEE d 'de' MMM 'de' yyyy",
                        java.util.Locale.forLanguageTag("es")
                    ).format(state.date),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.task_due_time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryPillButton(
                    text = TimeText.format(state.time),
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.task_priority),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SegmentedControl(
                    options = listOf(Priority.LOW, Priority.MEDIUM, Priority.HIGH),
                    selected = state.priority,
                    onSelect = onPriorityChange,
                    labelProvider = { stringResource(it.resourceString) }
                )
            }

            // Subtareas (v5 §3.3): título, check de 20 dp y filas de 48 dp.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.task_subtasks_section,
                            state.completedSubtasks,
                            state.subtasks.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.subtasks.isNotEmpty()) {
                        Text(
                            text = "${state.completedSubtasks}/${state.subtasks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.subtasks.forEachIndexed { index, subtask ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CampusCheckbox(
                            checked = subtask.done,
                            onCheckedChange = { onToggleSubtaskDone(index) },
                            contentDescription = subtask.title,
                            boxSize = 20.dp
                        )
                        Text(
                            text = subtask.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (subtask.done) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemoveSubtask(index) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.task_subtask_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                CampusTextField(
                    value = state.subtaskDraft,
                    onValueChange = onSubtaskDraftChange,
                    label = stringResource(R.string.task_subtask_add),
                    placeholder = stringResource(R.string.task_subtask_add),
                    isError = state.subtaskError,
                    supportingText = if (state.subtaskError) {
                        { Text(stringResource(R.string.task_subtask_empty_error)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddSubtask() }),
                    trailingIcon = {
                        IconButton(
                            onClick = onAddSubtask,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.task_subtask_add_action),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.isEditing && state.allSubtasksDone && state.completedAt == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.task_subtasks_all_done),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        CampusChip(
                            label = stringResource(R.string.task_complete_now),
                            selected = false,
                            onClick = { onToggleCompleted(true) }
                        )
                    }
                }
            }

            // Repetición (v5 §3.5), con fin opcional en el semestre activo.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.task_repeat_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatRule.entries.forEach { rule ->
                        CampusChip(
                            label = stringResource(
                                when (rule) {
                                    RepeatRule.NONE -> R.string.task_repeat_none
                                    RepeatRule.DAILY -> R.string.task_repeat_daily
                                    RepeatRule.WEEKLY -> R.string.task_repeat_weekly
                                }
                            ),
                            selected = state.repeatRule == rule,
                            onClick = { onRepeatRuleChange(rule) }
                        )
                    }
                }
                if (state.repeatRule != RepeatRule.NONE) {
                    Text(
                        text = stringResource(R.string.task_repeat_until),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SecondaryPillButton(
                        text = java.time.format.DateTimeFormatter.ofPattern(
                            "d MMM yyyy",
                            java.util.Locale.forLanguageTag("es")
                        ).format(state.repeatUntil ?: state.semesterEnd ?: state.date),
                        onClick = { showRepeatEndPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.semesterEnd != null) {
                        CampusChip(
                            label = stringResource(R.string.task_repeat_semester_end),
                            selected = state.repeatUntil == state.semesterEnd,
                            onClick = onUseSemesterEnd
                        )
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.task_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 10, 60, 24 * 60).forEach { minutes ->
                        val labelRes = REMINDER_LABELS.getValue(minutes)
                        CampusChip(
                            label = stringResource(labelRes),
                            selected = minutes in state.reminderMinutes,
                            onClick = {
                                if (minutes !in state.reminderMinutes && !notificationsGranted &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ) {
                                    // Primero se explica para qué sirve el permiso.
                                    showPermissionDialog = true
                                }
                                onToggleReminder(minutes)
                            }
                        )
                    }
                }
                if (state.reminderMinutes.isNotEmpty()) {
                    if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Text(
                            text = stringResource(R.string.notification_perm_denied),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (!exactAlarmsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.exact_alarm_needed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.exact_alarm_open_settings),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 14.dp)
                                    .clickable {
                                        context.startActivity(
                                            Intent(
                                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                            ).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            // Notas vinculadas a esta tarea (Fase 6)
            if (state.isEditing && linkedNotes.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.notes_list),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    linkedNotes.forEach { note ->
                        Text(
                            text = note.title.ifBlank { stringResource(R.string.notes_empty_title) } +
                                " — " + note.content.take(60).replace('\n', ' '),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        CampusButton(
            text = stringResource(R.string.save),
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (state.isEditing) {
            CampusButton(
                text = stringResource(R.string.delete),
                onClick = onDeleteRequest,
                style = CampusButtonStyle.DESTRUCTIVE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            )
        }
    }

    if (showDatePicker) {
        CampusDatePickerDialog(
            title = stringResource(R.string.task_due_date),
            initialDate = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { onDateChange(it); showDatePicker = false }
        )
    }

    if (showRepeatEndPicker) {
        val initialDate = state.repeatUntil ?: state.semesterEnd ?: state.date
        CampusDatePickerDialog(
            title = stringResource(R.string.task_repeat_until),
            initialDate = initialDate,
            onDismiss = { showRepeatEndPicker = false },
            onConfirm = { onRepeatUntilChange(it); showRepeatEndPicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            titleRes = R.string.task_due_time,
            initialTime = state.time,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                onTimeChange(time)
            }
        )
    }

    if (showPermissionDialog) {
        CampusDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = stringResource(R.string.notification_perm_title),
            message = stringResource(R.string.notification_perm_message),
            confirmText = stringResource(R.string.notification_perm_grant),
            dismissText = stringResource(R.string.notification_perm_later),
            onConfirm = {
                showPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoursePicker(
    courses: List<com.example.campuspocket.feature.academic.domain.model.Course>,
    selectedCourseId: Long?,
    onCourseChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = courses.firstOrNull { it.id == selectedCourseId }?.name
        ?: stringResource(R.string.task_no_course)

    CampusSelectField(
        value = selectedName,
        label = stringResource(R.string.task_course),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        CampusSelectItem(
            text = stringResource(R.string.task_no_course),
            selected = selectedCourseId == null,
            onClick = {
                onCourseChange(null)
                expanded = false
            }
        )
        courses.forEach { course ->
            CampusSelectItem(
                text = course.name,
                selected = course.id == selectedCourseId,
                onClick = {
                    onCourseChange(course.id)
                    expanded = false
                }
            )
        }
    }
}

private fun areNotificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

private fun canScheduleExactAlarms(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

@Composable
@Preview(name = "TaskFormScreen", showBackground = true)
fun PreviewTaskFormScreen() {
    com.example.campuspocket.core.designsystem.CampusTheme {
        androidx.compose.material3.Surface {
            TaskFormScreen(onDone = {})
        }
    }
}