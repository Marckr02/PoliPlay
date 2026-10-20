package com.example.campuspocket.core.designsystem.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.academic.domain.model.Priority
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.ui.TimeText
import com.example.campuspocket.feature.academic.ui.shortLabelRes
import com.example.campuspocket.feature.academic.ui.week.DaySchedule
import com.example.campuspocket.feature.academic.ui.week.WeekEntry
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.AccountBalance
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.MarkdownActions
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    timeRange: String,
    location: String?,
    progress: Float,
    onClick: () -> Unit
) {
    val heroContainer = MaterialTheme.extended.heroContainer
    val onHero = MaterialTheme.extended.onHeroContainer
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = heroContainer,
            contentColor = onHero
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = onHero.copy(alpha = 0.75f)
                )
                Box(modifier = Modifier.height(48.dp).width(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = progress,
                        strokeWidth = 4.dp,
                        color = onHero,
                        trackColor = onHero.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                        color = onHero
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.headlineMedium,
                color = onHero
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = onHero.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHero.copy(alpha = 0.85f)
                    )
                }
                location?.let { loc ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = onHero.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = loc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onHero.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val (backgroundColor, textColor) = when (priority) {
        Priority.HIGH -> Pair(ext.errorContainer, MaterialTheme.colorScheme.error)
        Priority.MEDIUM -> Pair(ext.warningContainer, ext.warning)
        Priority.LOW -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = stringResource(priority.resourceString), style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@Composable
fun BudgetProgressBar(
    modifier: Modifier = Modifier,
    progress: Float,
    spent: String,
    limit: String
) {
    val color = when {
        progress > 1f -> MaterialTheme.colorScheme.error
        progress >= 0.9f -> MaterialTheme.extended.warning
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "$spent / $limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = color)
        }
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            progress = progress.coerceIn(0f, 1f),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier, height: Dp = 16.dp, width: Dp = Dp.Unspecified) {
    Box(modifier = modifier.height(height).width(width).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)))
}

@Composable
fun LoadingShimmer(modifier: Modifier = Modifier, itemCount: Int = 5) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(itemCount) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ShimmerPlaceholder(width = 44.dp, height = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                ShimmerPlaceholder(width = 120.dp, height = 18.dp)
                                Spacer(Modifier.height(8.dp))
                                ShimmerPlaceholder(width = 80.dp, height = 14.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                when (icon) {
                    is Int -> Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    is Painter -> Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    is ImageVector -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = actionText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(150)
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150)
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, color = contentColor, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
            labelColor = contentColor,
            selectedLabelColor = contentColor
        )
    )
}

@Composable
fun KeypadAmountDisplay(amount: String, modifier: Modifier = Modifier) {
    Text(text = amount, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface, modifier = modifier.fillMaxWidth().padding(24.dp).wrapContentSize(Alignment.CenterEnd), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelProvider: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(text = labelProvider(option))
            }
        }
    }
}

internal val Priority.resourceString: Int
    get() = when (this) {
        Priority.HIGH -> R.string.task_priority_high
        Priority.MEDIUM -> R.string.task_priority_medium
        Priority.LOW -> R.string.task_priority_low
    }

@Composable
fun WeeklyScheduleGrid(modifier: Modifier = Modifier, days: List<DaySchedule>, onClassClick: (WeekEntry) -> Unit, timeRange: IntRange = 7..21) {
    val hours = (timeRange.first..timeRange.last).toList()
    val dayLabels = days.map { it.day }
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
                dayLabels.forEach { day ->
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(day.shortLabelRes()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                hours.forEach { hour -> TimeSlotRow(hour = hour, days = days, onClassClick = onClassClick) }
            }
        }
    }
}

