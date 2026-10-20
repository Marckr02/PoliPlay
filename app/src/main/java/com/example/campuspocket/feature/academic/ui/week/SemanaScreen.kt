package com.example.campuspocket.feature.academic.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.WeeklyScheduleGrid
import com.example.campuspocket.feature.academic.ui.TodayWeekSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemanaScreen(
    onShowToday: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeekViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                    isTodaySelected = false,
                    onShowToday = onShowToday,
                    onShowWeek = { /* ya estamos aquí */ },
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateToTasks) {
                        Text(stringResource(R.string.academic_tasks))
                    }
                    TextButton(onClick = onNavigateToCourses) {
                        Text(stringResource(R.string.academic_manage_courses))
                    }
                }

                if (state.isLoading) {
                    LoadingShimmer(modifier = Modifier.weight(1f))
                } else {
                    WeeklyScheduleGrid(
                        modifier = Modifier.weight(1f),
                        days = state.days,
                        onClassClick = { entry ->
                            onOpenCourse(entry.courseId)
                        }
                    )
                }
            }
        }
    )
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
                onOpenCourse = {},
                onAddTask = {}
            )
        }
    }
}