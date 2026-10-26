package com.example.campuspocket.feature.finance.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.campuspocket.core.designsystem.MoneyText
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.DangerPillButton
import com.example.campuspocket.core.designsystem.components.RoundIconButton
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

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = account?.name ?: stringResource(R.string.finance_accounts),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            account?.id?.let { id ->
                RoundIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.edit),
                    onClick = { onEditAccount(id) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        when (state) {
            AccountDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            AccountDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.account_movements),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (content.transactions.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.empty_transactions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(content.transactions, key = { it.id ?: 0L }) { tx ->
                            TransactionRow(
                                tx,
                                accountId = content.account.id ?: 0L,
                                accountName = content.account.name,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    item {
                        DangerPillButton(
                            text = stringResource(R.string.account_archive),
                            onClick = { showArchiveDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showArchiveDialog) {
        CampusDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = account?.name ?: "",
            message = stringResource(R.string.account_archived_hint),
            icon = Icons.Filled.Warning,
            destructive = true,
            confirmText = stringResource(R.string.account_archive),
            onConfirm = {
                showArchiveDialog = false
                viewModel.archive()
            }
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
    BentoCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
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
            MoneyText(
                amountCents = if (isPositive) tx.amountCents else -tx.amountCents,
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

internal fun transactionTypeLabelRes(type: TransactionType): Int = when (type) {
    TransactionType.EXPENSE -> R.string.finance_expense
    TransactionType.INCOME -> R.string.finance_income
    TransactionType.TRANSFER -> R.string.finance_transfer
    TransactionType.REFUND -> R.string.finance_refund
}
