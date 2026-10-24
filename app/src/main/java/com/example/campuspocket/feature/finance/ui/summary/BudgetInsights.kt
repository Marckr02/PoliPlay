package com.example.campuspocket.feature.finance.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.MoneyText
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.formatMoneyOrHidden
import com.example.campuspocket.feature.finance.domain.MonthPace

/**
 * Fila de dos insights bajo el hero del ritmo (v5 §2.3). Solo se muestra
 * cuando hay presupuesto total del mes. El cierre es peach cuando la
 * extrapolación termina por encima del presupuesto.
 */
@Composable
internal fun BudgetInsightsRow(
    pace: MonthPace,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val cardShape = RoundedCornerShape(24.dp)
        BentoCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = cardShape,
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = stringResource(R.string.finance_insight_per_day_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            MoneyText(
                amountCents = pace.perDayCents,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (pace.isBudgetExhausted) {
                    stringResource(R.string.finance_insight_budget_exhausted)
                } else {
                    pluralStringResource(
                        R.plurals.finance_insight_days_remaining,
                        pace.daysRemaining,
                        pace.daysRemaining
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val closingReliable = pace.hasEnoughDataForClosing
        val closingAlert = closingReliable && pace.isClosingOverBudget
        val ext = MaterialTheme.extended
        BentoCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = cardShape,
            containerColor = if (closingAlert) ext.peach else MaterialTheme.colorScheme.surface,
            contentColor = if (closingAlert) ext.onPeach else MaterialTheme.colorScheme.onSurface,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.finance_insight_closing_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (closingAlert) {
                        ext.onPeach.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )
                if (closingAlert) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.finance_insight_closing_over_budget),
                        tint = ext.onPeach,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (closingReliable) {
                MoneyText(
                    amountCents = pace.estimatedClosingCents,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (closingAlert) ext.onPeach else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            } else {
                Text(
                    text = stringResource(R.string.finance_insight_not_enough_data),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (closingReliable) {
                    stringResource(
                        R.string.finance_insight_closing_of,
                        formatMoneyOrHidden(pace.budgetCents)
                    )
                } else {
                    stringResource(R.string.finance_insight_not_enough_data)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (closingAlert) {
                    ext.onPeach.copy(alpha = 0.82f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
