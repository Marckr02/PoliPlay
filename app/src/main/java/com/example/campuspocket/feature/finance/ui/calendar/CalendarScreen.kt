package com.example.campuspocket.feature.finance.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.util.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Calendario mensual: balance estimado por día debajo de la fecha (spec 7.6). */
@Composable
fun CalendarScreen(
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
            }
            Text(
                text = state.month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                    .replaceFirstChar { it.uppercase() } + " " + state.month.year,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }

        // Cabecera Lun-Dom
        Row(Modifier.fillMaxWidth()) {
            listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do").forEach { d ->
                Text(
                    d,
                    Modifier.weight(1f),
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
                    onClick = { onDaySelected(day.date) },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val negative = day.balanceCents < 0
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.85f),
        colors = if (isToday) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (negative) "-" + Money.format(-day.balanceCents) else "+" + Money.format(day.balanceCents),
                style = MaterialTheme.typography.labelSmall,
                color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