@Composable
private fun TimeSlotRow(hour: Int, days: List<DaySchedule>, onClassClick: (WeekEntry) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = String.format("%02d:00", hour), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp).padding(top = 4.dp).fillMaxHeight(), textAlign = TextAlign.End)
        days.forEach { daySchedule ->
            val sessionsInHour = daySchedule.entries.filter { entry -> val startHour = entry.session.startTime.hour; val endHour = entry.session.endTime.hour; hour in startHour..endHour - 1 }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().fillMaxWidth()) {
                if (sessionsInHour.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        sessionsInHour.forEach { entry -> ClassBlock(entry = entry, onClick = { onClassClick(entry) }) }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassBlock(entry: WeekEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val courseColor = Color(entry.courseColorArgb)
    Card(
        modifier = modifier.fillMaxWidth().fillMaxHeight().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, courseColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(courseColor)
            )
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.courseName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.session_time_range, TimeText.format(entry.session.startTime), TimeText.format(entry.session.endTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                entry.session.room?.takeIf { it.isNotBlank() }?.let { room ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = room,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskFilterRow(modifier: Modifier = Modifier, selectedFilter: TaskFilter, onFilterChange: (TaskFilter) -> Unit) {
    LazyRow(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TaskFilter.entries.toTypedArray()) { filter ->
            val selected = selectedFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(filter) },
                label = { Text(text = stringResource(filter.label), style = MaterialTheme.typography.labelLarge) },
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

enum class TaskFilter(val label: Int) {
    ALL(R.string.filter_all_courses), TODAY(R.string.academic_due_today), THIS_WEEK(R.string.academic_due_tomorrow), BY_COURSE(R.string.task_course)
}

@Composable
fun TaskItemCard(modifier: Modifier = Modifier, task: Task, courseName: String?, onClick: () -> Unit, onToggleCompleted: (Boolean) -> Unit, onDelete: () -> Unit, onSwipeToComplete: () -> Unit, onSwipeToDelete: () -> Unit) {
    val isCompleted = task.completedAt != null
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isCompleted, onCheckedChange = { onToggleCompleted(it) }, modifier = Modifier.padding(end = 10.dp))
            Column(Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textDecoration = if (isCompleted) TextDecoration.LineThrough else null)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formatDueAt(task) + (courseName?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PriorityBadge(priority = task.priority)
                }
            }
        }
    }
}

private fun formatDueAt(task: Task): String {
    val formatter = DateTimeFormatter.ofPattern("EEE d 'de' MMM · HH:mm", Locale.forLanguageTag("es"))
    return formatter.format(task.dueAt.atZone(ZoneId.systemDefault()))
}

@Composable
@Preview(name = "TaskFilterRow", showBackground = true) fun PreviewTaskFilterRow() {
    CampusTheme { Surface { TaskFilterRow(selectedFilter = TaskFilter.ALL, onFilterChange = {}) } }
}

@Composable
fun AccountsCarousel(modifier: Modifier = Modifier, accounts: List<AccountBalance>, onAccountClick: (AccountBalance) -> Unit) {
    LazyRow(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(accounts, key = { it.account.id ?: 0L }) { balance -> AccountCarouselCard(balance = balance, onClick = { onAccountClick(balance) }) } }
}

@Composable
private fun AccountCarouselCard(balance: AccountBalance, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val account = balance.account
    val accountColor = Color(account.colorArgb)
    val heroContainer = MaterialTheme.extended.heroContainer
    val onHero = MaterialTheme.extended.onHeroContainer
    Card(
        modifier = modifier.width(280.dp).height(160.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = heroContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column { Text(text = account.name, style = MaterialTheme.typography.titleLarge, color = onHero); Text(text = stringResource(accountTypeLabelRes(account.type)), style = MaterialTheme.typography.labelSmall, color = onHero.copy(alpha = 0.75f)) }
                    Box(modifier = Modifier.size(36.dp).background(accountColor, CircleShape)) { Text(text = account.name.first().uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(Alignment.Center)) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column { Text(text = stringResource(R.string.account_current_balance), style = MaterialTheme.typography.labelSmall, color = onHero.copy(alpha = 0.75f)); Text(text = Money.format(balance.balanceCents), style = MaterialTheme.typography.headlineMedium, color = onHero) }
                }
            }
        }
    }
}

internal fun accountTypeLabelRes(type: AccountType): Int = when (type) { AccountType.CASH -> R.string.account_type_cash; AccountType.DEBIT -> R.string.account_type_debit; AccountType.SAVINGS -> R.string.account_type_savings; AccountType.CREDIT_CARD -> R.string.account_type_credit }

@Composable
fun MonthlyOverviewCard(modifier: Modifier = Modifier, income: Long, expense: Long, net: Long) {
    val total = (income + abs(expense)).toFloat()
    val incomePercent = if (total > 0) income.toFloat() / total else 0f
    val expensePercent = if (total > 0) abs(expense).toFloat() / total else 0f
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.finance_dashboard), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(text = stringResource(R.string.finance_income), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.extended.success); Text(text = Money.format(income), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.extended.success) }
                Column(Modifier.weight(1f)) { Text(text = stringResource(R.string.finance_expense), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error); Text(text = Money.format(expense), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error) }
                Column(Modifier.weight(1f)) { Text(text = stringResource(R.string.summary_available), style = MaterialTheme.typography.labelMedium, color = if (net >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error); Text(text = Money.format(net), style = MaterialTheme.typography.headlineSmall, color = if (net >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
            }
            Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)) {
                Box(modifier = Modifier.fillMaxWidth(incomePercent.coerceIn(0f, 1f)).height(6.dp).background(MaterialTheme.extended.success))
                Box(modifier = Modifier.fillMaxWidth(expensePercent.coerceIn(0f, 1f)).height(6.dp).background(MaterialTheme.colorScheme.error))
            }
        }
    }
}

