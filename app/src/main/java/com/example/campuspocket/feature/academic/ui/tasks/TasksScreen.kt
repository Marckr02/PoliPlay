package com.example.campuspocket.feature.academic.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.PriorityBadge
import com.example.campuspocket.core.designsystem.components.TaskFilterRow
import com.example.campuspocket.core.designsystem.components.TaskItemCard
import com.example.campuspocket.core.designsystem.components.TaskFilter
import com.example.campuspocket.feature.academic.domain.model.Task

@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Task?>(null) }
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }

    CampusTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.academic_tasks),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TaskFilterRow(
                modifier = Modifier.padding(top = 8.dp),
                selectedFilter = selectedFilter,
                onFilterChange = { filter ->
                    selectedFilter = filter
                    when (filter) {
                        TaskFilter.ALL -> viewModel.setShowCompleted(false)
                        TaskFilter.TODAY -> viewModel.setShowCompleted(false)
                        TaskFilter.THIS_WEEK -> viewModel.setShowCompleted(false)
                        TaskFilter.BY_COURSE -> viewModel.setShowCompleted(false)
                    }
                }
            )

            when {
                state.isLoading -> {
                    LoadingShimmer(modifier = Modifier.weight(1f))
                }

                state.tasks.isEmpty() -> {
                    EmptyStateCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.empty_tasks_here),
                        message = stringResource(R.string.empty_tasks),
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
                        items(state.tasks, key = { it.id ?: 0L }) { task ->
                            TaskItemCard(
                                task = task,
                                courseName = state.courses.firstOrNull { it.id == task.courseId }?.name,
                                onClick = { task.id?.let(onOpenTask) },
                                onToggleCompleted = { done -> viewModel.setCompleted(task, done) },
                                onDelete = { pendingDelete = task },
                                onSwipeToComplete = { viewModel.setCompleted(task, true) },
                                onSwipeToDelete = { pendingDelete = task }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        task.id?.let { viewModel.delete(it) }
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(task.title) },
            text = { Text(stringResource(R.string.delete_task_confirm)) }
        )
    }
}

@Composable
@Preview(name = "TasksScreen", showBackground = true)
fun PreviewTasksScreen() {
    CampusTheme {
        Surface {
            TasksScreen(
                onAddTask = {},
                onOpenTask = {}
            )
        }
    }
}