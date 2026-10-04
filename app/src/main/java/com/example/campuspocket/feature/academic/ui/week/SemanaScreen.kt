package com.example.campuspocket.feature.academic.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.ui.SessionRow
import com.example.campuspocket.feature.academic.ui.TodayWeekSwitch
import com.example.campuspocket.feature.academic.ui.labelRes

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
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.days, key = { it.day.value }) { daySchedule ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(daySchedule.day.labelRes()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (daySchedule.entries.isEmpty()) {
                            Text(
                                text = stringResource(R.string.day_no_sessions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            daySchedule.entries.forEach { entry ->
                                SessionRow(
                                    courseName = entry.courseName,
                                    courseColorArgb = entry.courseColorArgb,
                                    session = entry.session,
                                    modifier = Modifier.padding(top = 8.dp),
                                    onClick = { onOpenCourse(entry.courseId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
