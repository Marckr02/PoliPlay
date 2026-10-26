package com.example.campuspocket.feature.search.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.feature.finance.data.TransactionType

/**
 * Puente entre la búsqueda global y sus datos. La pantalla sigue siendo pura;
 * esta ruta guarda la consulta antes de navegar al resultado.
 */
@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenTransaction: (Long, TransactionType) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SearchScreen(
        state = state,
        onQueryChange = viewModel::setQuery,
        onFilterChange = viewModel::setFilter,
        onRecentSearchClick = viewModel::useRecentSearch,
        onClearRecentSearches = viewModel::clearRecentSearches,
        onCommitSearch = viewModel::commitSearch,
        onBack = onBack,
        onOpenTask = { id ->
            viewModel.commitSearch()
            onOpenTask(id)
        },
        onOpenNote = { id ->
            viewModel.commitSearch()
            onOpenNote(id)
        },
        onOpenCourse = { id ->
            viewModel.commitSearch()
            onOpenCourse(id)
        },
        onOpenTransaction = { id, type ->
            viewModel.commitSearch()
            onOpenTransaction(id, type)
        }
    )
}
