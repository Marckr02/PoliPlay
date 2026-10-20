package com.example.campuspocket.feature.finance.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.ui.accounts.AccountsScreen
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsScreen
import com.example.campuspocket.feature.finance.ui.calendar.CalendarScreen
import com.example.campuspocket.feature.finance.ui.history.HistoryScreen
import com.example.campuspocket.feature.finance.ui.payments.PaymentsScreen
import com.example.campuspocket.feature.finance.ui.summary.SummaryScreen

enum class FinanceTab { SUMMARY, ACCOUNTS, HISTORY, BUDGETS, CALENDAR, PAYMENTS }

@OptIn(ExperimentalMaterial3Api::class)
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
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf(FinanceTab.ACCOUNTS.name) }
    var showTypeSheet by remember { mutableStateOf(false) }
    val currentTab = FinanceTab.valueOf(tab)

    val tabs = listOf(
        FinanceTab.SUMMARY to R.string.finance_dashboard,
        FinanceTab.ACCOUNTS to R.string.finance_accounts,
        FinanceTab.HISTORY to R.string.finance_history,
        FinanceTab.BUDGETS to R.string.finance_budgets,
        FinanceTab.CALENDAR to R.string.finance_calendar,
        FinanceTab.PAYMENTS to R.string.finance_scheduled
    )

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTypeSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.transaction_new)) },
                text = { Text(stringResource(R.string.transaction_new)) }
            )
        }
    ) { innerPadding ->
        CampusTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOfFirst { it.first == currentTab }.coerceAtLeast(0),
                    edgePadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEach { (option, labelRes) ->
                        Tab(
                            selected = currentTab == option,
                            onClick = { tab = option.name },
                            text = {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (currentTab) {
                        FinanceTab.SUMMARY -> SummaryScreen(
                            onDefineBudget = { tab = FinanceTab.BUDGETS.name }
                        )
                        FinanceTab.ACCOUNTS -> AccountsScreen(
                            onOpenAccount = onOpenAccount,
                            onAddAccount = onAddAccount,
                            onManageCategories = onManageCategories
                        )
                        FinanceTab.HISTORY -> HistoryScreen(onOpenTransaction = onOpenTransaction)
                        FinanceTab.BUDGETS -> BudgetsScreen()
                        FinanceTab.CALENDAR -> CalendarScreen(onDaySelected = { _ -> })
                        FinanceTab.PAYMENTS -> PaymentsScreen(
                            onBack = {},
                            onAddPayment = { onAddPayment() },
                            onEditPayment = { onEditPayment(it) }
                        )
                    }
                }
            }
        }
    }

    if (showTypeSheet) {
        ModalBottomSheet(onDismissRequest = { showTypeSheet = false }) {
            listOf(
                TransactionType.EXPENSE to R.string.finance_expense,
                TransactionType.INCOME to R.string.finance_income,
                TransactionType.TRANSFER to R.string.finance_transfer,
                TransactionType.REFUND to R.string.finance_refund
            ).forEach { (type, labelRes) ->
                ListItem(
                    headlineContent = { Text(stringResource(labelRes)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showTypeSheet = false
                            onAddTransaction(type)
                        }
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.finance_payment)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showTypeSheet = false
                        onAddPayment()
                    }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.finance_withdrawal)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showTypeSheet = false
                        onWithdrawal()
                    }
            )
        }
    }
}

@Composable
@Preview(name = "FinanceHomeScreen", showBackground = true)
fun PreviewFinanceHomeScreen() {
    CampusTheme {
        Surface {
            FinanceHomeScreen(
                onOpenAccount = {},
                onAddAccount = {},
                onManageCategories = {},
                onOpenTransaction = { _, _ -> },
                onAddTransaction = { _ -> },
                onAddPayment = {},
                onEditPayment = {},
                onWithdrawal = {}
            )
        }
    }
}