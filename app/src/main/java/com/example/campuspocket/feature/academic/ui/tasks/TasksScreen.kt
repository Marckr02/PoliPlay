package com.example.campuspocket.feature.academic.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.components.CampusSelectField
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.components.TaskFilter
import com.example.campuspocket.core.designsystem.components.TaskFilterRow
import com.example.campuspocket.core.designsystem.components.TaskItemCard
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.navigation.LocalSnackbarHost
import com.example.campuspocket.core.navigation.showUndoSnackbar
import com.example.campuspocket.feature.academic.domain.model.Task
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by rememberSaveable { mutableStateOf(TaskFilter.ALL) }
    var selectedCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var courseMenuExpanded by remember { mutableStateOf(false) }
    val ext = MaterialTheme.extended
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHost.current
    val undoLabel = context.getString(R.string.snackbar_undo)
    val deletedMessage = context.getString(R.string.task_deleted_snackbar)
    val completedMessage = context.getString(R.string.task_completed_snackbar)

    LaunchedEffect(viewModel, snackbarHost) {
        viewModel.events.collect { event ->
            when (event) {
                is TasksEvent.Deleted -> {
                    val undo = snackbarHost.showUndoSnackbar(deletedMessage, undoLabel)
                    if (undo) viewModel.undoDelete() else viewModel.commitPendingDeletion()
                }

                is TasksEvent.Completed -> {
                    val undo = snackbarHost.showUndoSnackbar(completedMessage, undoLabel)
                    if (undo) {
                        viewModel.undoCompletion(event.task, event.nextOccurrenceId)
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedFilter, state.courses) {
        if (selectedFilter == TaskFilter.BY_COURSE && selectedCourseId == null) {
            selectedCourseId = state.courses.firstOrNull()?.id
        }
    }

    val today = LocalDate.now(ZoneId.systemDefault())
    val visibleTasks = remember(state.tasks, selectedFilter, selectedCourseId, today) {
        state.tasks.filter { task ->
            val dueDate = task.dueAt.atZone(ZoneId.systemDefault()).toLocalDate()
            when (selectedFilter) {
                TaskFilter.ALL, TaskFilter.COMPLETED -> true
                TaskFilter.TODAY -> dueDate == today
                TaskFilter.THIS_WEEK -> !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(6))
                TaskFilter.BY_COURSE -> task.courseId == selectedCourseId
            }
        }
    }
    val groups = remember(visibleTasks, selectedFilter, today) {
        groupTasks(visibleTasks, selectedFilter, today)
    }
    val selectedCourseName = state.courses.firstOrNull { it.id == selectedCourseId }?.name
        ?: stringResource(R.string.task_filter_by_course)

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(R.string.academic_tasks),
            onBack = onBack,
            actions = {
                RoundIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.task_new),
                    onClick = onAddTask,
                    containerColor = ext.accent,
                    contentColor = ext.onAccent
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TaskFilterRow(
                modifier = Modifier.padding(top = 4.dp),
                selectedFilter = selectedFilter,
                onFilterChange = { filter ->
                    selectedFilter = filter
                    viewModel.setShowCompleted(filter == TaskFilter.COMPLETED)
                }
            )

            if (selectedFilter == TaskFilter.BY_COURSE && state.courses.isNotEmpty()) {
                CampusSelectField(
                    value = selectedCourseName,
                    label = stringResource(R.string.task_filter_by_course),
                    expanded = courseMenuExpanded,
                    onExpandedChange = { courseMenuExpanded = it }
                ) {
                    state.courses.forEach { course ->
                        CampusSelectItem(
                            text = course.name,
                            selected = course.id == selectedCourseId,
                            onClick = {
                                selectedCourseId = course.id
                                courseMenuExpanded = false
                            }
                        )
                    }
                }
            }

            when {
                state.isLoading -> {
                    LoadingShimmer(modifier = Modifier.weight(1f))
                }

                visibleTasks.isEmpty() -> {
                    EmptyStateCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.empty_tasks_here),
                        message = if (selectedFilter == TaskFilter.ALL) {
                            stringResource(R.string.empty_tasks)
                        } else {
                            stringResource(R.string.task_empty_filter)
                        },
                        icon = painterResource(R.drawable.ic_notes),
                        actionText = stringResource(R.string.task_new),
                        onAction = onAddTask
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groups.forEach { (titleRes, tasks) ->
                            if (tasks.isNotEmpty()) {
                                item(key = "header-$titleRes") {
                                    Text(
                                        text = stringResource(titleRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                                items(tasks, key = { it.id ?: 0L }) { task ->
                                    TaskItemCard(
                                        modifier = Modifier.animateItem(),
                                        task = task,
                                        courseName = state.courses.firstOrNull { it.id == task.courseId }?.name,
                                        onClick = { task.id?.let(onOpenTask) },
                                        onToggleCompleted = { done -> viewModel.setCompleted(task, done) },
                                        onDelete = { viewModel.delete(task) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun groupTasks(
    tasks: List<Task>,
    filter: TaskFilter,
    today: LocalDate
): List<Pair<Int, List<Task>>> {
    if (filter == TaskFilter.COMPLETED) {
        return listOf(R.string.task_filter_completed to tasks)
    }
    val zone = ZoneId.systemDefault()
    val tomorrow = today.plusDays(1)
    val weekEnd = today.plusDays(6)
    return listOf(
        R.string.task_group_overdue to tasks.filter {
            it.dueAt.atZone(zone).toLocalDate().isBefore(today)
        },
        R.string.academic_today to tasks.filter {
            it.dueAt.atZone(zone).toLocalDate() == today
        },
        R.string.task_group_tomorrow to tasks.filter {
            it.dueAt.atZone(zone).toLocalDate() == tomorrow
        },
        R.string.task_filter_week to tasks.filter {
            val date = it.dueAt.atZone(zone).toLocalDate()
            date.isAfter(tomorrow) && !date.isAfter(weekEnd)
        },
        R.string.task_group_later to tasks.filter {
            it.dueAt.atZone(zone).toLocalDate().isAfter(weekEnd)
        }
    )
}

@Composable
@Preview(name = "TasksScreen", showBackground = true)
fun PreviewTasksScreen() {
    CampusTheme {
        Surface {
            TasksScreen(
                onAddTask = {},
                onOpenTask = {},
                onBack = {}
            )
        }
    }
}
