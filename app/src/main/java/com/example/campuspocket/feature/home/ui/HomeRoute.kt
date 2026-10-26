package com.example.campuspocket.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferences

/**
 * Puente entre Inicio y sus datos reales. HomeScreen sigue siendo un
 * composable puro y testeable; la ruta es la única que conoce Hilt.
 */
@Composable
fun HomeRoute(
    preferences: UserPreferences,
    onOpenModule: (CampusModule) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onTogglePrivacy: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddTask: () -> Unit = {},
    onAddNote: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val autoWhatsNew by viewModel.autoWhatsNew.collectAsStateWithLifecycle()
    HomeScreen(
        preferences = preferences,
        onOpenModule = onOpenModule,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        state = state,
        onOpenCourse = onOpenCourse,
        onOpenNotes = onOpenNotes,
        onOpenSearch = onOpenSearch,
        onTogglePrivacy = onTogglePrivacy,
        onAddExpense = onAddExpense,
        onAddTask = onAddTask,
        onAddNote = onAddNote,
        onToggleTaskCompleted = { task, done -> viewModel.setTaskCompleted(task, done) },
        showWhatsNew = autoWhatsNew,
        onWhatsNewDismiss = { viewModel.onWhatsNewSeen() }
    )
}