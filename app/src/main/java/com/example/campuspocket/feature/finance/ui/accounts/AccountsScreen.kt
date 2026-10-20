package com.example.campuspocket.feature.finance.ui.accounts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.AccountsCarousel
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.domain.AccountBalance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.total_balance) + ": " + Money.format(state.totalCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                TextButton(onClick = onManageCategories) {
                    Text(stringResource(R.string.categories_manage))
                }
                TextButton(onClick = onAddAccount) {
                    Text(stringResource(R.string.finance_new_account))
                }
            }
        }

        when {
            state.isLoading -> {
                LoadingShimmer(modifier = Modifier.weight(1f))
            }

            state.balances.isEmpty() -> {
                EmptyStateCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.empty_finance),
                    message = stringResource(R.string.empty_finance),
                    icon = painterResource(R.drawable.ic_wallet),
                    actionText = stringResource(R.string.finance_new_account),
                    onAction = onAddAccount
                )
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Carrusel de cuentas principales
                    Text(
                        text = stringResource(R.string.finance_accounts),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    AccountsCarousel(
                        accounts = state.balances,
                        onAccountClick = { balance ->
                            balance.account.id?.let(onOpenAccount)
                        }
                    )

                    // Lista agrupada por tipo
                    val grouped = state.balances.groupBy { it.account.type }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountType.entries.forEach { type ->
                            val group = grouped[type].orEmpty()
                            if (group.isNotEmpty()) {
                                item(key = "header_$type") {
                                    Text(
                                        text = stringResource(accountTypeLabelRes(type)),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                }
                                items(group, key = { it.account.id ?: 0L }) { balance ->
                                    AccountRow(
                                        balance = balance,
                                        onClick = { balance.account.id?.let(onOpenAccount) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    balance: AccountBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(balance.account.colorArgb), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = balance.account.name.first().uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = balance.account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = Money.format(balance.balanceCents),
                style = MaterialTheme.typography.titleMedium,
                color = if (balance.balanceCents < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

internal fun accountTypeLabelRes(type: AccountType): Int = when (type) {
    AccountType.CASH -> R.string.account_type_cash
    AccountType.DEBIT -> R.string.account_type_debit
    AccountType.SAVINGS -> R.string.account_type_savings
    AccountType.CREDIT_CARD -> R.string.account_type_credit
}

@Composable
@Preview(name = "AccountsScreen", showBackground = true)
fun PreviewAccountsScreen() {
    com.example.campuspocket.core.designsystem.CampusTheme {
        androidx.compose.material3.Surface {
            AccountsScreen(
                onOpenAccount = {},
                onAddAccount = {},
                onManageCategories = {}
            )
        }
    }
}