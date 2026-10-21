package com.example.campuspocket.feature.academic.ui.coursedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.DangerPillButton
import com.example.campuspocket.core.designsystem.components.PriorityBadge
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.ui.TimeText
import com.example.campuspocket.feature.academic.ui.labelRes

@Composable
fun CourseDetailScreen(
    onEditCourse: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    if (deleted) return

    CourseDetailContent(
        state = state,
        onBack = onBack,
        onEditCourse = onEditCourse,
        onDeleteRequest = { showDeleteDialog = true },
        modifier = modifier
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text((state as? CourseDetailUiState.Content)?.course?.name ?: "")
            },
            text = { Text(stringResource(R.string.delete_course_confirm)) }
        )
    }
}

@Composable
private fun CourseDetailContent(
    state: CourseDetailUiState,
    onBack: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val course = (state as? CourseDetailUiState.Content)?.course

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = course?.name ?: stringResource(R.string.academic_courses),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            course?.id?.let { id ->
                RoundIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.edit),
                    onClick = { onEditCourse(id) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        when (state) {
            CourseDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            CourseDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.course_detail_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is CourseDetailUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CourseHeroHeader(course = state.course)

                    Text(
                        text = stringResource(R.string.course_sessions_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.course.sessions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.session_list_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.course.sessions.forEach { session ->
                            SessionCard(
                                session = session,
                                colorArgb = state.course.colorArgb
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.course_tasks_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.tasks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.empty_tasks_here),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.tasks.forEach { task ->
                            BentoCard(contentPadding = PaddingValues(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(
                                                if (task.completedAt == null) R.string.task_pending else R.string.task_completed
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    PriorityBadge(priority = task.priority)
                                }
                            }
                        }
                    }

                    if (state.notes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.notes_list),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        state.notes.forEach { note ->
                            BentoCard(contentPadding = PaddingValues(14.dp)) {
                                Text(
                                    text = note.title.ifBlank { stringResource(R.string.notes_empty_title) },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = note.content.take(80).replace('\n', ' '),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    DangerPillButton(
                        text = stringResource(R.string.delete),
                        onClick = onDeleteRequest,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseHeroHeader(course: Course, modifier: Modifier = Modifier) {
    val courseColor = Color(course.colorArgb)
    BentoCard(
        modifier = modifier,
        containerColor = courseColor.copy(alpha = 0.16f),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(courseColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = course.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = course.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.course_section_short,
                                    course.section
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            course.teacher?.takeIf { it.isNotBlank() }?.let { teacher ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = teacher,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: ClassSession,
    colorArgb: Int,
    modifier: Modifier = Modifier
) {
    BentoCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(Color(colorArgb))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(session.dayOfWeek.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.session_time_range,
                        TimeText.format(session.startTime),
                        TimeText.format(session.endTime)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            session.room?.takeIf { it.isNotBlank() }?.let { room ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = room,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}