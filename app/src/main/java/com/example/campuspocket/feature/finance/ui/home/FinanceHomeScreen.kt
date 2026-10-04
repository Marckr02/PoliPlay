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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.ui.accounts.AccountsScreen
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsScreen
import com.example.campuspocket.feature.finance.ui.calendar.CalendarScreen
import com.example.campuspocket.feature.finance.ui.history.HistoryScreen
import com.example.campuspocket.feature.finance.ui.payments.PaymentsScreen
import com.example.campuspocket.feature.finance.ui.summary.SummaryScreen

enum class FinanceTab { SUMMARY, ACCOUNTS, HISTORY, BUDGETS, CALENDAR, PAYMENTS }

/**
 * Pestaña Finanzas: solo Cuentas e Historial (Resumen/Presupuestos/Calendario son Fase 5).
 */
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
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf(FinanceTab.ACCOUNTS.name) }
    var showTypeSheet by remember { mutableStateOf(false) }
    val currentTab = FinanceTab.valueOf(tab)

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            // El "+" siempre abre la hoja de tipos de transacción, en ambas pestañas.
            ExtendedFloatingActionButton(
                onClick = { showTypeSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.transaction_new)) }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp)
            ) {
                listOf(
                    FinanceTab.SUMMARY to R.string.finance_dashboard,
                    FinanceTab.ACCOUNTS to R.string.finance_accounts,
                    FinanceTab.HISTORY to R.string.finance_history,
                    FinanceTab.BUDGETS to R.string.finance_budgets,
                    FinanceTab.CALENDAR to R.string.finance_calendar,
                    FinanceTab.PAYMENTS to R.string.finance_scheduled
                ).forEachIndexed { index, (option, labelRes) ->
                    SegmentedButton(
                        selected = currentTab == option,
                        onClick = { tab = option.name },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 6)
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            when (currentTab) {
                FinanceTab.SUMMARY -> SummaryScreen()
                FinanceTab.ACCOUNTS -> AccountsScreen(
                    onOpenAccount = onOpenAccount,
                    onAddAccount = onAddAccount,
                    onManageCategories = onManageCategories
                )
                FinanceTab.HISTORY -> HistoryScreen(onOpenTransaction = onOpenTransaction)
                FinanceTab.BUDGETS -> BudgetsScreen()
                FinanceTab.CALENDAR -> CalendarScreen(onDaySelected = { _ -> })
                FinanceTab.PAYMENTS -> PaymentsScreen(onBack = {}, onAddPayment = { onAddPayment() }, onEditPayment = { onEditPayment(it) })
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
            // Pago programado (quinto)
            ListItem(
                headlineContent = { Text(stringResource(R.string.finance_payment)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showTypeSheet = false
                        onAddPayment()
                    }
            )
        }
    }
}
