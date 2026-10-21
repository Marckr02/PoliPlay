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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.FolderChipsRow
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.SearchPill
import com.example.campuspocket.core.designsystem.components.StaggeredNotesGrid
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity

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
    var showNewFolder by remember { mutableStateOf(false) }
    var deletingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var movingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notes_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (state.notes.size == 1) {
                        stringResource(R.string.notes_count_one)
                    } else {
                        stringResource(R.string.notes_count, state.notes.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                    onDelete = { note -> deletingNote = note },
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

            state.folders.isEmpty() -> {
                EmptyStateCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.notes_empty_heading),
                    message = stringResource(R.string.empty_notes),
                    icon = painterResource(R.drawable.ic_notes),
                    actionText = stringResource(R.string.notes_new),
                    onAction = onNewNote
                )
            }

            else -> {
                EmptyStateCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.notes_folder_empty),
                    message = stringResource(R.string.empty_notes),
                    icon = painterResource(R.drawable.ic_notes),
                    actionText = stringResource(R.string.notes_new),
                    onAction = onNewNote
                )
            }
        }
    }

    if (showNewFolder) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text(stringResource(R.string.notes_new_folder)) },
            text = {
                CampusTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = stringResource(R.string.notes_folder_name),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createFolder(folderName, null)
                    showNewFolder = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deletingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text(folder.name) },
            text = { Text(stringResource(R.string.notes_folder_delete_explain)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    deletingFolder = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingFolder = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deletingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = { Text(note.title.ifBlank { stringResource(R.string.notes_empty_title) }) },
            text = { Text(stringResource(R.string.delete_note_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(note.id)
                    deletingNote = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingNote = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    movingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { movingNote = null },
            title = { Text(note.title.ifBlank { stringResource(R.string.notes_empty_title) }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.note_move_to_folder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        viewModel.moveNoteToFolder(note.id, null)
                        movingNote = null
                    }) {
                        Text(stringResource(R.string.note_move_to_root))
                    }
                    state.folders.forEach { folder ->
                        TextButton(onClick = {
                            viewModel.moveNoteToFolder(note.id, folder.id)
                            movingNote = null
                        }) {
                            Text(folder.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { movingNote = null }) {
                    Text(stringResource(R.string.cancel))
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
            NotesScreen(onOpenNote = {}, onNewNote = {})
        }
    }
}
