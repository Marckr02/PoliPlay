package com.example.campuspocket.feature.finance.ui.budgets

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.BudgetProgressBar
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusButtonStyle
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.PillSegmentedControl
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.ui.summary.BudgetPaceCard
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    onManageCategories: () -> Unit = {},
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingCategory by remember { mutableStateOf<GetMonthSummary.CategoryRow?>(null) }
    var showTotalBudgetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Selector de mes + modo semestre
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                text = state.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                    .replaceFirstChar { it.uppercase() } + " " + state.yearMonth.year,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillSegmentedControl(
                options = listOf(false, true),
                selected = state.semesterMode,
                onSelect = { semester ->
                    if (state.semesterMode != semester) viewModel.toggleSemesterMode()
                },
                label = { semester ->
                    stringResource(
                        if (semester) R.string.budget_semester else R.string.budget_month
                    )
                }
            )
        }

        if (state.isLoading) {
            LoadingShimmer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val withBudget = state.rows.filter { it.budgetCents != null }
                val withoutBudget = state.rows.filter { it.budgetCents == null }

                item(key = "categorias") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CampusChip(
                            label = stringResource(R.string.finance_categories),
                            selected = false,
                            onClick = onManageCategories
                        )
                    }
                }

                if (!state.semesterMode) {
                    item(key = "presupuesto_total") {
                        MonthlyBudgetCard(
                            amountCents = state.monthlyBudgetCents,
                            spentCents = state.totalSpentCents,
                            onClick = { showTotalBudgetDialog = true }
                        )
                    }
                    state.pace?.let { pace ->
                        item(key = "ritmo") {
                            BudgetPaceCard(
                                pace = pace,
                                onEditBudget = { showTotalBudgetDialog = true }
                            )
                        }
                    }
                }

                if (withBudget.isNotEmpty()) {
                    item(key = "hdr_definidos") {
                        Text(
                            text = stringResource(R.string.budgets_monthly),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(withBudget, key = { it.categoryId }) { row ->
                        BudgetRow(
                            row = row,
                            onClick = { editingCategory = row }
                        )
                    }
                }
                if (withoutBudget.isNotEmpty()) {
                    item(key = "hdr_sin") {
                        Text(
                            text = stringResource(R.string.budgets_no_budget),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(withoutBudget, key = { it.categoryId }) { row ->
                        BudgetRow(row = row, onClick = { editingCategory = row })
                    }
                }
                if (state.rows.isEmpty()) {
                    item(key = "vacio") {
                        EmptyStateCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.empty_budgets),
                            message = stringResource(R.string.empty_budgets_message),
                            icon = painterResource(R.drawable.ic_wallet)
                        )
                    }
                }

                item(key = "copiar") {
                    CampusButton(
                        text = stringResource(R.string.budget_copy_previous),
                        onClick = { viewModel.copyFromPreviousMonth() },
                        style = CampusButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    editingCategory?.let { row ->
        BudgetEditDialog(
            row = row,
            onDismiss = { editingCategory = null },
            onSave = { cents ->
                viewModel.saveBudget(row.categoryId, cents)
                editingCategory = null
            },
            onDelete = if (row.budgetCents != null) {
                {
                    viewModel.deleteBudget(row.categoryId)
                    editingCategory = null
                }
            } else null
        )
    }

    if (showTotalBudgetDialog) {
        MonthlyBudgetEditDialog(
            amountCents = state.monthlyBudgetCents,
            onDismiss = { showTotalBudgetDialog = false },
            onSave = { cents ->
                viewModel.saveMonthlyBudget(cents)
                showTotalBudgetDialog = false
            },
            onDelete = if (state.monthlyBudgetCents != null) {
                {
                    viewModel.deleteMonthlyBudget()
                    showTotalBudgetDialog = false
                }
            } else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyBudgetCard(
    amountCents: Long?,
    spentCents: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.budget_total_month),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (amountCents != null) {
                        Money.format(spentCents) + " / " + Money.format(amountCents)
                    } else {
                        stringResource(R.string.budget_total_define)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (amountCents != null) {
                Text(
                    text = Money.format(amountCents),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun MonthlyBudgetEditDialog(
    amountCents: Long?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onDelete: (() -> Unit)?
) {
    var value by remember(amountCents) { mutableStateOf(amountCents ?: 0L) }

    CampusDialog(
        onDismissRequest = onDismiss,
        title = stringResource(
            if (amountCents == null) R.string.budget_total_define else R.string.budget_total_edit
        ),
        confirmText = stringResource(R.string.save),
        confirmEnabled = value > 0,
        onConfirm = { if (value > 0) onSave(value) },
        content = {
            CampusTextField(
                value = Money.toLocalDecimalString(value),
                onValueChange = { text ->
                    value = text.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L
                },
                label = stringResource(R.string.budget_amount),
                prefix = "$",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (onDelete != null) {
                CampusButton(
                    text = stringResource(R.string.delete),
                    onClick = onDelete,
                    style = CampusButtonStyle.DESTRUCTIVE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetRow(
    row: GetMonthSummary.CategoryRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(row.colorArgb))
                    )
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    text = if (row.budgetCents != null) {
                        Money.format(row.netSpentCents) + " / " + Money.format(row.budgetCents)
                    } else {
                        Money.format(row.netSpentCents)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (row.budgetCents != null) {
                BudgetProgressBar(
                    progress = row.percent ?: 0f,
                    spent = Money.format(row.netSpentCents),
                    limit = Money.format(row.budgetCents!!)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.budget_transaction_count, row.transactionCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if ((row.remainingCents ?: 0) < 0) {
                            stringResource(R.string.budget_exceeded) + ": " +
                                Money.format(kotlin.math.abs(row.remainingCents!!))
                        } else {
                            stringResource(R.string.budget_remaining) + ": " +
                                Money.format(row.remainingCents ?: 0)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if ((row.remainingCents ?: 0) < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.budget_transaction_count, row.transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BudgetEditDialog(
    row: GetMonthSummary.CategoryRow,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var amountCents by remember(row.categoryId) { mutableStateOf(row.budgetCents ?: 0L) }

    CampusDialog(
        onDismissRequest = onDismiss,
        title = row.name,
        confirmText = stringResource(R.string.save),
        confirmEnabled = amountCents > 0,
        onConfirm = { if (amountCents > 0) onSave(amountCents) },
        content = {
            CampusTextField(
                value = Money.toLocalDecimalString(amountCents),
                onValueChange = { text ->
                    amountCents = text.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L
                },
                label = stringResource(R.string.budget_amount),
                prefix = "$",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (onDelete != null) {
                CampusButton(
                    text = stringResource(R.string.delete),
                    onClick = onDelete,
                    style = CampusButtonStyle.DESTRUCTIVE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
@Preview(name = "BudgetsScreen", showBackground = true)
fun PreviewBudgetsScreen() {
    com.example.campuspocket.core.designsystem.CampusTheme {
        androidx.compose.material3.Surface {
            BudgetsScreen()
        }
    }
}