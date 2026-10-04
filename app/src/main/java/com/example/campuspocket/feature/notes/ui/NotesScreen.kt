package com.example.campuspocket.feature.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
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
    var deleteFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var deletingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var movingNote by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = { Text(stringResource(R.string.search)) },
                        singleLine = true,
                        trailingIcon = { Icon(Icons.Filled.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = { showNewFolder = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.notes_new_folder)) }
                )
                ExtendedFloatingActionButton(
                    onClick = onNewNote,
                    icon = { Icon(Icons.Filled.Edit, null) },
                    text = { Text(stringResource(R.string.note_title)) }
                )
            }
        }
    ) { inner ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.folders.isNotEmpty()) {
                    item(key = "hdr_folders") {
                        Text(
                            text = stringResource(R.string.notes_folders),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.folders, key = { "f" + it.id }) { folder ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.enterFolder(folder.id) }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        folder.name.first().uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(folder.name, style = MaterialTheme.typography.titleMedium)
                                }
                                // renombrar
                                IconButton(onClick = { renamingFolder = folder }) {
                                    Icon(Icons.Filled.Edit, null)
                                }
                                // eliminar
                                IconButton(onClick = { deleteFolder = folder }) {
                                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                if (state.notes.isNotEmpty()) {
                    item(key = "hdr_notes") {
                        Text(
                            text = stringResource(R.string.notes_list),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.notes, key = { "n" + it.id }) { note ->
                        Card(Modifier.fillMaxWidth().clickable { onOpenNote(note.id) }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = note.title.ifBlank { stringResource(R.string.notes_empty_title) },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = note.content.take(80).replace('\n', ' '),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { viewModel.togglePin(note.id, !note.pinned) }) {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        tint = if (note.pinned) Color(0xFFF5A623) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { deletingNote = note }) {
                                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { movingNote = note }) {
                                    Icon(Icons.Filled.MoreVert, null)
                                }
                            }
                        }
                    }
                }
                if (state.folders.isEmpty() && state.notes.isEmpty()) {
                    item(key = "vacio") {
                        Text(
                            text = stringResource(R.string.empty_notes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }

    // Nueva carpeta
    if (showNewFolder) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text(stringResource(R.string.notes_new_folder)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.notes_folder_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createFolder(name, null)
                    showNewFolder = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Renombrar carpeta
    renamingFolder?.let { folder ->
        var name by remember(folder.id) { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { renamingFolder = null },
            title = { Text(folder.name) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameFolder(folder.id, name)
                    renamingFolder = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renamingFolder = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Eliminar carpeta (explica qué pasa con subcarpetas y notas)
    deleteFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deleteFolder = null },
            title = { Text(folder.name) },
            text = {
                Text(
                    stringResource(R.string.notes_folder_delete_explain)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    deleteFolder = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteFolder = null }) { Text(stringResource(R.string.cancel)) } }
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

    // Mover a carpeta (simple: quitar o a raíz; para mover a una carpeta existente usar el menú)
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
