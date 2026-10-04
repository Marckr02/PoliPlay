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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
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
                    TextButton(onClick = { showPreview = !showPreview }) {
                        Text(stringResource(if (showPreview) R.string.markdown_preview else R.string.markdown_preview))
                    }
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
            // Barra de formato (cada chip aplica una acción pura probada en JVM)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val actions: List<Pair<String, (String, Int, Int) -> MarkdownActions.EditResult>> = listOf(
                    "H" to { t, s, e -> MarkdownActions.prefixLines(t, s, e, "#") },
                    "B" to { t, s, e -> MarkdownActions.wrap(t, s, e, "**") },
                    "I" to { t, s, e -> MarkdownActions.wrap(t, s, e, "*") },
                    "•" to { t, s, e -> MarkdownActions.prefixLines(t, s, e, "-") },
                    "1." to { t, s, e -> MarkdownActions.prefixLines(t, s, e, "1.") },
                    "☐" to { t, s, e -> MarkdownActions.prefixLines(t, s, e, "- [ ]") },
                    "</>" to { t, s, e -> MarkdownActions.wrap(t, s, e, "`") },
                    "```" to { t, s, e -> MarkdownActions.wrap(t, s, e, "```\n").let { r -> MarkdownActions.wrap(r.text, r.selectionStart, r.selectionEnd, "\n```") } }
                )
                actions.forEach { (label, action) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val current = viewModel.uiState.value
                            val result = action(current.content, current.selectionStart, current.selectionEnd)
                            viewModel.onContentChange(result.text)
                            viewModel.onSelectionChange(result.selectionStart, result.selectionEnd)
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

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


