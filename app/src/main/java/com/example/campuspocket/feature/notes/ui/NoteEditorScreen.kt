package com.example.campuspocket.feature.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.EditorModeTab
import com.example.campuspocket.core.designsystem.components.MarkdownToolbar
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.feature.notes.domain.MarkdownActions
import com.example.campuspocket.feature.notes.domain.MarkdownParser

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

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onDone,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            CampusTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = stringResource(R.string.note_title),
                placeholder = stringResource(R.string.notes_empty_title),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.togglePin() }) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(
                        if (state.pinned) R.string.note_unpin else R.string.note_pin
                    ),
                    tint = if (state.pinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            EditorModeTab(
                showPreview = showPreview,
                onToggle = { showPreview = !showPreview }
            )
        }

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
            NoteContentField(
                value = TextFieldValue(
                    text = state.content,
                    selection = TextRange(state.selectionStart, state.selectionEnd)
                ),
                onValueChange = { tv ->
                    viewModel.onContentChange(tv.text)
                    viewModel.onSelectionChange(tv.selection.start, tv.selection.end)
                },
                placeholder = stringResource(R.string.note_content),
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }
    }
}

/** Campo de contenido sin contorno, con fondo pista y soporte de selección. */
@Composable
private fun NoteContentField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(ext.track)
            .padding(16.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
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