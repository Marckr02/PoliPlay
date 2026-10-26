package com.example.campuspocket.feature.finance.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusButtonStyle
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.CampusDatePickerDialog
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.CampusSelectField
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.KeypadAmountInput
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
import com.example.campuspocket.core.designsystem.components.TransactionTypeSelector
import com.example.campuspocket.core.designsystem.formatMoneyOrHidden
import com.example.campuspocket.core.navigation.LocalSnackbarHost
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.TransactionError
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.TransactionTemplate
import com.example.campuspocket.feature.finance.ui.accounts.transactionTypeLabelRes

import java.time.LocalDate


import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.validCategories.collectAsStateWithLifecycle()
    val refundableExpenses by viewModel.refundableExpenses.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val snackbarHost = LocalSnackbarHost.current
    val haptics = LocalHapticFeedback.current
    val savedTemplateMessage = stringResource(R.string.template_saved)
    val deletedTemplateMessage = stringResource(R.string.template_deleted)
    val defaultTemplateName = stringResource(R.string.template_default_name)
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    val templateScroll = rememberScrollState()

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
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
            title = stringResource(transactionTypeLabelRes(state.type)),
            onBack = onDone
        )
        if (!state.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.isEditing && templates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.templates_section),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(templateScroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { template ->
                            TemplateChip(
                                template = template,
                                onClick = { viewModel.applyTemplate(template) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.deleteTemplate(template)
                                }
                            )
                        }
                    }
                }
            }

            KeypadAmountInput(
                amount = state.amountDisplay,
                onAmountChange = viewModel::onAmountChange,
                error = state.error == TransactionError.AMOUNT_NOT_POSITIVE,
                label = stringResource(R.string.transaction_amount)
            )

            if (state.error == TransactionError.AMOUNT_NOT_POSITIVE) {
                Text(
                    text = stringResource(R.string.transaction_error_amount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            AccountDropdown(
                label = stringResource(
                    if (state.type == TransactionType.TRANSFER) R.string.transaction_account
                    else R.string.transaction_account
                ),
                accounts = accounts,
                selectedId = state.accountId,
                error = state.error == TransactionError.MISSING_ACCOUNT,
                errorRes = R.string.transaction_error_missing_account,
                onSelect = viewModel::onAccountChange
            )
            if (state.usesLastAccount) {
                LastUsedLabel()
            }

            if (state.type == TransactionType.TRANSFER) {
                AccountDropdown(
                    label = stringResource(R.string.transaction_target_account),
                    accounts = accounts,
                    selectedId = state.targetAccountId,
                    error = state.error == TransactionError.MISSING_TARGET_ACCOUNT ||
                        state.error == TransactionError.TRANSFER_SAME_ACCOUNT,
                    errorRes = when (state.error) {
                        TransactionError.TRANSFER_SAME_ACCOUNT -> R.string.dialog_transfer_same_account
                        else -> R.string.transaction_error_missing_target
                    },
                    onSelect = viewModel::onTargetAccountChange
                )
            }

            if (state.type != TransactionType.TRANSFER) {
                var categoryExpanded by remember { mutableStateOf(false) }
                CampusSelectField(
                    value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "",
                    label = stringResource(R.string.transaction_category),
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    isError = state.error == TransactionError.MISSING_CATEGORY ||
                        state.error == TransactionError.WRONG_CATEGORY_KIND,
                    supportingText = when (state.error) {
                        TransactionError.MISSING_CATEGORY ->
                            stringResource(R.string.transaction_error_missing_category)
                        TransactionError.WRONG_CATEGORY_KIND ->
                            stringResource(R.string.transaction_error_wrong_kind)
                        else -> null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        CampusSelectItem(
                            text = category.name,
                            selected = category.id == state.categoryId,
                            onClick = {
                                viewModel.onCategoryChange(category.id ?: 0L)
                                categoryExpanded = false
                            }
                        )
                    }
                }
                if (state.usesLastCategory) {
                    LastUsedLabel()
                }
            }

            if (state.type == TransactionType.REFUND) {
                var refundExpanded by remember { mutableStateOf(false) }
                val refundValue = refundableExpenses.firstOrNull { it.id == state.refundOfId }
                    ?.let { it.description?.takeIf(String::isNotBlank) ?: formatMoneyOrHidden(it.amountCents) }
                    ?: ""
                CampusSelectField(
                    value = refundValue,
                    label = stringResource(R.string.refund_original_expense),
                    expanded = refundExpanded,
                    onExpandedChange = { refundExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CampusSelectItem(
                        text = stringResource(R.string.no),
                        selected = state.refundOfId == null,
                        onClick = {
                            viewModel.onRefundOfChange(null)
                            refundExpanded = false
                        }
                    )
                    refundableExpenses.forEach { expense ->
                        CampusSelectItem(
                            text = expense.description?.takeIf { it.isNotBlank() }
                                ?: formatMoneyOrHidden(expense.amountCents),
                            selected = expense.id == state.refundOfId,
                            onClick = {
                                viewModel.onRefundOfChange(expense.id)
                                refundExpanded = false
                            }
                        )
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.transaction_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryPillButton(
                    text = DATE_FORMATTER.format(state.date),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            CampusTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = stringResource(R.string.transaction_description),
                modifier = Modifier.fillMaxWidth()
            )
        }

        CampusButton(
            text = stringResource(R.string.save),
            onClick = viewModel::save,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (state.isEditing && state.type != TransactionType.TRANSFER) {
            CampusButton(
                text = stringResource(R.string.save_as_template),
                onClick = {
                    templateName = state.description.trim().ifBlank {
                        categories.firstOrNull { it.id == state.categoryId }?.name
                            ?: defaultTemplateName
                    }
                    showTemplateDialog = true
                },
                style = CampusButtonStyle.SECONDARY,
                enabled = state.amountCents > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            )
        }
        if (state.isEditing) {
            CampusButton(
                text = stringResource(R.string.delete),
                onClick = { showDeleteDialog = true },
                style = CampusButtonStyle.DESTRUCTIVE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            )
        }
    }

    if (showDatePicker) {
        CampusDatePickerDialog(
            title = stringResource(R.string.transaction_date),
            initialDate = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { viewModel.onDateChange(it); showDatePicker = false }
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
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selectedId: Long?,
    error: Boolean,
    errorRes: Int,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    CampusSelectField(
        value = accounts.firstOrNull { it.id == selectedId }?.name ?: "",
        label = label,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        isError = error,
        supportingText = if (error) stringResource(errorRes) else null,
        modifier = modifier
    ) {
        accounts.forEach { account ->
            CampusSelectItem(
                text = account.name,
                selected = account.id == selectedId,
                onClick = {
                    onSelect(account.id ?: 0L)
                    expanded = false
                }
            )
        }
    }
}

@Composable
@Preview(name = "TransactionFormScreen", showBackground = true)
fun PreviewTransactionFormScreen() {
    com.example.campuspocket.core.designsystem.CampusTheme {
        androidx.compose.material3.Surface {
            CompositionLocalProvider(LocalSnackbarHost provides SnackbarHostState()) {
                TransactionFormScreen(onDone = {})
            }
        }
    }
}

