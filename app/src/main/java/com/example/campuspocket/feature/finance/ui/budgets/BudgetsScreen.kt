package com.example.campuspocket.feature.finance.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Colores de la barra: azul normal, ámbar al 80 %, roja al pasar 100 %. */
private val BudgetOk = Color(0xFF4C8DFF)
private val BudgetWarn = Color(0xFFF5A623)
private val BudgetOver = Color(0xFFE5484D)

private fun barColor(percent: Float?): Color = when {
    percent == null -> BudgetOk
    percent > 1f -> BudgetOver
    percent >= 0.8f -> BudgetWarn
    else -> BudgetOk
}

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingCategory by remember { mutableStateOf<GetMonthSummary.CategoryRow?>(null) }

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
            FilterChip(
                selected = !state.semesterMode,
                onClick = { if (state.semesterMode) viewModel.toggleSemesterMode() },
                label = { Text(stringResource(R.string.budget_month)) }
            )
            FilterChip(
                selected = state.semesterMode,
                onClick = { if (!state.semesterMode) viewModel.toggleSemesterMode() },
                label = { Text(stringResource(R.string.budget_semester)) }
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val withBudget = state.rows.filter { it.budgetCents != null }
                val withoutBudget = state.rows.filter { it.budgetCents == null }

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
                        Text(
                            text = stringResource(R.string.empty_budgets),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                item(key = "copiar") {
                    TextButton(
                        onClick = { viewModel.copyFromPreviousMonth() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.budget_copy_previous))
                    }
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
}

@Composable
private fun BudgetRow(
    row: GetMonthSummary.CategoryRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                // Barra de progreso con color por umbral
                val pct = row.percent ?: 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct.coerceAtMost(1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor(row.percent))
                    )
                }
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
                        color = if ((row.remainingCents ?: 0) < 0) BudgetOver else MaterialTheme.colorScheme.onSurfaceVariant
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.name) },
        text = {
            OutlinedTextField(
                value = Money.toLocalDecimalString(amountCents),
                onValueChange = { text ->
                    amountCents = text.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L
                },
                label = { Text(stringResource(R.string.budget_amount)) },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (amountCents > 0) onSave(amountCents) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
