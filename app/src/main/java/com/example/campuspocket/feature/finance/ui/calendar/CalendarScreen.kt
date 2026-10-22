package com.example.campuspocket.feature.finance.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.util.Money
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Calendario mensual: balance estimado por día debajo de la fecha (spec 7.6). */
@Composable
fun CalendarScreen(
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    embedded: Boolean = false,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = Locale.forLanguageTag("es")
    val weekdayLabels = DayOfWeek.entries.map { day ->
        day.getDisplayName(TextStyle.SHORT, locale)
            .take(2)
            .replaceFirstChar { it.uppercase(locale) }
    }
    val monthLabel = state.month.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) } + " " + state.month.year

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (embedded) 0.dp else 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = if (embedded) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!embedded) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (embedded) monthLabel else stringResource(R.string.finance_calendar),
                    style = if (embedded) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (embedded) {
                        stringResource(R.string.finance_calendar)
                    } else {
                        monthLabel
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.calendar_previous_month),
                onClick = viewModel::previousMonth,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.calendar_next_month),
                onClick = viewModel::nextMonth,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.days, key = { it.date.toString() }) { day ->
                DayCell(
                    day = day,
                    isToday = day.date == LocalDate.now(),
                    onClick = if (embedded) null else ({ onDaySelected(day.date) }),
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: DayCellState,
    isToday: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val negative = day.balanceCents < 0
    val container = if (isToday) ext.accent else MaterialTheme.colorScheme.surface
    val content = if (isToday) ext.onAccent else MaterialTheme.colorScheme.onSurface
    BentoCard(
        modifier = modifier.aspectRatio(0.85f),
        containerColor = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = content
            )
            Text(
                text = if (negative) "-" + Money.format(-day.balanceCents) else "+" + Money.format(day.balanceCents),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isToday -> content.copy(alpha = 0.86f)
                    negative -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
        }
    }
}
