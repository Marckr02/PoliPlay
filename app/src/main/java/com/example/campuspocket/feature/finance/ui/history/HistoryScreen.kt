package com.example.campuspocket.feature.finance.ui.history

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.ui.accounts.transactionTypeLabelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenTransaction: (Long, TransactionType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDateRange by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.filters.query,
            onValueChange = viewModel::setQuery,
            label = { Text(stringResource(R.string.search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Rango de fechas
            Box(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = if (state.filters.startDate != null && state.filters.endDate != null) {
                        DATE_FORMATTER.format(state.filters.startDate) + " - " +
                            DATE_FORMATTER.format(state.filters.endDate)
                    } else {
                        ""
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.filter_date_range)) },
                    trailingIcon = {
                        IconButton(onClick = { showDateRange = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                FilterDropdown(
                    label = stringResource(R.string.filter_type),
                    selected = state.filters.type?.let { stringResource(transactionTypeLabelRes(it)) }
                        ?: stringResource(R.string.filter_all),
                    options = listOf<Pair<TransactionType?, String>>(
                        null to stringResource(R.string.filter_all),
                        TransactionType.EXPENSE to stringResource(R.string.finance_expense),
                        TransactionType.INCOME to stringResource(R.string.finance_income),
                        TransactionType.TRANSFER to stringResource(R.string.finance_transfer),
                        TransactionType.REFUND to stringResource(R.string.finance_refund)
                    ),
                    onSelect = viewModel::setType
                )
            }
            Box(Modifier.weight(1f)) {
                FilterDropdown(
                    label = stringResource(R.string.filter_account),
                    selected = state.filters.accountId?.let { id -> state.accounts[id]?.name }
                        ?: stringResource(R.string.filter_all),
                    options = listOf<Pair<Long?, String>>(
                        null to stringResource(R.string.filter_all)
                    ) + state.accounts.values.map { it.id to it.name },
                    onSelect = viewModel::setAccount
                )
            }
        }

        FilterDropdown(
            label = stringResource(R.string.filter_category),
            selected = state.filters.categoryId?.let { id -> state.categories[id]?.name }
                ?: stringResource(R.string.filter_all),
            options = listOf<Pair<Long?, String>>(
                null to stringResource(R.string.filter_all)
            ) + state.categories.values.sortedBy { it.name }.map { it.id to it.name },
            onSelect = viewModel::setCategory
        )

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.transactions.isEmpty() -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.empty_transactions),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.transactions, key = { it.id ?: 0L }) { tx ->
                        HistoryRow(
                            tx = tx,
                            accountName = state.accounts[tx.accountId]?.name ?: "",
                            category = state.categories[tx.categoryId]?.name ?: "",
                            categoryColor = state.categories[tx.categoryId]?.colorArgb,
                            onClick = { tx.id?.let { onOpenTransaction(it, tx.type) } }
                        )
                    }
                }
            }
        }
    }

    if (showDateRange) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.filters.startDate
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = state.filters.endDate
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDateRange = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDateRange(
                            pickerState.selectedStartDateMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            },
                            pickerState.selectedEndDateMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                        )
                        showDateRange = false
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDateRange = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterDropdown(
    label: String,
    selected: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    tx: com.example.campuspocket.feature.finance.domain.model.Transaction,
    accountName: String,
    category: String,
    categoryColor: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (categoryColor != null && category.isNotBlank()) {
                Box(
                    modifier = Modifier.size(32.dp).background(Color(categoryColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.first().uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = if (categoryColor != null) 12.dp else 0.dp)) {
                Text(
                    text = tx.description?.takeIf { it.isNotBlank() }
                        ?: stringResource(transactionTypeLabelRes(tx.type)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = DATE_WITH_YEAR.format(tx.date) +
                        " · " + accountName +
                        (if (category.isNotBlank()) " · $category" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = Money.format(
                    when (tx.type) {
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> -tx.amountCents
                        else -> tx.amountCents
                    }
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private val DATE_WITH_YEAR: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))