@Composable
fun CategoryExpenseChart(
    slices: List<Pair<Int, Long>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val total = slices.sumOf { it.second }.toFloat().coerceAtLeast(1f)
        var startAngle = -90f
        slices.forEach { (colorArgb, value) ->
            val sweepAngle = (value.toFloat() / total) * 360f
            drawArc(
                color = Color(colorArgb),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24f)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun TransactionTypeSegmentedButton(modifier: Modifier = Modifier, selectedType: TransactionType, onTypeChange: (TransactionType) -> Unit) {
    val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER, TransactionType.REFUND)
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) { types.forEachIndexed { index, type -> SegmentedButton(selected = selectedType == type, onClick = { onTypeChange(type) }, shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size), modifier = Modifier.weight(1f)) { Text(text = stringResource(transactionTypeLabelRes(type)), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }
}

internal fun transactionTypeLabelRes(type: TransactionType): Int = when (type) { TransactionType.EXPENSE -> R.string.finance_expense; TransactionType.INCOME -> R.string.finance_income; TransactionType.TRANSFER -> R.string.finance_transfer; TransactionType.REFUND -> R.string.finance_refund }

@Composable
fun KeypadAmountInput(modifier: Modifier = Modifier, amount: String, onAmountChange: (String) -> Unit, error: Boolean = false, label: String = "") {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label.isNotBlank()) { Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(text = if (amount.isEmpty()) "0" else amount, style = MaterialTheme.typography.displayMedium, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(24.dp).wrapContentSize(Alignment.CenterEnd), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫").forEach { key -> Button(onClick = { when (key) { "⌫" -> { if (amount.isNotEmpty()) onAmountChange(amount.dropLast(1)) } "." -> { if (!amount.contains(".")) onAmountChange(if (amount.isEmpty()) "0." else amount + ".") } else -> { if (amount != "0" || key == ".") onAmountChange(if (amount == "0") key else amount + key) } } }, modifier = Modifier.weight(1f).aspectRatio(1f).fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Text(text = key, style = MaterialTheme.typography.titleLarge) } } }
    }
}

@Composable
@Preview(name = "TransactionTypeSegmentedButton", showBackground = true) fun PreviewTransactionTypeSegmentedButton() {
    CampusTheme { Surface { TransactionTypeSegmentedButton(selectedType = TransactionType.EXPENSE, onTypeChange = {}) } }
}

@Composable
@Preview(name = "KeypadAmountInput", showBackground = true) fun PreviewKeypadAmountInput() {
    CampusTheme { Surface { KeypadAmountInput(amount = "123.45", onAmountChange = {}, label = stringResource(R.string.transaction_amount)) } }
}

@Composable
fun FolderChipsRow(
    modifier: Modifier = Modifier,
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    onFolderClick: (Long?) -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { onFolderClick(null) },
                label = { Text(text = stringResource(R.string.notes_list), style = MaterialTheme.typography.labelLarge) },
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        items(folders, key = { "f${it.id}" }) { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onFolderClick(folder.id) },
                label = { Text(text = folder.name, style = MaterialTheme.typography.labelLarge) },
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasContent = note.content.isNotBlank()
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.notes_empty_title) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    if (hasContent) {
                        Text(
                            text = note.content.take(120).replace('\n', ' '),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.note_pin),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onPinToggle(!note.pinned) }) {
                    Icon(
                        imageVector = if (note.pinned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (note.pinned) stringResource(R.string.note_unpin) else stringResource(R.string.note_pin),
                        tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onMove) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.note_move_to_folder)
                    )
                }
            }
        }
    }
}

