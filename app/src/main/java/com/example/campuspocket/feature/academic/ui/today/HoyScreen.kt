package com.example.campuspocket.feature.academic.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.HeroCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.ui.SessionRow
import com.example.campuspocket.feature.academic.ui.TimeText
import com.example.campuspocket.feature.academic.ui.TodayWeekSwitch
import com.example.campuspocket.feature.academic.ui.labelRes
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoyScreen(
    onShowWeek: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTime = remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime.value = LocalTime.now()
        }
    }

    val (currentSession, nextSession) = remember(state.entries, currentTime.value) {
        findCurrentAndNextSession(state.entries, currentTime.value)
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.task_new)) }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TodayWeekSwitch(
                    isTodaySelected = true,
                    onShowToday = { /* ya estamos aquí */ },
                    onShowWeek = onShowWeek,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(state.date.dayOfWeek.labelRes()) + ", " +
                            state.date.format(
                                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                    .withLocale(Locale.forLanguageTag("es"))
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
                        TextButton(onClick = onNavigateToTasks) {
                            Text(stringResource(R.string.academic_tasks))
                        }
                        TextButton(onClick = onNavigateToCourses) {
                            Text(stringResource(R.string.academic_manage_courses))
                        }
                    }
                }

                when {
                    state.isLoading -> {
                        LoadingShimmer(modifier = Modifier.weight(1f))
                    }

                    currentSession != null -> {
                        HeroCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.academic_current_class),
                            subtitle = currentSession.courseName,
                            timeRange = stringResource(
                                R.string.session_time_range,
                                TimeText.format(currentSession.session.startTime),
                                TimeText.format(currentSession.session.endTime)
                            ),
                            location = currentSession.session.room,
                            progress = calculateProgress(currentSession.session, currentTime.value),
                            onClick = { onOpenCourse(currentSession.courseId) }
                        )
                        OtherClassesSection(
                            entries = state.entries.filter { it != currentSession },
                            onOpenCourse = onOpenCourse
                        )
                    }

                    nextSession != null -> {
                        HeroCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.academic_next_class),
                            subtitle = nextSession.courseName,
                            timeRange = stringResource(
                                R.string.session_time_range,
                                TimeText.format(nextSession.session.startTime),
                                TimeText.format(nextSession.session.endTime)
                            ),
                            location = nextSession.session.room,
                            progress = 0f,
                            onClick = { onOpenCourse(nextSession.courseId) }
                        )
                        OtherClassesSection(
                            entries = state.entries.filter { it != nextSession },
                            onOpenCourse = onOpenCourse
                        )
                    }

                    else -> {
                        EmptyStateCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.academic_no_classes_today),
                            message = stringResource(R.string.empty_academic),
                            icon = R.drawable.ic_schedule,
                            actionText = stringResource(R.string.academic_import),
                            onAction = onNavigateToCourses
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun OtherClassesSection(
    entries: List<TodayEntry>,
    onOpenCourse: (Long) -> Unit
) {
    if (entries.isEmpty()) return
    
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.academic_today),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.session.id ?: it.hashCode().toLong() }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCourse(entry.courseId) }
                ) {
                    SessionRow(
                        courseName = entry.courseName,
                        courseColorArgb = entry.courseColorArgb,
                        session = entry.session,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

private fun findCurrentAndNextSession(
    entries: List<TodayEntry>,
    now: LocalTime
): Pair<TodayEntry?, TodayEntry?> {
    var current: TodayEntry? = null
    var next: TodayEntry? = null
    
    for (entry in entries) {
        if (now >= entry.session.startTime && now < entry.session.endTime) {
            current = entry
        } else if (now < entry.session.startTime && next == null) {
            next = entry
        }
    }
    return current to next
}

private fun calculateProgress(session: ClassSession, now: LocalTime): Float {
    val startMinutes = session.startTime.hour * 60 + session.startTime.minute
    val endMinutes = session.endTime.hour * 60 + session.endTime.minute
    val nowMinutes = now.hour * 60 + now.minute
    val total = endMinutes - startMinutes
    val elapsed = nowMinutes - startMinutes
    if (total <= 0) return 0f
    return (elapsed.toFloat() / total).coerceIn(0f, 1f)
}

@Composable
@Preview(name = "HoyScreen - With Classes", showBackground = true)
fun PreviewHoyScreenWithClasses() {
    CampusTheme {
        Surface {
            HoyScreen(
                onShowWeek = {},
                onNavigateToCourses = {},
                onNavigateToTasks = {},
                onOpenCourse = {},
                onAddTask = {}
            )
        }
    }
}

@Composable
@Preview(name = "HoyScreen - Empty", showBackground = true)
fun PreviewHoyScreenEmpty() {
    CampusTheme {
        Surface {
            HoyScreen(
                onShowWeek = {},
                onNavigateToCourses = {},
                onNavigateToTasks = {},
                onOpenCourse = {},
                onAddTask = {}
            )
        }
    }
}