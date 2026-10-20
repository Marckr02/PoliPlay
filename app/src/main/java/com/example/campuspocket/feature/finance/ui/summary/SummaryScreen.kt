package com.example.campuspocket.feature.finance.ui.summary

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.components.BudgetProgressBar
import com.example.campuspocket.core.designsystem.components.CategoryExpenseChart
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.MonthlyOverviewCard
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.MonthPace
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    modifier: Modifier = Modifier,
    onDefineBudget: () -> Unit = {},
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingShimmer(modifier = modifier.fillMaxSize())
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "available_card") {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.summary_available),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.format(state.availableCents),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (state.availableCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        CategoryExpenseChart(
                            slices = state.donutSlices,
                            modifier = Modifier.size(140.dp)
                        )
                    }
                }
            }

            item(key = "overview_card") {
                MonthlyOverviewCard(
                    modifier = Modifier.fillMaxWidth(),
                    income = max(state.totalBalanceCents, 0L),
                    expense = min(state.projectedSpentCents, 0L),
                    net = state.projectedBalanceCents
                )
            }

            item(key = "pace_card") {
                val pace = state.pace
                if (pace != null) {
                    BudgetPaceCard(pace = pace, onEditBudget = onDefineBudget)
                } else {
                    BudgetPaceSuggestionCard(onDefineBudget = onDefineBudget)
                }
            }

            if (state.upcomingPayments.isNotEmpty()) {
                item(key = "payments_card") {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.summary_upcoming_payments),
                                style = MaterialTheme.typography.titleSmall
                            )
                            state.upcomingPayments.forEach { p ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        p.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        Money.format(p.amountCents) + " · " + p.nextDueDate.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "top_categories_header") {
                Text(
                    text = stringResource(R.string.summary_top_categories),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            if (state.topCategories.isEmpty()) {
                item(key = "empty_categories") {
                    EmptyStateCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.empty_budgets),
                        message = stringResource(R.string.empty_budgets),
                        icon = painterResource(R.drawable.ic_wallet)
                    )
                }
            } else {
                items(state.topCategories, key = { it.categoryId }) { row ->
                    CategoryBudgetRow(row)
                }
            }
        }
    }
}

/**
 * Indicador de ritmo del presupuesto total (Fase 8B): esperado vs. real, con
 * marca del día actual sobre la barra. El relleno se limita al 100 %, pero el
 * porcentaje real se muestra completo.
 */
@Composable
private fun BudgetPaceCard(
    pace: MonthPace,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = if (pace.isOver) MaterialTheme.colorScheme.error else MaterialTheme.extended.success
    val monthName = pace.date.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pace_card_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(
                            R.string.pace_month_day,
                            monthName,
                            pace.day,
                            pace.daysInMonth
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onEditBudget) {
                    Text(stringResource(R.string.budget_total_edit))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PaceStat(
                    label = stringResource(R.string.pace_expected),
                    percent = formatPacePercent(pace.expectedPct),
                    amount = Money.format(pace.expectedCents),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                PaceStat(
                    label = stringResource(R.string.pace_real),
                    percent = formatPacePercent(pace.realPct),
                    amount = Money.format(pace.spentCents),
                    valueColor = statusColor,
                    modifier = Modifier.weight(1f)
                )
            }

            PaceBar(pace = pace, statusColor = statusColor)

            Text(
                text = if (pace.isOver) {
                    stringResource(R.string.pace_above, Money.format(pace.overCents))
                } else {
                    stringResource(R.string.pace_below)
                },
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
    }
}

@Composable
private fun PaceStat(
    label: String,
    percent: String,
    amount: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = percent, style = MaterialTheme.typography.titleMedium, color = valueColor)
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PaceBar(pace: MonthPace, statusColor: Color, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)

        if (pace.barProgress > 0f) {
            drawRoundRect(
                color = statusColor,
                size = Size(size.width * pace.barProgress, size.height),
                cornerRadius = radius
            )
        }

        // Marca vertical del ritmo esperado (día actual / días del mes).
        val markerX = (size.width * pace.expectedFraction)
            .coerceIn(3.dp.toPx(), size.width - 3.dp.toPx())
        drawLine(
            color = markerColor,
            start = Offset(markerX, 0f),
            end = Offset(markerX, size.height),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun BudgetPaceSuggestionCard(onDefineBudget: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.pace_no_budget_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.pace_no_budget_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onDefineBudget,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(stringResource(R.string.budget_total_define))
            }
        }
    }
}

private fun formatPacePercent(value: java.math.BigDecimal): String =
    value.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " %"

@Composable
private fun CategoryBudgetRow(row: GetMonthSummary.CategoryRow, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(row.colorArgb))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(row.name, style = MaterialTheme.typography.titleSmall)
                }
                Text(Money.format(row.netSpentCents), style = MaterialTheme.typography.bodyMedium)
            }
            row.budgetCents?.let { budget ->
                val pct = row.percent ?: 0f
                BudgetProgressBar(
                    progress = pct,
                    spent = Money.format(row.netSpentCents),
                    limit = Money.format(budget)
                )
            }
        }
    }
}

@Composable
@Preview(name = "SummaryScreen", showBackground = true)
fun PreviewSummaryScreen() {
    CampusTheme {
        Surface {
            SummaryScreen()
        }
    }
}