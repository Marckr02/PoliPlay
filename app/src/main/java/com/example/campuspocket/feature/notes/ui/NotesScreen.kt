package com.example.campuspocket.feature.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusButtonStyle
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.EmptyNotesState
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.FolderChipsRow
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.components.SearchPill
import com.example.campuspocket.core.designsystem.components.StaggeredNotesGrid
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.navigation.LocalSnackbarHost
import com.example.campuspocket.core.navigation.showUndoSnackbar
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import kotlinx.coroutines.launch

/**
 * Notas: buscador en píldora, chips de carpetas y cuadrícula escalonada de
 * tarjetas pastel. Las acciones de cada nota viven en la pulsación larga.
 */
@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val deletedMessage = stringResource(R.string.note_deleted_snackbar)
    val undoLabel = stringResource(R.string.snackbar_undo)
    var showNewFolder by remember { mutableStateOf(false) }
    var movingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderEntity?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(R.string.notes_title),
            onBack = null,
            actions = {
                RoundIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.notes_new),
                    onClick = onNewNote,
                    containerColor = MaterialTheme.extended.accent,
                    contentColor = MaterialTheme.extended.onAccent
                )
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (state.notes.size == 1) {
                    stringResource(R.string.notes_count_one)
                } else {
                    stringResource(R.string.notes_count, state.notes.size)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SearchPill(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(R.string.notes_search_placeholder)
            )

            FolderChipsRow(
                folders = state.folders,
                selectedFolderId = state.currentFolderId,
                onFolderClick = { folderId -> viewModel.enterFolder(folderId) },
                onAddFolder = { showNewFolder = true }
            )

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(top = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                LoadingShimmer()
            }

            state.notes.isNotEmpty() -> {
                StaggeredNotesGrid(
                    modifier = Modifier.weight(1f),
                    notes = state.notes,
                    onNoteClick = onOpenNote,
                    onPinToggle = { noteId, pinned -> viewModel.togglePin(noteId, pinned) },
                    onDelete = { note ->
                        scope.launch {
                            val deleted = viewModel.deleteNoteForUndo(note.id) ?: return@launch
                            val undone = snackbarHost.showUndoSnackbar(
                                message = deletedMessage,
                                actionLabel = undoLabel,
                                timeoutMillis = 5_000L
                            )
                            if (undone) viewModel.restoreNote(deleted)
                        }
                    },
                    onMove = { note -> movingNote = note }
                )
            }

            state.query.isNotBlank() -> {
                EmptyStateCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.search),
                    message = stringResource(R.string.notes_no_results),
                    icon = painterResource(R.drawable.ic_notes)
                )
            }

            else -> {
                EmptyNotesState(
                    modifier = Modifier.fillMaxWidth(),
                    onNewNote = onNewNote
                )
            }
        }
    }
    }

    if (showNewFolder) {
        var folderName by remember { mutableStateOf("") }
        CampusDialog(
            onDismissRequest = { showNewFolder = false },
            title = stringResource(R.string.notes_new_folder),
            confirmText = stringResource(R.string.save),
            confirmEnabled = folderName.isNotBlank(),
            onConfirm = {
                viewModel.createFolder(folderName, null)
                showNewFolder = false
            },
            content = {
                CampusTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = stringResource(R.string.notes_folder_name),
                    singleLine = true
                )
            }
        )
    }

    deletingFolder?.let { folder ->
        CampusDialog(
            onDismissRequest = { deletingFolder = null },
            title = folder.name,
            message = stringResource(R.string.notes_folder_delete_explain),
            icon = Icons.Filled.Delete,
            destructive = true,
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteFolder(folder.id)
                deletingFolder = null
            }
        )
    }

    movingNote?.let { note ->
        CampusDialog(
            onDismissRequest = { movingNote = null },
            title = note.title.ifBlank { stringResource(R.string.notes_empty_title) },
            message = stringResource(R.string.note_move_to_folder),
            confirmText = stringResource(R.string.cancel),
            onConfirm = { movingNote = null },
            showDismiss = false,
            content = {
                CampusButton(
                    text = stringResource(R.string.note_move_to_root),
                    onClick = {
                        viewModel.moveNoteToFolder(note.id, null)
                        movingNote = null
                    },
                    style = CampusButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
                state.folders.forEach { folder ->
                    CampusButton(
                        text = folder.name,
                        onClick = {
                            viewModel.moveNoteToFolder(note.id, folder.id)
                            movingNote = null
                        },
                        style = CampusButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
@Preview(name = "NotesScreen", showBackground = true)
fun PreviewNotesScreen() {
    CampusTheme {
        Surface {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalSnackbarHost provides remember { androidx.compose.material3.SnackbarHostState() }
            ) {
                NotesScreen(onOpenNote = {}, onNewNote = {})
            }
        }
    }
}
