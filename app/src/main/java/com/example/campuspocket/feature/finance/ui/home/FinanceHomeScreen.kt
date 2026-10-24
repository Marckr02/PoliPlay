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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.snapshotFlow
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
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsViewModel
import com.example.campuspocket.feature.finance.ui.budgets.budgetItems
import com.example.campuspocket.feature.finance.ui.history.HistoryViewModel
import com.example.campuspocket.feature.finance.ui.history.historyItems
import com.example.campuspocket.feature.finance.ui.payments.PaymentsView
import com.example.campuspocket.feature.finance.ui.payments.PaymentsViewModel
import com.example.campuspocket.feature.finance.ui.payments.paymentItems
import com.example.campuspocket.feature.finance.ui.summary.BudgetPaceCard
import com.example.campuspocket.feature.finance.ui.summary.BudgetPaceSuggestionCard
import com.example.campuspocket.feature.finance.ui.summary.SummaryViewModel
import kotlinx.coroutines.flow.filterNotNull

enum class FinanceTab { MOVEMENTS, BUDGETS, PAYMENTS }

/**
 * Finanzas v5: un único LazyColumn de página completa. Cabecera, ritmo, cuentas
 * y resumen arriba; pestañas fijas (`stickyHeader`); debajo, cada pestaña
 * aporta sus propios ítems perezosos. La posición de scroll se guarda por pestaña.
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
    val ext = MaterialTheme.extended

    var tab by rememberSaveable { mutableStateOf(FinanceTab.MOVEMENTS.name) }
    val currentTab = FinanceTab.entries.firstOrNull { it.name == tab } ?: FinanceTab.MOVEMENTS
    var paymentsView by rememberSaveable { mutableStateOf(PaymentsView.LIST) }

    val listState = rememberLazyListState()

    // Posición por pestaña: firstVisibleItemIndex + offset (v5).
    val scrollPositions = rememberSaveable {
        mutableStateOf(HashMap<String, Pair<Int, Int>>())
    }
    var scrollRequest by remember { mutableStateOf(0) }

    // Último índice conocido de la cabecera de pestañas, calculado del layout.
    var tabsIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == "pestanas" }
                ?.index
        }.filterNotNull().collect { tabsIndex = it }
    }

    fun selectTab(selected: FinanceTab) {
        if (selected.name == tab) return
        scrollPositions.value[currentTab.name] =
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        tab = selected.name
        scrollRequest += 1
    }

    LaunchedEffect(scrollRequest) {
        if (scrollRequest == 0) return@LaunchedEffect
        val saved = scrollPositions.value[currentTab.name]
        if (saved != null) {
            listState.scrollToItem(saved.first, saved.second)
        } else {
            // Índice calculado de la cabecera de pestañas, nunca un número fijo.
            val target = tabsIndex ?: listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == "pestanas" }
                ?.index
            if (target != null) listState.animateScrollToItem(target)
        }
    }

    val tabContent = financeTabContent(
        currentTab = currentTab,
        paymentsView = paymentsView,
        onPaymentsViewChange = { paymentsView = it },
        onOpenTransaction = onOpenTransaction,
        onManageCategories = onManageCategories,
        onAddPayment = onAddPayment,
        onEditPayment = onEditPayment,
        onOpenCalendar = onOpenCalendar
    )

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

        tabContent()
    }
}

/**
 * Resuelve el ViewModel y el estado solo de la pestaña activa y devuelve el
 * bloque de ítems que Finanzas añadirá a su LazyColumn.
 */
@Composable
private fun financeTabContent(
    currentTab: FinanceTab,
    paymentsView: PaymentsView,
    onPaymentsViewChange: (PaymentsView) -> Unit,
    onOpenTransaction: (Long, TransactionType) -> Unit,
    onManageCategories: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onOpenCalendar: () -> Unit
): LazyListScope.() -> Unit = when (currentTab) {
    FinanceTab.MOVEMENTS -> {
        val viewModel: HistoryViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val content: LazyListScope.() -> Unit = {
            historyItems(
                state = state,
                viewModel = viewModel,
                onOpenTransaction = onOpenTransaction
            )
        }
        content
    }

    FinanceTab.BUDGETS -> {
        val viewModel: BudgetsViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val content: LazyListScope.() -> Unit = {
            budgetItems(
                state = state,
                viewModel = viewModel,
                onManageCategories = onManageCategories
            )
        }
        content
    }

    FinanceTab.PAYMENTS -> {
        val viewModel: PaymentsViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val content: LazyListScope.() -> Unit = {
            paymentItems(
                state = state,
                viewModel = viewModel,
                view = paymentsView,
                onViewChange = onPaymentsViewChange,
                onAddPayment = onAddPayment,
                onEditPayment = onEditPayment,
                onOpenCalendar = onOpenCalendar
            )
        }
        content
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
