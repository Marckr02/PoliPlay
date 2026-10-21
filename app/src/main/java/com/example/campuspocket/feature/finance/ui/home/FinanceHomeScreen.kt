package com.example.campuspocket.feature.finance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.components.AccountsCarousel
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.PillSegmentedControl
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.ui.accounts.AccountsViewModel
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsScreen
import com.example.campuspocket.feature.finance.ui.history.HistoryScreen
import com.example.campuspocket.feature.finance.ui.payments.PaymentsScreen
import com.example.campuspocket.feature.finance.ui.summary.SummaryViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class FinanceTab { MOVEMENTS, BUDGETS, PAYMENTS }

/**
 * Finanzas: resumen y cuentas en la cabecera, cuatro acciones rápidas y tres
 * pestañas. Sin barra de pestañas Material ni FAB: la creación vive en el dock contextual
 * y en las acciones de esta pantalla.
 */
@Composable
fun FinanceHomeScreen(
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onManageCategories: () -> Unit,
    onOpenTransaction: (Long, TransactionType) -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onWithdrawal: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    summaryViewModel: SummaryViewModel = hiltViewModel(),
    accountsViewModel: AccountsViewModel = hiltViewModel()
) {
    val summary by summaryViewModel.uiState.collectAsStateWithLifecycle()
    val accounts by accountsViewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(FinanceTab.MOVEMENTS.name) }
    val currentTab = FinanceTab.entries.firstOrNull { it.name == tab } ?: FinanceTab.MOVEMENTS
    val ext = MaterialTheme.extended
    val monthLabel = YearMonth.now()
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es")))
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("es")) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.finance_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(ext.track)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.onTrack
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.finance_balance_total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Money.format(summary.totalBalanceCents),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceChip(
                    iconRes = R.drawable.ic_arrow_down,
                    amountText = "+" + Money.format(summary.incomeCents),
                    contentDescription = stringResource(
                        R.string.finance_income_amount,
                        Money.format(summary.incomeCents)
                    ),
                    container = ext.mint,
                    content = ext.onMint
                )
                BalanceChip(
                    iconRes = R.drawable.ic_arrow_up,
                    amountText = "−" + Money.format(summary.expenseCents),
                    contentDescription = stringResource(
                        R.string.finance_expense_amount,
                        Money.format(summary.expenseCents)
                    ),
                    container = ext.peach,
                    content = ext.onPeach
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.finance_accounts),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            RoundIconButton(
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.finance_add_account),
                onClick = onAddAccount,
                size = 44.dp,
                containerColor = ext.track,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))

        if (accounts.balances.isEmpty()) {
            BentoCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.finance_no_accounts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                PrimaryPillButton(
                    text = stringResource(R.string.finance_add_account),
                    onClick = onAddAccount
                )
            }
        } else {
            AccountsCarousel(
                modifier = Modifier.padding(start = 20.dp),
                accounts = accounts.balances,
                onAccountClick = { onOpenAccount(it.account.id ?: 0L) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickFinanceAction(
                iconRes = R.drawable.ic_arrow_up,
                label = stringResource(R.string.finance_quick_expense),
                container = ext.peach,
                content = ext.onPeach,
                onClick = { onAddTransaction(TransactionType.EXPENSE) },
                modifier = Modifier.weight(1f)
            )
            QuickFinanceAction(
                iconRes = R.drawable.ic_arrow_down,
                label = stringResource(R.string.finance_quick_income),
                container = ext.mint,
                content = ext.onMint,
                onClick = { onAddTransaction(TransactionType.INCOME) },
                modifier = Modifier.weight(1f)
            )
            QuickFinanceAction(
                iconRes = R.drawable.ic_swap,
                label = stringResource(R.string.finance_quick_transfer),
                container = ext.sky,
                content = ext.onSky,
                onClick = { onAddTransaction(TransactionType.TRANSFER) },
                modifier = Modifier.weight(1f)
            )
            QuickFinanceAction(
                iconRes = R.drawable.ic_wallet,
                label = stringResource(R.string.finance_quick_withdraw),
                container = ext.lavender,
                content = ext.onLavender,
                onClick = onWithdrawal,
                modifier = Modifier.weight(1f)
            )
        }

        PillSegmentedControl(
            options = FinanceTab.entries,
            selected = currentTab,
            onSelect = { tab = it.name },
            label = { option ->
                stringResource(
                    when (option) {
                        FinanceTab.MOVEMENTS -> R.string.finance_tab_movements
                        FinanceTab.BUDGETS -> R.string.finance_tab_budgets
                        FinanceTab.PAYMENTS -> R.string.finance_tab_payments
                    }
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Box(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
            when (currentTab) {
                FinanceTab.MOVEMENTS -> HistoryScreen(
                    onOpenTransaction = onOpenTransaction,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                )
                FinanceTab.BUDGETS -> BudgetsScreen(onManageCategories = onManageCategories)
                FinanceTab.PAYMENTS -> PaymentsScreen(
                    onAddPayment = onAddPayment,
                    onEditPayment = onEditPayment,
                    onOpenCalendar = onOpenCalendar
                )
            }
        }
    }
}

@Composable
private fun BalanceChip(
    iconRes: Int,
    amountText: String,
    contentDescription: String,
    container: Color,
    content: Color
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(iconRes),
            contentDescription = null,
            tint = content,
            modifier = Modifier.width(14.dp).height(14.dp)
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.labelSmall,
            color = content
        )
    }
}

@Composable
private fun QuickFinanceAction(
    iconRes: Int,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoundIconButton(
            iconRes = iconRes,
            contentDescription = label,
            onClick = onClick,
            size = 56.dp,
            containerColor = container,
            contentColor = content
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}