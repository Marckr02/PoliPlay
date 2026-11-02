package com.example.campuspocket.feature.finance.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusOverline
import com.example.campuspocket.core.designsystem.MoneyText
import com.example.campuspocket.core.designsystem.ShapeHero
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusTextButton
import com.example.campuspocket.core.designsystem.components.PaceGauge
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.formatMoneyOrHidden
import com.example.campuspocket.core.designsystem.ShapeCard
import com.example.campuspocket.core.util.CampusFormats
import com.example.campuspocket.feature.finance.domain.MonthPace
import java.time.format.TextStyle
import java.util.Locale

/**
 * Héroe de ritmo de Finanzas (v8 §7): medidor semicircular, leyenda y estado.
 * Es el único lugar, junto con la tarjeta compacta de Inicio, donde se usa
 * `PaceGauge`; Resumen y Presupuestos conservan su tarjeta con barra.
 */
@Composable
internal fun PaceGaugeCard(
    pace: MonthPace,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val dark = ext.isDark
    val monthName = pace.date.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val exhausted = pace.isBudgetExhausted
    val over = pace.isOver
    val fillColor = when {
        exhausted || over -> ext.gaugeAlert
        dark -> ext.gaugeFill
        else -> ext.mint
    }
    val percent = CampusFormats.percentValue(pace.realPct)
    val expectedPercent = CampusFormats.percentValue(pace.expectedPct)
    val accessibility = stringResource(
        R.string.finance_pace_accessibility,
        "$percent %",
        "$expectedPercent %"
    )
    val statusText = when {
        exhausted -> stringResource(R.string.finance_gauge_exhausted)
        over -> stringResource(
            R.string.finance_gauge_above_status,
            formatMoneyOrHidden(pace.overCents)
        )
        else -> stringResource(
            R.string.finance_gauge_below_status,
            formatMoneyOrHidden(pace.expectedCents - pace.spentCents)
        )
    }

    BentoCard(
        modifier = modifier,
        containerColor = ext.ink,
        contentColor = ext.onInk,
        shape = ShapeHero,
        contentPadding = PaddingValues(20.dp)
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
                style = CampusOverline,
                color = ext.lime,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            CampusTextButton(
                text = stringResource(R.string.budget_total_edit),
                onClick = onEditBudget,
                contentColor = ext.lime
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PaceGauge(
                spentFraction = pace.barProgress,
                expectedFraction = pace.expectedFraction,
                percentLabel = percent,
                contentDescription = accessibility,
                fillColor = fillColor,
                caption = stringResource(R.string.finance_gauge_spent_caption),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            PaceLegend(
                label = stringResource(R.string.finance_pace_spent_label),
                amountCents = pace.spentCents,
                modifier = Modifier.weight(1f)
            )
            PaceLegend(
                label = stringResource(R.string.finance_pace_expected_label),
                amountCents = pace.expectedCents,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp)
                .background(
                    color = ext.onInk.copy(alpha = 0.08f),
                    shape = ShapeCard
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (over || exhausted) Icons.Filled.Warning else Icons.Filled.Check,
                contentDescription = null, // decorativo: el texto contiguo describe el estado
                tint = if (over || exhausted) ext.onInk else ext.lime,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = ext.onInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaceLegend(
    label: String,
    amountCents: Long,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = CampusOverline,
            color = ext.onInk.copy(alpha = 0.55f)
        )
        Spacer(Modifier.height(2.dp))
        MoneyText(
            amountCents = amountCents,
            style = MaterialTheme.typography.titleMedium,
            color = ext.onInk,
            maxLines = 1
        )
    }
}
