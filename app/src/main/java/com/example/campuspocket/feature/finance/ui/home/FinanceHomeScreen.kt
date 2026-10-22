package com.example.campuspocket.feature.finance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.AccountsCarousel
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.PillSegmentedControl
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.ui.accounts.AccountsViewModel
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsScreen
import com.example.campuspocket.feature.finance.ui.history.HistoryScreen
import com.example.campuspocket.feature.finance.ui.payments.PaymentsScreen
import com.example.campuspocket.feature.finance.ui.summary.BudgetPaceCard
import com.example.campuspocket.feature.finance.ui.summary.BudgetPaceSuggestionCard
import com.example.campuspocket.feature.finance.ui.summary.SummaryViewModel

enum class FinanceTab { MOVEMENTS, BUDGETS, PAYMENTS }

/**
 * Finanzas v4: cabecera fija con título, hero de ritmo del presupuesto, cuentas
 * compactas, fila de resumen y tres pestañas. La creación vive en el botón
 * contextual del dock y su hoja; aquí no se duplican acciones.
 */
@Composable
fun FinanceHomeScreen(
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onManageCategories: () -> Unit,
    onOpenTransaction: (Long, TransactionType) -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
    summaryViewModel: SummaryViewModel = hiltViewModel(),
    accountsViewModel: AccountsViewModel = hiltViewModel()
) {
    val summary by summaryViewModel.uiState.collectAsStateWithLifecycle()
    val accounts by accountsViewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(FinanceTab.MOVEMENTS.name) }
    val currentTab = FinanceTab.entries.firstOrNull { it.name == tab } ?: FinanceTab.MOVEMENTS
    val ext = MaterialTheme.extended

    val listState = rememberLazyListState()
    var scrollToTabs by remember { mutableStateOf(false) }

    fun selectTab(selected: FinanceTab) {
        if (tab != selected.name) {
            tab = selected.name
            scrollToTabs = true
        }
    }

    LaunchedEffect(scrollToTabs) {
        if (scrollToTabs) {
            listState.animateScrollToItem(4)
            scrollToTabs = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "cabecera") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.finance_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                RoundIconButton(
                    icon = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.finance_open_calendar),
                    onClick = onOpenCalendar,
                    size = 48.dp,
                    containerColor = ext.track,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item(key = "ritmo") {
            Box(Modifier.padding(horizontal = 20.dp)) {
                val pace = summary.pace
                if (pace == null) {
                    BudgetPaceSuggestionCard(
                        onDefineBudget = { selectTab(FinanceTab.BUDGETS) }
                    )
                } else {
                    BudgetPaceCard(
                        pace = pace,
                        onEditBudget = { selectTab(FinanceTab.BUDGETS) }
                    )
                }
            }
        }

        item(key = "cuentas") {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
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
                        size = 48.dp,
                        containerColor = ext.track,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (accounts.balances.isEmpty()) {
                    BentoCard(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(18.dp)
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
            }
        }

        item(key = "resumen") {
            FinanceSummaryRow(
                onClick = onOpenSummary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        stickyHeader(key = "pestanas") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                PillSegmentedControl(
                    options = FinanceTab.entries,
                    selected = currentTab,
                    onSelect = { selected -> selectTab(selected) },
                    label = { option ->
                        stringResource(
                            when (option) {
                                FinanceTab.MOVEMENTS -> R.string.finance_tab_movements
                                FinanceTab.BUDGETS -> R.string.finance_tab_budgets
                                FinanceTab.PAYMENTS -> R.string.finance_tab_payments
                            }
                        )
                    }
                )
            }
        }

        item(key = "contenido_${currentTab.name}") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                when (currentTab) {
                    FinanceTab.MOVEMENTS -> HistoryScreen(
                        onOpenTransaction = onOpenTransaction,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTab.BUDGETS -> BudgetsScreen(
                        onManageCategories = onManageCategories,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FinanceTab.PAYMENTS -> PaymentsScreen(
                        onAddPayment = onAddPayment,
                        onEditPayment = onEditPayment,
                        onOpenCalendar = onOpenCalendar,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Fila única que abre el resumen completo del mes como pantalla secundaria. */
@Composable
private fun FinanceSummaryRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.finance_summary_row_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.finance_summary_view),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
