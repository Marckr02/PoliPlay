package com.example.campuspocket.feature.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.EditorModeTab
import com.example.campuspocket.core.designsystem.components.MarkdownToolbar
import com.example.campuspocket.feature.notes.domain.MarkdownActions
import com.example.campuspocket.feature.notes.domain.MarkdownParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPreview by remember { mutableStateOf(false) }

    // Guardado al salir.
    DisposableEffect(Unit) {
        onDispose {
            kotlinx.coroutines.runBlocking { viewModel.flush() }
        }
    }

    LaunchedEffect(state.deleted) { if (state.deleted) onDone() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        placeholder = { Text(stringResource(R.string.notes_empty_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    EditorModeTab(
                        showPreview = showPreview,
                        onToggle = { showPreview = !showPreview }
                    )
                    TextButton(onClick = { viewModel.togglePin() }) {
                        Text(stringResource(if (state.pinned) R.string.note_unpin else R.string.note_pin))
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner)
        ) {
            // Barra de formato
            MarkdownToolbar(
                modifier = Modifier.fillMaxWidth(),
                onAction = { _, _, _ -> MarkdownActions.EditResult("", 0, 0) }, // Not used directly
                currentContent = state.content,
                selectionStart = state.selectionStart,
                selectionEnd = state.selectionEnd,
                onContentChange = viewModel::onContentChange,
                onSelectionChange = viewModel::onSelectionChange
            )

            if (showPreview) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    MarkdownPreview(
                        markdown = state.content,
                        onToggleCheckbox = { rawStart ->
                            viewModel.onContentChange(
                                MarkdownParser.toggleCheckbox(state.content, rawStart)
                            )
                        }
                    )
                }
            } else {
                OutlinedTextField(
                    value = TextFieldValue(text = state.content, selection = TextRange(state.selectionStart, state.selectionEnd)),
                    onValueChange = { tv ->
                        viewModel.onContentChange(tv.text)
                        viewModel.onSelectionChange(tv.selection.start, tv.selection.end)
                    },
                    placeholder = { Text(stringResource(R.string.note_content)) },
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
    }
}

@Composable
@Preview(name = "NoteEditorScreen", showBackground = true)
fun PreviewNoteEditorScreen() {
    com.example.campuspocket.core.designsystem.CampusTheme {
        androidx.compose.material3.Surface {
            NoteEditorScreen(onDone = {})
        }
    }
}