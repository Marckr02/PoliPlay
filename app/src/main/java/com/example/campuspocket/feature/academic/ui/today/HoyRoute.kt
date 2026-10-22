package com.example.campuspocket.feature.academic.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier

@Composable
fun HoyRoute(
    onShowWeek: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HoyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HoyScreen(
        onShowWeek = onShowWeek,
        onNavigateToCourses = onNavigateToCourses,
        onNavigateToTasks = onNavigateToTasks,
        onOpenCourse = onOpenCourse,
        modifier = modifier,
        state = state,
        onSelectDate = viewModel::selectDate,
        onToggleTask = viewModel::setCompleted,
        onOpenTask = onOpenTask,
        onDeleteTask = viewModel::delete
    )
}
