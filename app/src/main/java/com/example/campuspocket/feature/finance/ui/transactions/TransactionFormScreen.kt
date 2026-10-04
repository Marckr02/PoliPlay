package com.example.campuspocket.feature.finance.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.TransactionError
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.ui.accounts.transactionTypeLabelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(transactionTypeLabelRes(state.type))) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!state.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.amountDisplay,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.transaction_amount)) },
                prefix = { Text("$") },
                isError = state.error == TransactionError.AMOUNT_NOT_POSITIVE,
                supportingText = {
                    if (state.error == TransactionError.AMOUNT_NOT_POSITIVE) {
                        Text(stringResource(R.string.transaction_error_amount))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

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
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.transaction_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        isError = state.error == TransactionError.MISSING_CATEGORY ||
                            state.error == TransactionError.WRONG_CATEGORY_KIND,
                        supportingText = {
                            when (state.error) {
                                TransactionError.MISSING_CATEGORY ->
                                    Text(stringResource(R.string.transaction_error_missing_category))
                                TransactionError.WRONG_CATEGORY_KIND ->
                                    Text(stringResource(R.string.transaction_error_wrong_kind))
                                else -> {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.onCategoryChange(category.id ?: 0L)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (state.type == TransactionType.REFUND) {
                var refundExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = refundExpanded,
                    onExpandedChange = { refundExpanded = it }
                ) {
                    OutlinedTextField(
                        value = refundableExpenses.firstOrNull { it.id == state.refundOfId }
                            ?.let { it.description?.takeIf(String::isNotBlank) ?: Money.format(it.amountCents) }
                            ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.refund_original_expense)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = refundExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = refundExpanded,
                        onDismissRequest = { refundExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no)) },
                            onClick = {
                                viewModel.onRefundOfChange(null)
                                refundExpanded = false
                            }
                        )
                        refundableExpenses.forEach { expense ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        expense.description?.takeIf { it.isNotBlank() }
                                            ?: Money.format(expense.amountCents)
                                    )
                                },
                                onClick = {
                                    viewModel.onRefundOfChange(expense.id)
                                    refundExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.transaction_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text(DATE_FORMATTER.format(state.date))
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.transaction_description)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.save))
            }

            if (state.isEditing) {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDateChange(
                                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    }
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_transaction_confirm)) }
        )
    }
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
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = accounts.firstOrNull { it.id == selectedId }?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = error,
            supportingText = { if (error) Text(stringResource(errorRes)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onSelect(account.id ?: 0L)
                        expanded = false
                    }
                )
            }
        }
    }
}
