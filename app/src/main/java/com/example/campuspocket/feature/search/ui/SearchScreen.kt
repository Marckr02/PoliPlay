package com.example.campuspocket.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.CampusTextButton
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.SearchPill
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.formatMoneyOrHiddenSigned
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.finance.data.TransactionSubtype
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.ui.accounts.transactionTypeLabelRes
import com.example.campuspocket.feature.notes.data.NoteEntity
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SEARCH_TASK_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))

private val SEARCH_TRANSACTION_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

/**
 * Pantalla secundaria de búsqueda global (v7 B4): campo enfocado al entrar,
 * chips por tipo, resultados agrupados con coincidencias resaltadas,
 * búsquedas recientes y anuncio del total para lectores de pantalla.
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onCommitSearch: () -> Unit,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenTransaction: (Long, TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(R.string.search),
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchPill(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search_placeholder),
                focusRequester = focusRequester,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onCommitSearch()
                        keyboard?.hide()
                    }
                )
            )

            if (state.availableFilters.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(state.availableFilters, key = { it.name }) { filter ->
                        CampusChip(
                            label = filter.label(),
                            selected = state.filter == filter,
                            onClick = { onFilterChange(filter) },
                            role = Role.Tab
                        )
                    }
                }
            }

            if (!state.hasQuery) {
                IfBlankQuery(
                    recentSearches = state.recentSearches,
                    onRecentSearchClick = onRecentSearchClick,
                    onClearRecentSearches = onClearRecentSearches
                )
            } else {
                val query = state.query.trim()
                if (state.isLoading) {
                    LoadingShimmer(modifier = Modifier.fillMaxWidth().weight(1f))
                } else {
                    Text(
                        text = pluralStringResource(
                            R.plurals.search_result_count,
                            state.totalResults,
                            state.totalResults
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        }
                    )

                    if (state.totalResults == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            EmptyStateCard(
                                title = stringResource(R.string.search_no_results_title),
                                message = stringResource(R.string.search_no_results_message, query),
                                icon = Icons.Filled.Search,
                                actionText = stringResource(R.string.search_clear_query),
                                onAction = { onQueryChange("") }
                            )
                        }
                    } else {
                        SearchResults(
                            state = state,
                            query = query,
                            onOpenTask = onOpenTask,
                            onOpenNote = onOpenNote,
                            onOpenCourse = onOpenCourse,
                            onOpenTransaction = onOpenTransaction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.IfBlankQuery(
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit
) {
    if (recentSearches.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            EmptyStateCard(
                title = stringResource(R.string.search_empty_title),
                message = stringResource(R.string.search_empty_message),
                icon = Icons.Filled.Search
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_recent_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            CampusTextButton(
                text = stringResource(R.string.search_clear_recent),
                onClick = onClearRecentSearches
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 4.dp)
        ) {
            items(recentSearches) { recent ->
                CampusChip(
                    label = recent,
                    selected = false,
                    onClick = { onRecentSearchClick(recent) },
                    role = Role.Button
                )
            }
        }
        Text(
            text = stringResource(R.string.search_recent_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColumnScope.SearchResults(
    state: SearchUiState,
    query: String,
    onOpenTask: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenTransaction: (Long, TransactionType) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (state.tasks.isNotEmpty()) {
            item(key = "search-header-tasks") {
                SearchSectionHeader(R.string.search_section_tasks, state.tasks.size)
            }
            items(state.tasks, key = { "search-task-${it.task.id}" }) { result ->
                TaskResultCard(
                    result = result,
                    query = query,
                    onClick = { result.task.id?.let(onOpenTask) }
                )
            }
        }

        if (state.notes.isNotEmpty()) {
            item(key = "search-header-notes") {
                SearchSectionHeader(R.string.search_section_notes, state.notes.size)
            }
            items(state.notes, key = { "search-note-${it.id}" }) { note ->
                NoteResultCard(
                    note = note,
                    query = query,
                    onClick = { onOpenNote(note.id) }
                )
            }
        }

        if (state.transactions.isNotEmpty()) {
            item(key = "search-header-transactions") {
                SearchSectionHeader(
                    R.string.search_section_transactions,
                    state.transactions.size
                )
            }
            items(
                state.transactions,
                key = {
                    "search-transaction-${it.transaction.id ?: it.transaction.createdAt}"
                }
            ) { result ->
                TransactionResultCard(
                    result = result,
                    query = query,
                    onClick = {
                        result.transaction.id?.let { id ->
                            onOpenTransaction(id, result.transaction.type)
                        }
                    }
                )
            }
        }

        if (state.courses.isNotEmpty()) {
            item(key = "search-header-courses") {
                SearchSectionHeader(R.string.search_section_courses, state.courses.size)
            }
            items(state.courses, key = { "search-course-${it.id ?: it.code}" }) { course ->
                CourseResultCard(
                    course = course,
                    query = query,
                    onClick = { course.id?.let(onOpenCourse) }
                )
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(titleRes: Int, count: Int) {
    Text(
        text = stringResource(titleRes, count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() }
    )
}

@Composable
private fun TaskResultCard(
    result: TaskSearchResult,
    query: String,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    val task = result.task
    val due = remember(task.dueAt) {
        task.dueAt
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(SEARCH_TASK_DATE)
    }
    val completed = if (task.completedAt != null) {
        stringResource(R.string.search_task_completed)
    } else {
        null
    }
    val meta = listOfNotNull(
        result.courseName?.takeIf { it.isNotBlank() },
        due,
        completed
    ).joinToString(" · ")

    SearchResultCard(
        iconRes = R.drawable.ic_action_task_list,
        containerColor = ext.accent,
        contentColor = ext.onAccent,
        title = highlighted(task.title, query),
        meta = highlighted(meta, query),
        onClick = onClick
    )
}

@Composable
private fun NoteResultCard(
    note: NoteEntity,
    query: String,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    val title = note.title.ifBlank { stringResource(R.string.notes_empty_title) }
    val preview = remember(note.id, note.content, note.title, query) {
        notePreview(note, query)
    }

    SearchResultCard(
        iconRes = R.drawable.ic_notes,
        containerColor = ext.lavender,
        contentColor = ext.onLavender,
        title = highlighted(title, query),
        meta = if (preview.isBlank()) null else highlighted(preview, query),
        onClick = onClick
    )
}

@Composable
private fun TransactionResultCard(
    result: TransactionSearchResult,
    query: String,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    val transaction = result.transaction
    val title = transaction.description?.takeIf { it.isNotBlank() }
        ?: stringResource(transactionTypeLabelRes(transaction.type))
    val accounts = if (
        transaction.subtype == TransactionSubtype.RETIRO_CAJERO &&
        !result.targetAccountName.isNullOrBlank()
    ) {
        "${result.accountName.orEmpty()} → ${result.targetAccountName}"
    } else {
        result.accountName
    }
    val meta = listOfNotNull(
        transaction.date.format(SEARCH_TRANSACTION_DATE),
        accounts?.takeIf { it.isNotBlank() },
        result.categoryName?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    val signedCents = when (transaction.type) {
        TransactionType.EXPENSE,
        TransactionType.TRANSFER -> -transaction.amountCents
        TransactionType.INCOME,
        TransactionType.REFUND -> transaction.amountCents
    }
    val (iconRes, container, content) = when (transaction.type) {
        TransactionType.EXPENSE -> Triple(R.drawable.ic_move_expense, ext.peach, ext.onPeach)
        TransactionType.INCOME -> Triple(R.drawable.ic_move_income, ext.mint, ext.onMint)
        TransactionType.TRANSFER -> Triple(
            if (transaction.subtype == TransactionSubtype.RETIRO_CAJERO) {
                R.drawable.ic_move_withdraw
            } else {
                R.drawable.ic_move_transfer
            },
            ext.sky,
            ext.onSky
        )
        TransactionType.REFUND -> Triple(R.drawable.ic_move_income, ext.mint, ext.onMint)
    }

    SearchResultCard(
        iconRes = iconRes,
        containerColor = container,
        contentColor = content,
        title = highlighted(title, query),
        meta = highlighted(meta, query),
        trailing = formatMoneyOrHiddenSigned(signedCents),
        trailingColor = when (transaction.type) {
            TransactionType.INCOME,
            TransactionType.REFUND -> ext.success
            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
            TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
        },
        onClick = onClick
    )
}

@Composable
private fun CourseResultCard(
    course: Course,
    query: String,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    val meta = listOfNotNull(
        course.code.takeIf { it.isNotBlank() },
        course.section.takeIf { it.isNotBlank() },
        course.teacher?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    SearchResultCard(
        iconRes = R.drawable.ic_dock_school_outline,
        containerColor = ext.sky,
        contentColor = ext.onSky,
        title = highlighted(course.name, query),
        meta = highlighted(meta, query),
        onClick = onClick
    )
}

@Composable
private fun SearchResultCard(
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    title: AnnotatedString,
    meta: AnnotatedString?,
    onClick: () -> Unit,
    trailing: String? = null,
    trailingColor: Color = Color.Unspecified
) {
    BentoCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null, // decorativo: el texto contiguo describe el resultado
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta != null && meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelLarge,
                    color = trailingColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun highlighted(text: String, query: String): AnnotatedString {
    val background = MaterialTheme.extended.accentMuted
    val foreground = MaterialTheme.extended.onAccentMuted
    return remember(text, query, background, foreground) {
        if (text.isEmpty() || query.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                var start = 0
                while (start < text.length) {
                    val match = text.indexOf(query, startIndex = start, ignoreCase = true)
                    if (match < 0) {
                        append(text.substring(start))
                        break
                    }
                    append(text.substring(start, match))
                    withStyle(
                        SpanStyle(
                            background = background,
                            color = foreground,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(text.substring(match, match + query.length))
                    }
                    start = match + query.length
                }
            }
        }
    }
}

private fun notePreview(note: NoteEntity, query: String): String {
    val content = note.content
        .replace('\n', ' ')
        .replace(WHITESPACE, " ")
        .trim()
    if (content.isEmpty()) return ""

    val titleMatches = note.title.contains(query, ignoreCase = true)
    val matchIndex = content.indexOf(query, ignoreCase = true)
    if (titleMatches || matchIndex < 0) {
        return content.take(NOTE_PREVIEW_LENGTH).appendEllipsis(content.length)
    }

    val start = (matchIndex - NOTE_PREVIEW_CONTEXT).coerceAtLeast(0)
    val end = (start + NOTE_PREVIEW_LENGTH).coerceAtMost(content.length)
    val snippet = content.substring(start, end)
    return buildString {
        if (start > 0) append('…')
        append(snippet)
        if (end < content.length) append('…')
    }
}

private fun String.appendEllipsis(totalLength: Int): String =
    if (totalLength > length) "$this…" else this

@Composable
private fun SearchFilter.label(): String = stringResource(
    when (this) {
        SearchFilter.ALL -> R.string.search_filter_all
        SearchFilter.TASKS -> R.string.search_filter_tasks
        SearchFilter.NOTES -> R.string.search_filter_notes
        SearchFilter.TRANSACTIONS -> R.string.search_filter_transactions
        SearchFilter.COURSES -> R.string.search_filter_courses
    }
)

private const val NOTE_PREVIEW_LENGTH = 120
private const val NOTE_PREVIEW_CONTEXT = 36
private val WHITESPACE = Regex("\\s+")
