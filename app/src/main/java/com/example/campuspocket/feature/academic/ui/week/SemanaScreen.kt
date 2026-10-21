package com.example.campuspocket.feature.academic.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.components.WeeklyScheduleGrid
import androidx.compose.material3.Surface

/**
 * Semana: rejilla horaria sin FAB. Las acciones viven en la cabecera y el
 * dock contextual añade la tarea desde la pestaña Académico.
 */
@Composable
fun SemanaScreen(
    onShowToday: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeekViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.academic_week),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.week_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.academic_today),
                    onClick = onShowToday,
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

        if (state.isLoading) {
            LoadingShimmer(modifier = Modifier.weight(1f))
        } else {
            WeeklyScheduleGrid(
                modifier = Modifier.weight(1f),
                days = state.days,
                onClassClick = { entry -> onOpenCourse(entry.courseId) }
            )
        }
    }
}

@Composable
@Preview(name = "SemanaScreen", showBackground = true)
fun PreviewSemanaScreen() {
    CampusTheme {
        Surface {
            SemanaScreen(
                onShowToday = {},
                onNavigateToCourses = {},
                onNavigateToTasks = {},
                onOpenCourse = {}
            )
        }
    }
}
