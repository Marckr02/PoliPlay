package com.example.campuspocket.feature.finance.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusNumeralHero
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusButtonStyle
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.CampusDatePickerDialog
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSelectField
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
import com.example.campuspocket.core.designsystem.components.TransactionKeypadPanel
import com.example.campuspocket.core.designsystem.components.TransactionTypeSelector
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.formatMoneyOrHidden
import com.example.campuspocket.core.navigation.LocalSnackbarHost
import com.example.campuspocket.core.navigation.showUndoSnackbar
import com.example.campuspocket.core.util.Haptics
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.AccountBalance
import com.example.campuspocket.feature.finance.domain.CategoryHabits
import com.example.campuspocket.feature.finance.domain.TransactionError
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.domain.model.TransactionTemplate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

private enum class AccountSheetTarget { SOURCE, TARGET }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val categories by viewModel.validCategories.collectAsStateWithLifecycle()
    val habits by viewModel.categoryHabits.collectAsStateWithLifecycle()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsStateWithLifecycle()
    val impact by viewModel.monthlyImpact.collectAsStateWithLifecycle()
    val refundableExpenses by viewModel.refundableExpenses.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val snackbarHost = LocalSnackbarHost.current
    val haptics = LocalHapticFeedback.current
    val savedMessage = stringResource(
        if (state.type == TransactionType.EXPENSE) {
            R.string.transaction_saved_undo
        } else {
            R.string.transaction_saved_undo_other
        }
    )
    val undoLabel = stringResource(R.string.undo)
    val savedTemplateMessage = stringResource(R.string.template_saved)
    val deletedTemplateMessage = stringResource(R.string.template_deleted)
    val defaultTemplateName = stringResource(R.string.template_default_name)

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var categoryQuery by remember { mutableStateOf("") }
    var accountSheet by remember { mutableStateOf<AccountSheetTarget?>(null) }
    var templateName by remember { mutableStateOf("") }
    val templateScroll = rememberScrollState()

    // Guardar y volver: deja ver el snackbar con Deshacer unos segundos.
    LaunchedEffect(state.saved) {
        if (!state.saved) return@LaunchedEffect
        if (!state.isEditing) {
            val undone = snackbarHost.showUndoSnackbar(
                message = savedMessage,
                actionLabel = undoLabel,
                timeoutMillis = 2_000L
            )
            if (undone) viewModel.deleteLastTransaction()
        }
        onDone()
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDone()
    }

    // "Seguir añadiendo": cada guardado muestra su propio snackbar sin salir.
    LaunchedEffect(state.savedCount) {
        if (state.savedCount <= 0) return@LaunchedEffect
        val undone = snackbarHost.showUndoSnackbar(
            message = savedMessage,
            actionLabel = undoLabel,
            timeoutMillis = 4_000L
        )
        if (undone) viewModel.deleteLastTransaction()
    }

    LaunchedEffect(state.templateFeedbackId) {
        val feedback = state.templateFeedback ?: return@LaunchedEffect
        snackbarHost.showSnackbar(
            when (feedback) {
                TemplateFeedback.SAVED -> savedTemplateMessage
                TemplateFeedback.DELETED -> deletedTemplateMessage
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        CampusTopBar(
            title = stringResource(transactionTitleRes(state.type, state.isEditing)),
            onBack = onDone,
            backContentDescription = stringResource(R.string.close),
            backIcon = Icons.Filled.Close
        )

        if (!state.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TransactionTypeSelector(
                    selectedType = state.type,
                    onTypeChange = viewModel::onTypeChange,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!state.isEditing && templates.isNotEmpty()) {
                    TemplatesRow(
                        templates = templates,
                        scrollState = templateScroll,
                        onApply = viewModel::applyTemplate,
                        onDelete = { template ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteTemplate(template)
                        }
                    )
                }

                AmountSection(
                    state = state,
                    impact = impact
                )

                if (state.type != TransactionType.TRANSFER) {
                    CategorySection(
                        categories = categories,
                        habits = habits,
                        selectedId = state.categoryId,
                        error = state.error,
                        onSelect = viewModel::onCategoryChange,
                        onMore = {
                            categoryQuery = ""
                            showCategorySheet = true
                        }
                    )
                    if (state.usesLastCategory) LastUsedLabel()
                }

                if (state.type == TransactionType.REFUND) {
                    RefundSection(
                        refundableExpenses = refundableExpenses,
                        selectedId = state.refundOfId,
                        onSelect = viewModel::onRefundOfChange
                    )
                }

                if (state.type == TransactionType.TRANSFER) {
                    AccountSection(
                        title = stringResource(R.string.withdrawal_from),
                        balances = accountBalances,
                        selectedId = state.accountId,
                        errorRes = if (state.error == TransactionError.MISSING_ACCOUNT) {
                            R.string.transaction_error_missing_account
                        } else {
                            null
                        },
                        lastUsed = state.usesLastAccount,
                        onSelect = viewModel::onAccountChange,
                        onMore = { accountSheet = AccountSheetTarget.SOURCE }
                    )
                    AccountSection(
                        title = stringResource(R.string.withdrawal_to),
                        balances = accountBalances,
                        selectedId = state.targetAccountId,
                        errorRes = when (state.error) {
                            TransactionError.MISSING_TARGET_ACCOUNT ->
                                R.string.transaction_error_missing_target
                            TransactionError.TRANSFER_SAME_ACCOUNT ->
                                R.string.dialog_transfer_same_account
                            else -> null
                        },
                        lastUsed = false,
                        onSelect = viewModel::onTargetAccountChange,
                        onMore = { accountSheet = AccountSheetTarget.TARGET }
                    )
                } else {
                    AccountSection(
                        title = stringResource(R.string.transaction_account_title),
                        balances = accountBalances,
                        selectedId = state.accountId,
                        errorRes = if (state.error == TransactionError.MISSING_ACCOUNT) {
                            R.string.transaction_error_missing_account
                        } else {
                            null
                        },
                        lastUsed = state.usesLastAccount,
                        onSelect = viewModel::onAccountChange,
                        onMore = { accountSheet = AccountSheetTarget.SOURCE }
                    )
                }

                DateSection(
                    date = state.date,
                    onDateChange = viewModel::onDateChange,
                    onChooseDate = { showDatePicker = true }
                )

                ExtrasSection(
                    state = state,
                    suggestions = descriptionSuggestions,
                    onToggleDescription = viewModel::onToggleDescription,
                    onToggleKeepAdding = viewModel::onToggleKeepAdding,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onApplySuggestion = viewModel::applyDescriptionSuggestion
                )

                if (state.isEditing && state.type != TransactionType.TRANSFER) {
                    SecondaryPillButton(
                        text = stringResource(R.string.save_as_template),
                        onClick = {
                            templateName = state.description.trim().ifBlank {
                                categories.firstOrNull { it.id == state.categoryId }?.name
                                    ?: defaultTemplateName
                            }
                            showTemplateDialog = true
                        },
                        enabled = state.amountCents > 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.isEditing) {
                    CampusButton(
                        text = stringResource(R.string.delete),
                        onClick = { showDeleteDialog = true },
                        style = CampusButtonStyle.DESTRUCTIVE,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            TransactionKeypadPanel(
                amountDigits = state.amountCents.toString(),
                onAmountChange = viewModel::onAmountChange,
                onSave = viewModel::save,
                saveEnabled = state.amountCents > 0 && !state.saved
            )
        }
    }

    if (showDatePicker) {
        CampusDatePickerDialog(
            title = stringResource(R.string.transaction_date),
            initialDate = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.onDateChange(it)
                showDatePicker = false
            }
        )
    }

    if (showDeleteDialog) {
        CampusDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.delete),
            message = stringResource(R.string.delete_transaction_confirm),
            icon = Icons.Filled.Warning,
            destructive = true,
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            }
        )
    }

    if (showTemplateDialog) {
        CampusDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = stringResource(R.string.save_as_template),
            confirmText = stringResource(R.string.save),
            confirmEnabled = templateName.isNotBlank(),
            onConfirm = {
                viewModel.saveAsTemplate(templateName)
                showTemplateDialog = false
            },
            content = {
                CampusTextField(
                    value = templateName,
                    onValueChange = { templateName = it.take(40) },
                    label = stringResource(R.string.template_name),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    if (showCategorySheet) {
        CategorySheet(
            categories = categories,
            recentIds = habits.recentCategoryIds,
            selectedId = state.categoryId,
            query = categoryQuery,
            onQueryChange = { categoryQuery = it },
            onSelect = { id ->
                viewModel.onCategoryChange(id)
                showCategorySheet = false
            },
            onCreate = { name ->
                viewModel.createCategory(name)
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    accountSheet?.let { target ->
        AccountSheet(
            title = if (target == AccountSheetTarget.TARGET) {
                stringResource(R.string.transaction_target_account)
            } else {
                stringResource(R.string.transaction_account_title)
            },
            balances = accountBalances,
            selectedId = if (target == AccountSheetTarget.TARGET) {
                state.targetAccountId
            } else {
                state.accountId
            },
            onSelect = { id ->
                if (target == AccountSheetTarget.TARGET) {
                    viewModel.onTargetAccountChange(id)
                } else {
                    viewModel.onAccountChange(id)
                }
                accountSheet = null
            },
            onDismiss = { accountSheet = null }
        )
    }
}

private fun transactionTitleRes(type: TransactionType, isEditing: Boolean): Int = when (type) {
    TransactionType.EXPENSE -> if (isEditing) {
        R.string.transaction_title_edit_expense
    } else {
        R.string.transaction_title_new_expense
    }
    TransactionType.INCOME -> if (isEditing) {
        R.string.transaction_title_edit_income
    } else {
        R.string.transaction_title_new_income
    }
    TransactionType.TRANSFER -> if (isEditing) {
        R.string.transaction_title_edit_transfer
    } else {
        R.string.transaction_title_new_transfer
    }
    TransactionType.REFUND -> if (isEditing) {
        R.string.transaction_title_edit_refund
    } else {
        R.string.transaction_title_new_refund
    }
}

@Composable
private fun TemplatesRow(
    templates: List<TransactionTemplate>,
    scrollState: androidx.compose.foundation.ScrollState,
    onApply: (TransactionTemplate) -> Unit,
    onDelete: (TransactionTemplate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.templates_section),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEach { template ->
                TemplateChip(
                    template = template,
                    onClick = { onApply(template) },
                    onLongClick = { onDelete(template) }
                )
            }
        }
    }
}

@Composable
private fun AmountSection(
    state: TransactionFormUiState,
    impact: TransactionImpact
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$ ${state.amountDisplay}",
            style = CampusNumeralHero.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (state.error == TransactionError.AMOUNT_NOT_POSITIVE) {
            Text(
                text = stringResource(R.string.transaction_error_amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

        val pace = impact.pace
        if (pace != null) {
            val over = pace.isOver
            val color = if (over) ext.onPeach else ext.onMint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Icon(
                    imageVector = if (over) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(
                        R.string.transaction_impact_below,
                        formatPercent(pace.realPct),
                        formatPercent(pace.expectedPct)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        } else if (
            state.type != TransactionType.EXPENSE &&
            impact.accountName != null &&
            impact.accountAfterCents != null
        ) {
            Text(
                text = stringResource(
                    R.string.transaction_account_after,
                    impact.accountName,
                    formatMoneyOrHidden(impact.accountAfterCents)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    categories: List<Category>,
    habits: CategoryHabits,
    selectedId: Long?,
    error: TransactionError?,
    onSelect: (Long) -> Unit,
    onMore: () -> Unit
) {
    val view = LocalView.current
    val suggestedLabel = stringResource(R.string.transaction_category_suggested)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.transaction_category_title))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleCategories(categories, habits, selectedId).forEach { category ->
                val isSuggested = habits.suggestedCategoryId == category.id
                CampusChip(
                    label = category.name,
                    selected = category.id == selectedId,
                    onClick = {
                        category.id?.let {
                            Haptics.tick(view)
                            onSelect(it)
                        }
                    },
                    leading = { CategoryDot(category.colorArgb) },
                    role = Role.RadioButton,
                    semanticLabel = if (isSuggested) {
                        "${category.name}, $suggestedLabel"
                    } else {
                        null
                    }
                )
            }
            CampusChip(
                label = stringResource(R.string.transaction_category_more),
                selected = false,
                onClick = onMore
            )
        }
        if (categories.isEmpty()) {
            Text(
                text = stringResource(R.string.transaction_categories_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when (error) {
            TransactionError.MISSING_CATEGORY -> ErrorText(R.string.transaction_error_missing_category)
            TransactionError.WRONG_CATEGORY_KIND -> ErrorText(R.string.transaction_error_wrong_kind)
            else -> Unit
        }
    }
}

@Composable
private fun RefundSection(
    refundableExpenses: List<Transaction>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = refundableExpenses.firstOrNull { it.id == selectedId }
        ?.let { it.description?.takeIf(String::isNotBlank) ?: formatMoneyOrHidden(it.amountCents) }
        ?: ""
    CampusSelectField(
        value = selectedLabel,
        label = stringResource(R.string.refund_original_expense),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        CampusSelectItem(
            text = stringResource(R.string.no),
            selected = selectedId == null,
            onClick = {
                onSelect(null)
                expanded = false
            }
        )
        refundableExpenses.forEach { expense ->
            CampusSelectItem(
                text = expense.description?.takeIf { it.isNotBlank() }
                    ?: formatMoneyOrHidden(expense.amountCents),
                selected = expense.id == selectedId,
                onClick = {
                    onSelect(expense.id)
                    expanded = false
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountSection(
    title: String,
    balances: List<AccountBalance>,
    selectedId: Long?,
    errorRes: Int?,
    lastUsed: Boolean,
    onSelect: (Long) -> Unit,
    onMore: () -> Unit
) {
    val view = LocalView.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        if (balances.isEmpty()) {
            Text(
                text = stringResource(R.string.transaction_accounts_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleBalances(balances, selectedId).forEach { balance ->
                    val account = balance.account
                    CampusChip(
                        label = stringResource(
                            R.string.transaction_account_balance,
                            account.name,
                            formatMoneyOrHidden(balance.balanceCents)
                        ),
                        selected = account.id == selectedId,
                        onClick = {
                            account.id?.let {
                                Haptics.tick(view)
                                onSelect(it)
                            }
                        },
                        leading = { CategoryDot(account.colorArgb) },
                        role = Role.RadioButton,
                        modifier = Modifier.heightIn(min = 52.dp)
                    )
                }
                if (balances.size > visibleBalances(balances, selectedId).size) {
                    CampusChip(
                        label = stringResource(R.string.transaction_category_more),
                        selected = false,
                        onClick = onMore
                    )
                }
            }
        }
        if (lastUsed) LastUsedLabel()
        errorRes?.let { ErrorText(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateSection(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onChooseDate: () -> Unit
) {
    val view = LocalView.current
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isOther = date != today && date != yesterday
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.transaction_date))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CampusChip(
                label = stringResource(R.string.transaction_date_today),
                selected = date == today,
                onClick = {
                    Haptics.tick(view)
                    onDateChange(today)
                },
                role = Role.RadioButton
            )
            CampusChip(
                label = stringResource(R.string.transaction_date_yesterday),
                selected = date == yesterday,
                onClick = {
                    Haptics.tick(view)
                    onDateChange(yesterday)
                },
                role = Role.RadioButton
            )
            CampusChip(
                label = if (isOther) {
                    DATE_FORMATTER.format(date)
                } else {
                    stringResource(R.string.transaction_date_choose)
                },
                selected = isOther,
                onClick = onChooseDate,
                role = Role.RadioButton
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtrasSection(
    state: TransactionFormUiState,
    suggestions: List<Transaction>,
    onToggleDescription: () -> Unit,
    onToggleKeepAdding: (Boolean) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onApplySuggestion: (Transaction) -> Unit
) {
    val view = LocalView.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CampusChip(
                label = stringResource(R.string.transaction_add_note),
                selected = state.showDescription,
                onClick = {
                    Haptics.tick(view)
                    onToggleDescription()
                }
            )
            CampusChip(
                label = stringResource(R.string.transaction_keep_adding),
                selected = state.keepAdding,
                onClick = {
                    Haptics.tick(view)
                    onToggleKeepAdding(!state.keepAdding)
                }
            )
        }

        if (state.showDescription) {
            CampusTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.transaction_description),
                modifier = Modifier.fillMaxWidth()
            )
            if (suggestions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.transaction_description_suggestions),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        val label = suggestion.description.orEmpty().let {
                            if (it.length > 28) it.take(27) + "…" else it
                        }
                        CampusChip(
                            label = label,
                            selected = false,
                            onClick = { onApplySuggestion(suggestion) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    categories: List<Category>,
    recentIds: List<Long>,
    selectedId: Long?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val byId = categories.mapNotNull { category -> category.id?.let { it to category } }.toMap()
    val ordered = buildList {
        recentIds.forEach { id -> byId[id]?.let(::add) }
        addAll(categories)
    }.distinctBy { it.id }
    val cleanQuery = query.trim()
    val filtered = if (cleanQuery.isEmpty()) {
        ordered
    } else {
        ordered.filter { it.name.contains(cleanQuery, ignoreCase = true) }
    }
    val exactExists = categories.any { it.name.equals(cleanQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.transaction_category_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            CampusTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "",
                placeholder = stringResource(R.string.transaction_category_search),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { "${it.id}:${it.name}" }) { category ->
                    CategorySheetRow(
                        category = category,
                        selected = category.id == selectedId,
                        onClick = { category.id?.let(onSelect) }
                    )
                }
                if (cleanQuery.isNotEmpty() && !exactExists) {
                    item(key = "create") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.extended.accentMuted)
                                .clickable(role = Role.Button) { onCreate(cleanQuery) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.transaction_category_create, cleanQuery),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.extended.onAccentMuted
                            )
                        }
                    }
                }
                if (filtered.isEmpty() && cleanQuery.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.transaction_categories_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySheetRow(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryDot(category.colorArgb)
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                tint = MaterialTheme.extended.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    title: String,
    balances: List<AccountBalance>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(balances, key = { "${it.account.id}:${it.account.name}" }) { balance ->
                    val account = balance.account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.RadioButton) {
                                account.id?.let(onSelect)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryDot(account.colorArgb)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatMoneyOrHidden(balance.balanceCents),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (account.id == selectedId) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                                tint = MaterialTheme.extended.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun ErrorText(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
private fun CategoryDot(colorArgb: Int) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
    )
}

private fun visibleCategories(
    categories: List<Category>,
    habits: CategoryHabits,
    selectedId: Long?
): List<Category> {
    val byId = categories.mapNotNull { category -> category.id?.let { it to category } }.toMap()
    val ids = buildList {
        selectedId?.let(::add)
        habits.suggestedCategoryId?.let(::add)
        addAll(habits.mostUsedCategoryIds)
        addAll(categories.mapNotNull { it.id })
    }.distinct()
    return ids.mapNotNull { byId[it] }.take(5)
}

private fun visibleBalances(
    balances: List<AccountBalance>,
    selectedId: Long?
): List<AccountBalance> {
    val limit = if (balances.size > 3) 2 else 3
    val selected = balances.firstOrNull { it.account.id == selectedId }
    return buildList {
        selected?.let(::add)
        balances.forEach { balance ->
            if (size < limit && none { it.account.id == balance.account.id }) add(balance)
        }
    }.take(limit)
}

private fun formatPercent(value: BigDecimal): String =
    value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " %"

@Composable
private fun TemplateChip(
    template: TransactionTemplate,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    CampusChip(
        label = "${template.name} · ${formatMoneyOrHidden(template.amountCents)}",
        selected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(R.string.template_delete_action)
    )
}

@Composable
private fun LastUsedLabel() {
    Text(
        text = stringResource(R.string.last_used),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
@Preview(name = "TransactionFormScreen", showBackground = true)
fun PreviewTransactionFormScreen() {
    com.example.campuspocket.core.designsystem.CampusTheme {
        Surface {
            CompositionLocalProvider(LocalSnackbarHost provides SnackbarHostState()) {
                TransactionFormScreen(onDone = {})
            }
        }
    }
}