@Composable
fun StaggeredNotesGrid(
    notes: List<NoteEntity>,
    onNoteClick: (Long) -> Unit,
    onPinToggle: (Long, Boolean) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onMove: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(notes.size, key = { notes[it].id }) { index ->
            val note = notes[index]
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onPinToggle = { pinned -> onPinToggle(note.id, pinned) },
                onDelete = { onDelete(note) },
                onMove = { onMove(note) }
            )
        }
    }
}

@Composable
@Preview(name = "FolderChipsRow", showBackground = true) fun PreviewFolderChipsRow() {
    CampusTheme { Surface { FolderChipsRow(folders = emptyList(), selectedFolderId = null, onFolderClick = {}) } }
}

@Composable
@Preview(name = "NoteCard", showBackground = true) fun PreviewNoteCard() {
    CampusTheme { Surface { NoteCard(note = NoteEntity(id = 1, title = "Sample Note", content = "This is a sample note content that shows how the card looks.", folderId = null, courseId = null, taskId = null, pinned = false, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()), onClick = {}, onPinToggle = {}, onDelete = {}, onMove = {}) } }
}

@Composable
fun MarkdownToolbar(
    modifier: Modifier = Modifier,
    onAction: ((String, Int, Int) -> MarkdownActions.EditResult)? = null,
    currentContent: String,
    selectionStart: Int,
    selectionEnd: Int,
    onContentChange: (String) -> Unit,
    onSelectionChange: (Int, Int) -> Unit
) {
    val actions = listOf(
        "H" to { t: String, s: Int, e: Int -> MarkdownActions.prefixLines(t, s, e, "#") },
        "B" to { t: String, s: Int, e: Int -> MarkdownActions.wrap(t, s, e, "**") },
        "I" to { t: String, s: Int, e: Int -> MarkdownActions.wrap(t, s, e, "*") },
        "•" to { t: String, s: Int, e: Int -> MarkdownActions.prefixLines(t, s, e, "-") },
        "1." to { t: String, s: Int, e: Int -> MarkdownActions.prefixLines(t, s, e, "1.") },
        "☐" to { t: String, s: Int, e: Int -> MarkdownActions.prefixLines(t, s, e, "- [ ]") },
        "</>" to { t: String, s: Int, e: Int -> MarkdownActions.wrap(t, s, e, "`") },
        "```" to { t: String, s: Int, e: Int ->
            val r = MarkdownActions.wrap(t, s, e, "```\n")
            MarkdownActions.wrap(r.text, r.selectionStart, r.selectionEnd, "\n```")
        }
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        actions.forEach { (label, action) ->
            FilterChip(
                selected = false,
                onClick = {
                    val result = action(currentContent, selectionStart, selectionEnd)
                    onContentChange(result.text)
                    onSelectionChange(result.selectionStart, result.selectionEnd)
                },
                label = { Text(label) },
                modifier = Modifier.padding(end = 4.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun EditorModeTab(
    showPreview: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (showPreview) Icons.Filled.Edit else Icons.Filled.Search,
            contentDescription = if (showPreview) stringResource(R.string.markdown_edit) else stringResource(R.string.markdown_preview)
        )
    }
}