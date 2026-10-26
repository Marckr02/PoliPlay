package com.example.campuspocket.feature.notes.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.EditorModeTab
import com.example.campuspocket.core.designsystem.components.MarkdownToolbar
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.feature.notes.domain.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var showPreview by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showCoursePicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedCourse = courses.firstOrNull { it.id == state.courseId }
    val selectedFolder = folders.firstOrNull { it.id == state.folderId }

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
            verticalAlignment = Alignment.CenterVertically
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
            RoundIconButton(
                iconRes = R.drawable.ic_share,
                contentDescription = stringResource(R.string.note_share),
                onClick = { showShare = true },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            EditorModeTab(
                showPreview = showPreview,
                onToggle = { showPreview = !showPreview }
            )
            RoundIconButton(
                iconRes = R.drawable.ic_undo,
                contentDescription = stringResource(R.string.note_undo),
                onClick = viewModel::undo,
                enabled = state.canUndo
            )
            RoundIconButton(
                iconRes = R.drawable.ic_redo,
                contentDescription = stringResource(R.string.note_redo),
                onClick = viewModel::redo,
                enabled = state.canRedo
            )
            Spacer(Modifier.weight(1f))
            SaveStatus(isSaving = state.isSaving, lastSavedAt = state.lastSavedAt)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NoteLinkChip(
                label = selectedCourse?.name ?: stringResource(R.string.note_link_course),
                iconRes = R.drawable.ic_link,
                hasValue = selectedCourse != null,
                onClick = { showCoursePicker = true }
            )
            NoteLinkChip(
                label = selectedFolder?.name ?: stringResource(R.string.notes_add_folder),
                iconRes = R.drawable.ic_folder,
                hasValue = selectedFolder != null,
                onClick = { showFolderPicker = true }
            )
        }

        // Barra de formato
        MarkdownToolbar(
            modifier = Modifier.fillMaxWidth(),
            currentContent = state.content,
            selectionStart = state.selectionStart,
            selectionEnd = state.selectionEnd,
            onContentChange = viewModel::onContentChange,
            onSelectionChange = viewModel::onSelectionChange,
            onActionApplied = viewModel::commitHistoryNow
        )

        if (showPreview) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                MarkdownPreview(
                    markdown = state.content,
                    onToggleCheckbox = { rawStart ->
                        viewModel.onContentChange(
                            MarkdownParser.toggleCheckbox(state.content, rawStart)
                        )
                        viewModel.commitHistoryNow()
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

    if (showShare) {
        ModalBottomSheet(
            onDismissRequest = { showShare = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.note_share),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                ShareMenuRow(
                    text = stringResource(R.string.note_share_text),
                    onClick = {
                        showShare = false
                        shareAsText(context, state.title, state.content)
                    }
                )
                ShareMenuRow(
                    text = stringResource(R.string.note_share_markdown),
                    onClick = {
                        showShare = false
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                writeMarkdownCacheFile(context, state.title, state.content)
                            }
                            if (uri == null) {
                                Toast.makeText(
                                    context,
                                    R.string.note_share_error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/markdown"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, state.title)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        context.getString(R.string.note_share)
                                    )
                                )
                            }
                        }
                    }
                )
                ShareMenuRow(
                    text = stringResource(R.string.note_copy),
                    onClick = {
                        showShare = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                state.title.ifBlank { context.getString(R.string.notes_empty_title) },
                                state.content
                            )
                        )
                        Toast.makeText(context, R.string.note_copied, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    if (showCoursePicker) {
        NoteLinkPicker(
            title = stringResource(R.string.note_course_picker),
            noneLabel = stringResource(R.string.note_no_course),
            selectedId = state.courseId,
            items = courses.mapNotNull { course ->
                course.id?.let { it to "${course.name} · ${course.code}" }
            },
            onDismiss = { showCoursePicker = false },
            onSelect = { id ->
                viewModel.setCourseId(id)
                showCoursePicker = false
            }
        )
    }

    if (showFolderPicker) {
        NoteLinkPicker(
            title = stringResource(R.string.note_folder_picker),
            noneLabel = stringResource(R.string.note_no_folder),
            selectedId = state.folderId,
            items = folders.mapNotNull { folder ->
                folder.id?.let { it to folder.name }
            },
            onDismiss = { showFolderPicker = false },
            onSelect = { id ->
                viewModel.setFolderId(id)
                showFolderPicker = false
            }
        )
    }
}

@Composable
private fun SaveStatus(isSaving: Boolean, lastSavedAt: Long) {
    if (!isSaving && lastSavedAt == 0L) return
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isSaving) {
            Text(
                text = stringResource(R.string.note_saving_status),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                tint = ext.onMint,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = stringResource(R.string.note_saved_status),
                style = MaterialTheme.typography.labelSmall,
                color = ext.onMint
            )
        }
    }
}

@Composable
private fun NoteLinkChip(
    label: String,
    iconRes: Int,
    hasValue: Boolean,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (hasValue) ext.cream else ext.track)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null, // decorativo: la etiqueta contigua describe la accion
            tint = if (hasValue) ext.onCream else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (hasValue) ext.onCream else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ShareMenuRow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NoteLinkPicker(
    title: String,
    noneLabel: String,
    selectedId: Long?,
    items: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    CampusDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(R.string.cancel),
        onConfirm = onDismiss,
        showDismiss = false,
        content = {
            Column(
                Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CampusSelectItem(
                    text = noneLabel,
                    selected = selectedId == null,
                    onClick = { onSelect(null) }
                )
                items.forEach { (id, label) ->
                    CampusSelectItem(
                        text = label,
                        selected = selectedId == id,
                        onClick = { onSelect(id) }
                    )
                }
            }
        }
    )
}

private fun shareAsText(context: Context, title: String, content: String) {
    val body = buildString {
        if (title.isNotBlank()) append(title).append("\n\n")
        append(content)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.note_share))
    )
}

private fun writeMarkdownCacheFile(context: Context, title: String, content: String) =
    runCatching {
        val dir = File(context.cacheDir, "notes").apply { mkdirs() }
        val file = File(dir, "${safeFileName(title)}.md")
        val markdown = buildString {
            if (title.isNotBlank()) append("# ").append(title).append("\n\n")
            append(content)
        }
        file.writeText(markdown)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()

private fun safeFileName(title: String): String =
    title.trim()
        .replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
        .trim('_')
        .ifBlank { "nota" }
        .take(60)

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
