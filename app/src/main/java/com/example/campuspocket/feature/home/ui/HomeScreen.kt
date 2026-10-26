package com.example.campuspocket.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuspocket.BuildConfig
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusNumeral
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSectionHeader
import com.example.campuspocket.core.designsystem.MoneyText
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.PriorityBadge
import com.example.campuspocket.core.designsystem.components.PrivacyToggleButton
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.components.SearchPill
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferences
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.finance.domain.BudgetPace
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Inicio bento: saludo, búsqueda global, barra de acciones rápidas, próxima clase,
 * presupuesto diario, tareas urgentes y notas recientes.
 */
@Composable
fun HomeScreen(
    preferences: UserPreferences,
    onOpenModule: (CampusModule) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    state: HomeUiState = HomeUiState(),
    onOpenCourse: (Long) -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onTogglePrivacy: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddTask: () -> Unit = {},
    onAddNote: () -> Unit = {},
    onToggleTaskCompleted: (Task, Boolean) -> Unit = { _, _ -> },
    onSearchQueryChange: (String) -> Unit = {},
    showWhatsNew: Boolean = false,
    onWhatsNewDismiss: () -> Unit = {}
) {
    val activeModules = CampusModule.entries.filter { preferences.isModuleEnabled(it) }
    val displayName = preferences.displayName.ifBlank { null }
    val ext = MaterialTheme.extended
    val dateText = remember(state.today) {
        state.today.format(
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es"))
        ).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es")) }
    }

    var showSearchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = dateText.uppercase(Locale.forLanguageTag("es")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayName?.let { stringResource(R.string.home_greeting_name, it) }
                        ?: stringResource(R.string.home_greeting_generic),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            RoundIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search),
                onClick = { showSearchDialog = true },
                size = 40.dp,
                containerColor = ext.track,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            PrivacyToggleButton(onToggle = onTogglePrivacy)
            Spacer(Modifier.width(4.dp))
            HomeAvatar(
                displayName = displayName,
                onClick = onOpenSettings
            )
        }

        if (activeModules.isEmpty()) {
            EmptyStateCard(
                title = stringResource(R.string.home_no_modules_title),
                message = stringResource(R.string.home_no_modules_message),
                actionText = stringResource(R.string.home_choose_modules),
                onAction = onOpenSettings
            )
            return@Column
        }

        // Fila de Acciones Rápidas (+ Gasto, + Tarea, + Nota)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (preferences.isModuleEnabled(CampusModule.FINANCE)) {
                QuickActionButton(
                    label = stringResource(R.string.home_quick_add_format, stringResource(R.string.home_quick_expense)),
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f)
                )
            }
            if (preferences.isModuleEnabled(CampusModule.ACADEMIC)) {
                QuickActionButton(
                    label = stringResource(R.string.home_quick_add_format, stringResource(R.string.home_quick_task)),
                    onClick = onAddTask,
                    modifier = Modifier.weight(1f)
                )
            }
            if (preferences.isModuleEnabled(CampusModule.NOTES)) {
                QuickActionButton(
                    label = stringResource(R.string.home_quick_add_format, stringResource(R.string.home_quick_note)),
                    onClick = onAddNote,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (preferences.isModuleEnabled(CampusModule.ACADEMIC)) {
            NextClassCard(
                nextClass = state.nextClass,
                laterClass = state.laterClass,
                onClick = {
                    val id = state.nextClass?.courseId
                    if (id != null && id > 0L) onOpenCourse(id) else onOpenModule(CampusModule.ACADEMIC)
                }
            )
        }

        val showFinance = preferences.isModuleEnabled(CampusModule.FINANCE)
        val showAcademic = preferences.isModuleEnabled(CampusModule.ACADEMIC)
        if (showFinance || showAcademic) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 176.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showFinance) {
                    FinanceSummaryCard(
                        today = state.today,
                        monthlyBudgetCents = state.monthlyBudgetCents,
                        monthlySpentCents = state.monthlySpentCents,
                        monthlyRemainingCents = state.monthlyRemainingCents,
                        onClick = { onOpenModule(CampusModule.FINANCE) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                if (showAcademic) {
                    TasksSummaryCard(
                        pendingTasks = state.pendingTasks,
                        tasksDueToday = state.tasksDueToday,
                        onClick = { onOpenModule(CampusModule.ACADEMIC) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        // Tareas Urgentes
        if (showAcademic && state.urgentTasks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampusSectionHeader(
                    title = stringResource(R.string.home_pending_tasks_title),
                    actionText = stringResource(R.string.home_see_all),
                    onAction = { onOpenModule(CampusModule.ACADEMIC) }
                )
                state.urgentTasks.forEach { task ->
                    BentoCard(
                        onClick = { onOpenModule(CampusModule.ACADEMIC) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = task.completedAt != null,
                                onCheckedChange = { done -> onToggleTaskCompleted(task, done) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            PriorityBadge(priority = task.priority)
                        }
                    }
                }
            }
        }

        if (preferences.isModuleEnabled(CampusModule.NOTES)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampusSectionHeader(
                    title = stringResource(R.string.home_recent_notes),
                    actionText = stringResource(R.string.home_see_all),
                    onAction = onOpenNotes
                )
                BentoCard(
                    onClick = onOpenNotes,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    if (state.recentNotes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_recent_notes_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.recentNotes.forEachIndexed { index, note ->
                            if (index > 0) {
                                Spacer(Modifier.height(12.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (index) {
                                                0 -> ext.lavender
                                                else -> ext.limeSoft
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (note.title.ifBlank { "N" }).take(1).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (index == 0) ext.onLavender else ext.onLimeSoft
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = note.title.ifBlank { stringResource(R.string.notes_empty_title) },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = note.content.take(52).replace('\n', ' '),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Búsqueda Global (Parte 4 #3)
    if (showSearchDialog) {
        CampusDialog(
            onDismissRequest = {
                showSearchDialog = false
                onSearchQueryChange("")
            },
            title = stringResource(R.string.search),
            confirmText = stringResource(R.string.ok),
            onConfirm = {
                showSearchDialog = false
                onSearchQueryChange("")
            },
            showDismiss = false,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SearchPill(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = stringResource(R.string.search)
                    )

                    val res = state.searchResults
                    val hasResults = res.tasks.isNotEmpty() || res.notes.isNotEmpty() ||
                        res.courses.isNotEmpty() || res.transactions.isNotEmpty()
                    val totalResults = res.tasks.size + res.notes.size +
                        res.courses.size + res.transactions.size

                    if (state.searchQuery.isNotBlank() && !hasResults) {
                        Text(
                            text = stringResource(R.string.home_search_no_results, state.searchQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (state.searchQuery.isNotBlank() && hasResults) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.home_search_result_count,
                                totalResults,
                                totalResults
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            }
                        )
                    }

                    if (res.tasks.isNotEmpty()) {
                        Text(stringResource(R.string.home_search_tasks, res.tasks.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        res.tasks.forEach { task ->
                            Text(text = stringResource(R.string.home_search_bullet, task.title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (res.notes.isNotEmpty()) {
                        Text(stringResource(R.string.home_search_notes, res.notes.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        res.notes.forEach { note ->
                            Text(
                                text = stringResource(
                                    R.string.home_search_bullet,
                                    note.title.ifBlank { stringResource(R.string.notes_empty_title) }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (res.courses.isNotEmpty()) {
                        Text(stringResource(R.string.home_search_courses, res.courses.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        res.courses.forEach { course ->
                            Text(text = stringResource(R.string.home_search_bullet, course.name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (res.transactions.isNotEmpty()) {
                        Text(stringResource(R.string.home_search_transactions, res.transactions.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        res.transactions.forEach { tx ->
                            Text(
                                text = stringResource(
                                    R.string.home_search_bullet,
                                    "${tx.description.orEmpty()} (${Money.format(tx.amountCents)})"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        )
    }

    // Aviso automático de novedades: una sola vez tras actualizar (v7 A5).
    if (showWhatsNew) {
        CampusDialog(
            onDismissRequest = onWhatsNewDismiss,
            title = stringResource(R.string.settings_whats_new_dialog_title, BuildConfig.VERSION_NAME),
            confirmText = stringResource(R.string.ok),
            onConfirm = onWhatsNewDismiss,
            showDismiss = false,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    stringArrayResource(R.array.release_notes_current).forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", style = MaterialTheme.typography.bodyMedium)
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun HomeAvatar(displayName: String?, onClick: () -> Unit) {
    val ext = MaterialTheme.extended
    val description = stringResource(R.string.home_open_settings_desc)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ext.lavender),
            contentAlignment = Alignment.Center
        ) {
            if (displayName.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                    tint = ext.onLavender,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = ext.onLavender
                )
            }
        }
    }
}

@Composable
private fun NextClassCard(
    nextClass: HomeClass?,
    laterClass: HomeClass?,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    BentoCard(
        onClick = onClick,
        containerColor = ext.ink,
        contentColor = ext.onInk,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_next_class),
                style = MaterialTheme.typography.labelMedium,
                color = ext.lime
            )
            if (nextClass != null) {
                Text(
                    text = when {
                        nextClass.isCurrent -> stringResource(R.string.home_starting_now)
                        nextClass.minutesUntil in 1..59 -> stringResource(
                            R.string.home_in_minutes,
                            nextClass.minutesUntil.toInt()
                        )
                        else -> nextClass.startLabel
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.onInk,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ext.lime)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (nextClass == null) {
            Text(
                text = stringResource(R.string.home_no_next_class),
                style = MaterialTheme.typography.headlineSmall,
                color = ext.onInk
            )
            Text(
                text = stringResource(R.string.home_no_next_class_message),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.onInkMuted
            )
        } else {
            Text(
                text = nextClass.courseName,
                style = MaterialTheme.typography.headlineMedium,
                color = ext.onInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                        tint = ext.onInkMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${nextClass.startLabel} – ${nextClass.endLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onInkMuted
                    )
                }
                nextClass.room?.takeIf { it.isNotBlank() }?.let { room ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                            tint = ext.onInkMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = room,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.onInkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ext.onInk.copy(alpha = 0.14f))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = laterClass?.let {
                    stringResource(R.string.home_up_next, it.courseName, it.startLabel)
                } ?: stringResource(R.string.home_no_next_class),
                style = MaterialTheme.typography.bodySmall,
                color = ext.onInkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinanceSummaryCard(
    today: LocalDate,
    monthlyBudgetCents: Long?,
    monthlySpentCents: Long,
    monthlyRemainingCents: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val fraction = if (monthlyBudgetCents != null && monthlyBudgetCents > 0L) {
        (monthlySpentCents.toFloat() / monthlyBudgetCents).coerceIn(0f, 1f)
    } else {
        null
    }
    val pace = BudgetPace.compute(today, monthlyBudgetCents, monthlySpentCents)
    BentoCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = ext.accent,
        contentColor = ext.onAccent,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_remaining_label),
            style = MaterialTheme.typography.labelSmall,
            color = ext.onAccent.copy(alpha = 0.82f)
        )
        Spacer(Modifier.height(4.dp))
        if (monthlyRemainingCents != null) {
            MoneyText(
                amountCents = monthlyRemainingCents,
                style = MaterialTheme.typography.headlineMedium,
                color = ext.onAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = "—",
                style = MaterialTheme.typography.headlineMedium,
                color = ext.onAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (pace != null) {
            Text(
                text = stringResource(R.string.home_pace_per_day, Money.format(pace.perDayCents)),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ext.onAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = stringResource(R.string.budgets_no_budget),
                style = MaterialTheme.typography.labelSmall,
                color = ext.onAccent.copy(alpha = 0.82f)
            )
        }
        Spacer(Modifier.weight(1f))
        if (fraction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(ext.onAccent.copy(alpha = 0.22f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(5.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(ext.ink)
                )
            }
        }
    }
}

@Composable
private fun TasksSummaryCard(
    pendingTasks: Int,
    tasksDueToday: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    BentoCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_tasks_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = pendingTasks.toString(),
            style = CampusNumeral,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (pendingTasks == 0) {
                stringResource(R.string.home_tasks_all_done)
            } else {
                stringResource(R.string.home_tasks_pending, pendingTasks)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        if (tasksDueToday > 0) {
            Text(
                text = stringResource(R.string.home_tasks_due_today, tasksDueToday),
                style = MaterialTheme.typography.labelSmall,
                color = ext.onPeach,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(ext.peach)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}