package com.example.campuspocket.feature.finance.ui.summary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.LocalMotion
import com.example.campuspocket.core.designsystem.MoneyText
import com.example.campuspocket.core.designsystem.MotionTokens
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.formatMoneyOrHidden
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.BudgetProgressBar
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.CategoryExpenseChart
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.MonthlyOverviewCard
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.MonthPace
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

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
                item(key = "spending_chart") {
                    val pace = state.pace
                    if (pace != null) {
                        SpendingPaceSection(
                            pace = pace,
                            dailySpentCents = state.dailySpentCents
                        )
                    }
                }

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
                            if (remainingCents != null) {
                                MoneyText(
                                    amountCents = remainingCents,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (remainingCents < 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            } else {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                                state.upcomingPayments.take(3).forEach { p ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            p.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            formatMoneyOrHidden(p.amountCents) + " · " + p.nextDueDate.toString(),
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
    val motion = LocalMotion.current
    val monthName = pace.date.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val overBudget = pace.spentCents > pace.budgetCents
    val targetStatusContainer = if (pace.isOver) ext.dangerSoft else ext.mint
    val targetStatusContent = if (pace.isOver) ext.onDangerSoft else ext.onMint
    val targetStatusColor = if (pace.isOver) ext.dangerSoft else ext.mint
    val statusText = if (pace.isOver) {
        stringResource(R.string.finance_pace_above_badge)
    } else {
        stringResource(R.string.finance_pace_below_badge)
    }
    val phrase = when {
        overBudget -> stringResource(
            R.string.finance_pace_over_budget_message,
            formatMoneyOrHidden(pace.spentCents - pace.budgetCents)
        )
        pace.isOver -> stringResource(
            R.string.finance_pace_above_message,
            formatMoneyOrHidden(pace.overCents)
        )
        else -> stringResource(
            R.string.finance_pace_below_message,
            formatMoneyOrHidden(pace.expectedCents - pace.spentCents)
        )
    }

    // Animación de entrada (v5 §10.2 #1): la barra crece 600 ms, la marca del
    // esperado cae con resorte y el porcentaje cuenta. Se guarda el último valor
    // para que, al volver a Finanzas tras guardar, crezca desde el valor previo.
    val previousBar = rememberSaveable(pace.date.year, pace.date.monthValue) {
        mutableFloatStateOf(0f)
    }
    val animatedBar = remember(pace.date) { Animatable(previousBar.floatValue) }
    val previousPct = rememberSaveable(pace.date.year, pace.date.monthValue) {
        mutableFloatStateOf(0f)
    }
    val animatedPct = remember(pace.date) { Animatable(previousPct.floatValue) }
    val previousExpected = rememberSaveable(pace.date.year, pace.date.monthValue) {
        mutableFloatStateOf(0f)
    }
    val animatedExpected = remember(pace.date) { Animatable(previousExpected.floatValue) }

    LaunchedEffect(pace.barProgress, motion.enabled) {
        if (motion.enabled) {
            animatedBar.animateTo(
                targetValue = pace.barProgress,
                animationSpec = motion.tween(600, MotionTokens.EmphasizedDecelerate)
            )
        } else {
            animatedBar.snapTo(pace.barProgress)
        }
        previousBar.floatValue = animatedBar.value
    }
    LaunchedEffect(pace.realPct, motion.enabled) {
        val target = pace.realPct.toFloat()
        if (motion.enabled) {
            animatedPct.animateTo(
                targetValue = target,
                animationSpec = motion.tween(600, MotionTokens.EmphasizedDecelerate)
            )
        } else {
            animatedPct.snapTo(target)
        }
        previousPct.floatValue = animatedPct.value
    }
    LaunchedEffect(pace.expectedFraction, motion.enabled) {
        if (motion.enabled) {
            delay(200L)
            animatedExpected.animateTo(
                targetValue = pace.expectedFraction,
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            animatedExpected.snapTo(pace.expectedFraction)
        }
        previousExpected.floatValue = animatedExpected.value
    }

    val statusColor by animateColorAsState(
        targetValue = targetStatusColor,
        animationSpec = motion.tween(250),
        label = "paceBarColor"
    )
    val statusContainer by animateColorAsState(
        targetValue = targetStatusContainer,
        animationSpec = motion.tween(250),
        label = "paceStatusContainer"
    )
    val statusContent by animateColorAsState(
        targetValue = targetStatusContent,
        animationSpec = motion.tween(250),
        label = "paceStatusContent"
    )
    val accessibilityText = stringResource(
        R.string.finance_pace_accessibility,
        formatPacePercent(pace.realPct),
        formatPacePercent(pace.expectedPct)
    )

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
                formatPacePercent(animatedPct.value),
                stringResource(R.string.finance_pace_spent_word)
            ),
            style = MaterialTheme.typography.displaySmall,
            color = ext.onInk,
            maxLines = 2,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = accessibilityText
            }
        )
        Spacer(Modifier.height(14.dp))
        PaceBar(
            progress = animatedBar.value,
            expectedFraction = animatedExpected.value,
            trackColor = ext.onInk.copy(alpha = 0.18f),
            fillColor = statusColor,
            markerColor = ext.onInk
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PaceStat(
                label = stringResource(R.string.finance_pace_spent_label),
                amountCents = pace.spentCents,
                valueColor = ext.onInk,
                modifier = Modifier.weight(1f)
            )
            PaceStat(
                label = stringResource(R.string.finance_pace_expected_label),
                amountCents = pace.expectedCents,
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
                AnimatedContent(
                    targetState = statusText,
                    transitionSpec = {
                        (fadeIn(motion.tween(200)) +
                            scaleIn(
                                initialScale = 0.85f,
                                animationSpec = motion.tween(200)
                            )) togetherWith
                            (fadeOut(motion.tween(120)) +
                                scaleOut(
                                    targetScale = 0.95f,
                                    animationSpec = motion.tween(120)
                                ))
                    },
                    label = "paceStatus"
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusContent
                    )
                }
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
    amountCents: Long,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor.copy(alpha = 0.72f)
        )
        MoneyText(
            amountCents = amountCents,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor
        )
    }
}

@Composable
private fun PaceBar(
    progress: Float,
    expectedFraction: Float,
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

        if (progress > 0f) {
            drawRoundRect(
                color = fillColor,
                size = Size(size.width * progress, size.height),
                cornerRadius = radius
            )
        }

        // Marca vertical del ritmo esperado (día actual / días del mes).
        val markerX = (size.width * expectedFraction)
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

private fun formatPacePercent(value: BigDecimal): String =
    value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " %"

/** Formato del porcentaje animado, redondeado a un decimal half-up. */
private fun formatPacePercent(value: Float): String =
    BigDecimal.valueOf((value * 10f).roundToLong(), 1)
        .setScale(1, RoundingMode.HALF_UP)
        .toPlainString()
        .replace('.', ',') + " %"

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
                MoneyText(
                    amountCents = row.netSpentCents,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            row.budgetCents?.let { budget ->
                val pct = row.percent ?: 0f
                BudgetProgressBar(
                    progress = pct,
                    spent = formatMoneyOrHidden(row.netSpentCents),
                    limit = formatMoneyOrHidden(budget)
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