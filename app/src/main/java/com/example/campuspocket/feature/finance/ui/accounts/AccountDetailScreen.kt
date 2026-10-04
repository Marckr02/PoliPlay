@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.campuspocket.feature.finance.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Transaction
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

@Composable
fun AccountDetailScreen(
    onEditAccount: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val archived by viewModel.archived.collectAsStateWithLifecycle()
    var showArchiveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(archived) {
        if (archived) onBack()
    }
    if (archived) return

    val account = (state as? AccountDetailUiState.Content)?.account

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = account?.name ?: stringResource(R.string.finance_accounts),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    account?.id?.let { id ->
                        TextButton(onClick = { onEditAccount(id) }) {
                            Text(stringResource(R.string.edit))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (state) {
            AccountDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            AccountDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.course_detail_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is AccountDetailUiState.Content -> {
                val content = state as AccountDetailUiState.Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_movements),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (content.transactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.empty_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(content.transactions, key = { it.id ?: 0L }) { tx ->
                                TransactionRow(
                                    tx,
                                    accountId = content.account.id ?: 0L,
                                    accountName = content.account.name
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { showArchiveDialog = true },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.account_archive),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveDialog = false
                        viewModel.archive()
                    }
                ) {
                    Text(stringResource(R.string.account_archive))
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(account?.name ?: "") },
            text = { Text(stringResource(R.string.account_archived_hint)) }
        )
    }
}

@Composable
internal fun TransactionRow(
    tx: Transaction,
    accountId: Long,
    accountName: String,
    modifier: Modifier = Modifier
) {
    // Signo según el efecto en ESTA cuenta (spec 7.1).
    val isPositive = when (tx.type) {
        TransactionType.INCOME, TransactionType.REFUND -> true
        TransactionType.EXPENSE -> false
        TransactionType.TRANSFER -> tx.targetAccountId == accountId
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tx.description?.takeIf { it.isNotBlank() }
                        ?: stringResource(transactionTypeLabelRes(tx.type)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Money.format(if (isPositive) tx.amountCents else -tx.amountCents),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = DATE_FORMATTER.format(tx.date) + " · " + accountName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun transactionTypeLabelRes(type: TransactionType): Int = when (type) {
    TransactionType.EXPENSE -> R.string.finance_expense
    TransactionType.INCOME -> R.string.finance_income
    TransactionType.TRANSFER -> R.string.finance_transfer
    TransactionType.REFUND -> R.string.finance_refund
}
