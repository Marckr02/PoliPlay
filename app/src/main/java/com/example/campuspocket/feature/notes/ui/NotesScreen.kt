package com.example.campuspocket.feature.notes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.FolderChipsRow
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.NoteCard
import com.example.campuspocket.core.designsystem.components.StaggeredNotesGrid
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewFolder by remember { mutableStateOf(false) }
    var renamingFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var deletingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var movingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        label = { Text(stringResource(R.string.search)) },
                        placeholder = { Text(stringResource(R.string.search)) },
                        singleLine = true,
                        trailingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search)) },
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = { showNewFolder = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.notes_new_folder)) },
                    text = { Text(stringResource(R.string.notes_new_folder)) }
                )
                ExtendedFloatingActionButton(
                    onClick = onNewNote,
                    icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.note_title)) },
                    text = { Text(stringResource(R.string.note_title)) }
                )
            }
        }
    ) { inner ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                LoadingShimmer()
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.folders.isNotEmpty()) {
                        FolderChipsRow(
                            modifier = Modifier.padding(vertical = 8.dp),
                            folders = state.folders,
                            selectedFolderId = state.currentFolderId,
                            onFolderClick = { folderId ->
                                viewModel.enterFolder(folderId)
                            }
                        )
                    }
                    
                    if (state.notes.isNotEmpty()) {
                        StaggeredNotesGrid(
                            modifier = Modifier.weight(1f),
                            notes = state.notes,
                            onNoteClick = onOpenNote,
                            onPinToggle = { noteId, pinned -> viewModel.togglePin(noteId, pinned) },
                            onDelete = { note -> deletingNote = note },
                            onMove = { note -> movingNote = note }
                        )
                    } else if (state.folders.isEmpty()) {
                        EmptyStateCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.empty_notes),
                            message = stringResource(R.string.empty_notes),
                            icon = painterResource(R.drawable.ic_notes),
                            actionText = stringResource(R.string.note_title),
                            onAction = onNewNote
                        )
                    }
                }
            }
        }
    }

    // Nueva carpeta
    if (showNewFolder) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text(stringResource(R.string.notes_new_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.notes_folder_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createFolder(folderName, null)
                    showNewFolder = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Renombrar carpeta
    renamingFolder?.let { folder ->
        var folderNewName by remember(folder.id) { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { renamingFolder = null },
            title = { Text(folder.name) },
            text = {
                OutlinedTextField(
                    value = folderNewName,
                    onValueChange = { folderNewName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameFolder(folder.id, folderNewName)
                    renamingFolder = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renamingFolder = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Eliminar carpeta
    deletingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text(folder.name) },
            text = {
                Text(
                    stringResource(R.string.notes_folder_delete_explain)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    deletingFolder = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deletingFolder = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Eliminar nota
    deletingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = { Text(note.title.ifBlank { stringResource(R.string.notes_empty_title) }) },
            text = { Text(stringResource(R.string.delete_task_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(note.id)
                    deletingNote = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deletingNote = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Mover a carpeta
    movingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { movingNote = null },
            title = { Text(note.title.ifBlank { stringResource(R.string.notes_empty_title) }) },
            text = { Text(stringResource(R.string.note_move_to_folder)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.moveNoteToFolder(note.id, null)
                    movingNote = null
                }) { Text(stringResource(R.string.note_move_to_root)) }
            },
            dismissButton = { TextButton(onClick = { movingNote = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
@Preview(name = "NotesScreen", showBackground = true)
fun PreviewNotesScreen() {
    CampusTheme {
        Surface {
            NotesScreen(
                onOpenNote = {},
                onNewNote = {}
            )
        }
    }
}