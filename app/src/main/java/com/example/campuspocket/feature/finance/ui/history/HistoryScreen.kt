package com.example.campuspocket.feature.finance.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.SearchPill
import com.example.campuspocket.core.designsystem.components.TransactionRow
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.TransactionSubtype
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.ui.accounts.transactionTypeLabelRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))

/**
 * Movimientos: buscador en píldora, filtros compactos sin campos con borde y
 * una lista de transacciones en tarjetas sin elevación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenTransaction: (Long, TransactionType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDateRange by remember { mutableStateOf(false) }
    val ext = MaterialTheme.extended

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SearchPill(
            value = state.filters.query,
            onValueChange = viewModel::setQuery,
            placeholder = stringResource(R.string.search)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            item(key = "date") {
                DateFilterPill(
                    label = stringResource(R.string.filter_date_range),
                    value = if (state.filters.startDate != null && state.filters.endDate != null) {
                        DATE_FORMATTER.format(state.filters.startDate) + " – " +
                            DATE_FORMATTER.format(state.filters.endDate)
                    } else {
                        stringResource(R.string.filter_all)
                    },
                    onClick = { showDateRange = true }
                )
            }
            item(key = "type") {
                FilterPill(
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
            item(key = "account") {
                FilterPill(
                    label = stringResource(R.string.filter_account),
                    selected = state.filters.accountId?.let { id -> state.accounts[id]?.name }
                        ?: stringResource(R.string.filter_all),
                    options = listOf<Pair<Long?, String>>(
                        null to stringResource(R.string.filter_all)
                    ) + state.accounts.values.map { it.id to it.name },
                    onSelect = viewModel::setAccount
                )
            }
            item(key = "category") {
                FilterPill(
                    label = stringResource(R.string.filter_category),
                    selected = state.filters.categoryId?.let { id -> state.categories[id]?.name }
                        ?: stringResource(R.string.filter_all),
                    options = listOf<Pair<Long?, String>>(
                        null to stringResource(R.string.filter_all)
                    ) + state.categories.values.sortedBy { it.name }.map { it.id to it.name },
                    onSelect = viewModel::setCategory
                )
            }
            item(key = "withdrawal") {
                CampusChip(
                    label = stringResource(R.string.finance_withdrawal),
                    selected = state.filters.subtype == TransactionSubtype.RETIRO_CAJERO,
                    onClick = {
                        viewModel.setSubtype(
                            if (state.filters.subtype == TransactionSubtype.RETIRO_CAJERO) {
                                null
                            } else {
                                TransactionSubtype.RETIRO_CAJERO
                            }
                        )
                    }
                )
            }
        }

        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingShimmer()
                }
            }

            state.transactions.isEmpty() -> {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ext.lavender),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_wallet),
                                contentDescription = null,
                                tint = ext.onLavender,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.empty_transactions),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.empty_transactions_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            else -> state.transactions.forEach { tx ->
                HistoryTransactionCard(
                    tx = tx,
                    state = state,
                    onOpenTransaction = onOpenTransaction
                )
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

@Composable
private fun HistoryTransactionCard(
    tx: Transaction,
    state: HistoryUiState,
    onOpenTransaction: (Long, TransactionType) -> Unit
) {
    val ext = MaterialTheme.extended
    val category = state.categories[tx.categoryId]?.name.orEmpty()
    val accountName = state.accounts[tx.accountId]?.name.orEmpty()
    val targetAccountName = tx.targetAccountId
        ?.let { id -> state.accounts[id]?.name }
        .orEmpty()
    val typeLabel = stringResource(transactionTypeLabelRes(tx.type))
    val (iconContainer, iconContent) = when (tx.type) {
        TransactionType.EXPENSE -> ext.peach to ext.onPeach
        TransactionType.INCOME -> ext.mint to ext.onMint
        TransactionType.TRANSFER -> ext.sky to ext.onSky
        TransactionType.REFUND -> ext.lavender to ext.onLavender
    }
    val accountsLabel = if (
        tx.subtype == TransactionSubtype.RETIRO_CAJERO &&
        targetAccountName.isNotBlank()
    ) {
        "$accountName → $targetAccountName"
    } else {
        accountName
    }
    val title = tx.description?.takeIf { it.isNotBlank() }
        ?: if (tx.subtype == TransactionSubtype.RETIRO_CAJERO) {
            stringResource(R.string.finance_withdrawal)
        } else {
            typeLabel
        }
    val meta = DATE_WITH_YEAR.format(tx.date) +
        (if (accountsLabel.isNotBlank()) " · $accountsLabel" else "") +
        (if (category.isNotBlank()) " · $category" else "")
    BentoCard(
        contentPadding = PaddingValues(0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        TransactionRow(
            title = title,
            meta = meta,
            amount = Money.format(
                when (tx.type) {
                    TransactionType.EXPENSE,
                    TransactionType.TRANSFER -> -tx.amountCents
                    else -> tx.amountCents
                }
            ),
            iconText = category.takeIf { it.isNotBlank() } ?: typeLabel,
            iconColor = iconContainer,
            onIconColor = iconContent,
            amountColor = when (tx.type) {
                TransactionType.INCOME,
                TransactionType.REFUND -> ext.success
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
            },
            onClick = { tx.id?.let { onOpenTransaction(it, tx.type) } }
        )
    }
}

@Composable
private fun DateFilterPill(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(PillShape)
            .background(ext.track)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        FilterPillText(label = label, value = value)
    }
}

@Composable
private fun <T> FilterPill(
    label: String,
    selected: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val ext = MaterialTheme.extended
    Box(modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(PillShape)
                .background(ext.track)
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterPillText(label = label, value = selected)
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
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
private fun FilterPillText(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val DATE_WITH_YEAR: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))
