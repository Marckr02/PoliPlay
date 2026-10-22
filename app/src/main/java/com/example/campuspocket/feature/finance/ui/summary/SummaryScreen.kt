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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.BudgetProgressBar
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.CategoryExpenseChart
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.MonthlyOverviewCard
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
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
    onBack: (() -> Unit)? = null,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(R.string.summary_title),
            onBack = onBack
        )
        if (state.isLoading) {
            LoadingShimmer(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "available_card") {
                    BentoCard(Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.finance_remaining_summary),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val remainingCents = state.monthlyRemainingCents
                            Text(
                                text = remainingCents?.let { Money.format(it) } ?: "—",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if ((remainingCents ?: 0L) < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
                        BentoCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
}

/**
 * Indicador de ritmo del presupuesto total (Fase 8B): hero ink que lidera con
 * lo gastado, marca blanca del esperado y estado por debajo/por encima.
 */
@Composable
internal fun BudgetPaceCard(
    pace: MonthPace,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val monthName = pace.date.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val overBudget = pace.spentCents > pace.budgetCents
    val statusContainer = if (pace.isOver) ext.dangerSoft else ext.mint
    val statusContent = if (pace.isOver) ext.onDangerSoft else ext.onMint
    val statusColor = if (pace.isOver) ext.dangerSoft else ext.mint
    val statusText = if (pace.isOver) {
        stringResource(R.string.finance_pace_above_badge)
    } else {
        stringResource(R.string.finance_pace_below_badge)
    }
    val phrase = when {
        overBudget -> stringResource(
            R.string.finance_pace_over_budget_message,
            Money.format(pace.spentCents - pace.budgetCents)
        )
        pace.isOver -> stringResource(
            R.string.finance_pace_above_message,
            Money.format(pace.overCents)
        )
        else -> stringResource(
            R.string.finance_pace_below_message,
            Money.format(pace.expectedCents - pace.spentCents)
        )
    }

    BentoCard(
        modifier = modifier,
        containerColor = ext.ink,
        contentColor = ext.onInk,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.finance_pace_overline,
                    monthName.uppercase(Locale.getDefault()),
                    pace.day,
                    pace.daysInMonth
                ),
                style = MaterialTheme.typography.labelSmall,
                color = ext.onInk.copy(alpha = 0.72f),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onEditBudget,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = ext.lime
                )
            ) {
                Text(stringResource(R.string.budget_total_edit))
            }
        }
        Text(
            text = stringResource(
                R.string.pace_real_value,
                formatPacePercent(pace.realPct),
                stringResource(R.string.finance_pace_spent_word)
            ),
            style = MaterialTheme.typography.displaySmall,
            color = ext.onInk,
            maxLines = 2
        )
        Spacer(Modifier.height(14.dp))
        PaceBar(
            pace = pace,
            trackColor = ext.onInk.copy(alpha = 0.18f),
            fillColor = statusColor,
            markerColor = ext.onInk
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PaceStat(
                label = stringResource(R.string.finance_pace_spent_label),
                amount = Money.format(pace.spentCents),
                valueColor = ext.onInk,
                modifier = Modifier.weight(1f)
            )
            PaceStat(
                label = stringResource(R.string.finance_pace_expected_label),
                amount = Money.format(pace.expectedCents),
                valueColor = ext.onInk.copy(alpha = 0.72f),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(statusContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pace.isOver) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_up),
                        contentDescription = null,
                        tint = statusContent,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = statusContent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusContent
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = phrase,
            style = MaterialTheme.typography.bodySmall,
            color = ext.onInk.copy(alpha = 0.82f)
        )
    }
}

@Composable
private fun PaceStat(
    label: String,
    amount: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor.copy(alpha = 0.72f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor
        )
    }
}

@Composable
private fun PaceBar(
    pace: MonthPace,
    trackColor: Color,
    fillColor: Color,
    markerColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)

        if (pace.barProgress > 0f) {
            drawRoundRect(
                color = fillColor,
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
internal fun BudgetPaceSuggestionCard(onDefineBudget: () -> Unit, modifier: Modifier = Modifier) {
    BentoCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Text(
            text = stringResource(R.string.finance_pace_no_budget_title),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.finance_pace_no_budget_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        PrimaryPillButton(
            text = stringResource(R.string.finance_pace_create_budget),
            onClick = onDefineBudget,
            modifier = Modifier.align(Alignment.Start)
        )
    }
}

private fun formatPacePercent(value: java.math.BigDecimal): String =
    value.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " %"

@Composable
private fun CategoryBudgetRow(row: GetMonthSummary.CategoryRow, modifier: Modifier = Modifier) {
    BentoCard(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
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