package com.example.campuspocket.feature.finance.ui.summary

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.LocalPrivacyMode
import com.example.campuspocket.core.designsystem.MoneyText
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.domain.MonthPace
import kotlin.math.max

/**
 * "Ritmo del gasto" (v5 §2.5): gráfico de líneas esperado vs. gastado vs.
 * proyección, con guías al 50 % y 100 %, leyenda con trazos diferenciados y
 * descripción textual para TalkBack. Los montos respetan el modo privado.
 */
@Composable
internal fun SpendingPaceSection(
    pace: MonthPace,
    dailySpentCents: List<Long>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Text(
                text = stringResource(R.string.summary_pace_chart_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(14.dp))
            SpendingPaceChart(
                pace = pace,
                dailySpentCents = dailySpentCents,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
            Spacer(Modifier.height(12.dp))
            ChartLegend()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaceMiniCard(
                label = stringResource(R.string.summary_stat_spent),
                valueCents = pace.spentCents,
                modifier = Modifier.weight(1f)
            )
            PaceMiniCard(
                label = stringResource(R.string.summary_stat_expected),
                valueCents = pace.expectedCents,
                modifier = Modifier.weight(1f)
            )
            PaceMiniCard(
                label = stringResource(R.string.summary_stat_closing),
                valueCents = pace.estimatedClosingCents.takeIf { pace.hasEnoughDataForClosing },
                fallback = stringResource(R.string.finance_insight_not_enough_data),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SpendingPaceChart(
    pace: MonthPace,
    dailySpentCents: List<Long>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val expectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current

    val guide50 = stringResource(R.string.summary_chart_guide_50)
    val guide100 = stringResource(R.string.summary_chart_guide_100)
    val todayLabel = stringResource(R.string.summary_chart_today)
    val hidden = LocalPrivacyMode.current
    val chartDescription = if (hidden) {
        stringResource(R.string.summary_chart_accessibility_hidden)
    } else {
        val closing = if (pace.hasEnoughDataForClosing) {
            Money.format(pace.estimatedClosingCents)
        } else {
            stringResource(R.string.finance_insight_not_enough_data)
        }
        stringResource(
            R.string.summary_chart_accessibility,
            Money.format(pace.spentCents),
            Money.format(pace.expectedCents),
            closing
        )
    }

    val labelPaint = remember(expectedColor, density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = expectedColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    val spentSeries = if (dailySpentCents.size >= pace.daysInMonth) {
        dailySpentCents
    } else {
        List(pace.daysInMonth) { index ->
            if (index == pace.day - 1) pace.spentCents else 0L
        }
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = chartDescription }
    ) {
        val top = 16.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val left = 2.dp.toPx()
        val rightLabel = 42.dp.toPx()
        val plotRight = (size.width - rightLabel).coerceAtLeast(left + 1f)
        val plotWidth = plotRight - left
        val plotHeight = (bottom - top).coerceAtLeast(1f)

        val maxValue = max(
            max(pace.budgetCents, pace.estimatedClosingCents),
            max(pace.spentCents, 1L)
        ).toFloat()

        fun xFor(day: Int): Float =
            left + plotWidth * (day.toFloat() / pace.daysInMonth.toFloat())

        fun yFor(cents: Long): Float {
            val safe = cents.coerceIn(0L, maxValue.toLong()).toFloat()
            return bottom - plotHeight * (safe / maxValue)
        }

        val expectedDash = PathEffect.dashPathEffect(
            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
            0f
        )
        val projectionDash = PathEffect.dashPathEffect(
            floatArrayOf(2.dp.toPx(), 7.dp.toPx()),
            0f
        )
        val guideColor = expectedColor.copy(alpha = 0.24f)

        // Guías horizontales al 50 % y 100 % del presupuesto.
        listOf(0.5f to guide50, 1f to guide100).forEach { (fraction, label) ->
            val y = yFor((pace.budgetCents * fraction).toLong())
            drawLine(
                color = guideColor,
                start = Offset(left, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                plotRight + 6.dp.toPx(),
                y + labelPaint.textSize / 3f,
                labelPaint
            )
        }

        // Esperado: recta discontinua (día 0, $0) → (último día, presupuesto).
        drawPath(
            path = Path().apply {
                moveTo(xFor(0), yFor(0))
                lineTo(xFor(pace.daysInMonth), yFor(pace.budgetCents))
            },
            color = expectedColor.copy(alpha = 0.55f),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = expectedDash)
        )

        // Gastado hasta hoy: trazo continuo de 3 dp + relleno al 14 %.
        val spentPath = Path().apply {
            moveTo(xFor(0), yFor(0))
            for (day in 1..pace.day) {
                lineTo(xFor(day), yFor(spentSeries.getOrElse(day - 1) { 0L }))
            }
        }
        val fillPath = Path().apply {
            moveTo(xFor(0), yFor(0))
            for (day in 1..pace.day) {
                lineTo(xFor(day), yFor(spentSeries.getOrElse(day - 1) { 0L }))
            }
            lineTo(xFor(pace.day), bottom)
            lineTo(xFor(0), bottom)
            close()
        }
        drawPath(fillPath, primary.copy(alpha = 0.14f))
        drawPath(
            path = spentPath,
            color = primary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Proyección: del punto de hoy al cierre estimado, punteada al 60 %.
        val todayX = xFor(pace.day)
        val spentToday = spentSeries.getOrElse(pace.day - 1) { pace.spentCents }
        drawPath(
            path = Path().apply {
                moveTo(todayX, yFor(spentToday))
                lineTo(xFor(pace.daysInMonth), yFor(pace.estimatedClosingCents))
            },
            color = primary.copy(alpha = 0.6f),
            style = Stroke(width = 2.dp.toPx(), pathEffect = projectionDash)
        )

        // Marca vertical de "Hoy" y punto sobre la curva con borde surface.
        drawLine(
            color = expectedColor.copy(alpha = 0.42f),
            start = Offset(todayX, top),
            end = Offset(todayX, bottom),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(2.dp.toPx(), 5.dp.toPx()),
                0f
            )
        )
        val todayY = yFor(spentToday)
        drawCircle(surface, radius = 6.dp.toPx(), center = Offset(todayX, todayY))
        drawCircle(primary, radius = 4.dp.toPx(), center = Offset(todayX, todayY))

        val todayWidth = labelPaint.measureText(todayLabel)
        val todayTextX = (todayX - todayWidth / 2f)
            .coerceIn(left, (plotRight - todayWidth).coerceAtLeast(left))
        drawContext.canvas.nativeCanvas.drawText(
            todayLabel,
            todayTextX,
            top + labelPaint.textSize,
            labelPaint
        )
    }
}

@Composable
private fun ChartLegend() {
    val primary = MaterialTheme.colorScheme.primary
    val expected = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        LegendItem(
            label = stringResource(R.string.summary_chart_legend_spent),
            color = primary
        )
        LegendItem(
            label = stringResource(R.string.summary_chart_legend_expected),
            color = expected,
            dashed = true
        )
        LegendItem(
            label = stringResource(R.string.summary_chart_legend_projection),
            color = primary.copy(alpha = 0.6f),
            dotted = true
        )
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    dashed: Boolean = false,
    dotted: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width(22.dp)
                .height(8.dp)
        ) {
            val effect = when {
                dotted -> PathEffect.dashPathEffect(
                    floatArrayOf(2.dp.toPx(), 4.dp.toPx()),
                    0f
                )
                dashed -> PathEffect.dashPathEffect(
                    floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                    0f
                )
                else -> null
            }
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = effect
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PaceMiniCard(
    label: String,
    valueCents: Long?,
    modifier: Modifier = Modifier,
    fallback: String? = null
) {
    BentoCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        if (valueCents != null) {
            MoneyText(
                amountCents = valueCents,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = fallback.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
