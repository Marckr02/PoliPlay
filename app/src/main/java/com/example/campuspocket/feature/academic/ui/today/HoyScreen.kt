package com.example.campuspocket.feature.academic.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusSectionHeader
import com.example.campuspocket.core.designsystem.components.DayStrip
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.components.TaskItemCard
import com.example.campuspocket.core.designsystem.components.TimelineRow
import com.example.campuspocket.feature.academic.domain.model.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Hoy: tira de días, línea de tiempo de clases y tareas que vencen hoy.
 * Sin FAB: el dock contextual es el único punto de creación.
 */
@Composable
fun HoyScreen(
    onShowWeek: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    modifier: Modifier = Modifier,
    state: HoyUiState = HoyUiState(),
    onSelectDate: (LocalDate) -> Unit = {},
    onToggleTask: (Task, Boolean) -> Unit = { _, _ -> },
    onOpenTask: (Long) -> Unit = {}
) {
    val dateText = state.selectedDate.format(
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es"))
    ).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es")) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.today_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(
                    icon = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.today_open_week),
                    onClick = onShowWeek,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                RoundIconButton(
                    icon = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.today_open_tasks),
                    onClick = onNavigateToTasks,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                RoundIconButton(
                    iconRes = R.drawable.ic_schedule,
                    contentDescription = stringResource(R.string.today_open_courses),
                    onClick = onNavigateToCourses,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (state.weekDays.isNotEmpty()) {
            DayStrip(
                days = state.weekDays,
                selectedDate = state.selectedDate,
                onSelect = onSelectDate
            )
        }

        CampusSectionHeader(
            title = stringResource(R.string.today_agenda),
            actionText = stringResource(R.string.today_open_week),
            onAction = onShowWeek
        )

        if (state.classes.isEmpty()) {
            BentoCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
                Text(
                    text = stringResource(R.string.today_no_classes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.today_no_classes_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.classes.forEach { entry ->
                    TimelineRow(
                        startTime = entry.startLabel,
                        endTime = entry.endLabel,
                        courseName = entry.courseName,
                        room = entry.room,
                        courseColor = Color(entry.courseColorArgb),
                        isCurrent = entry.isCurrent,
                        onClick = { onOpenCourse(entry.courseId) },
                        progress = if (entry.isCurrent) entry.progress else null
                    )
                }
            }
        }

        CampusSectionHeader(
            title = stringResource(R.string.today_tasks_title),
            actionText = stringResource(R.string.today_open_tasks),
            onAction = onNavigateToTasks
        )

        if (state.tasks.isEmpty()) {
            BentoCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
                Text(
                    text = stringResource(R.string.today_no_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tasks.forEach { entry ->
                    TaskItemCard(
                        task = entry.task,
                        courseName = entry.courseName,
                        onClick = { entry.task.id?.let(onOpenTask) },
                        onToggleCompleted = { done -> onToggleTask(entry.task, done) },
                        onDelete = {},
                        onSwipeToComplete = { onToggleTask(entry.task, true) },
                        onSwipeToDelete = {}
                    )
                }
            }
        }
    }
}
