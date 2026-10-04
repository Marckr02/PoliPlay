package com.example.campuspocket.feature.academic.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.ui.SessionRow
import com.example.campuspocket.feature.academic.ui.TodayWeekSwitch
import com.example.campuspocket.feature.academic.ui.labelRes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HoyScreen(
    onShowWeek: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
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
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.entries.isEmpty() -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.academic_no_classes_today),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.entries, key = { it.session.id ?: it.hashCode().toLong() }) { entry ->
                        Card(Modifier.fillMaxWidth()) {
                            SessionRow(
                                courseName = entry.courseName,
                                courseColorArgb = entry.courseColorArgb,
                                session = entry.session,
                                modifier = Modifier.padding(12.dp),
                                onClick = { onOpenCourse(entry.courseId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
