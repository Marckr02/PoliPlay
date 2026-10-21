package com.example.campuspocket.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.PillShape
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
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

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
    val ext = MaterialTheme.extended
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = ext.ink,
            contentColor = ext.onInk
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
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
                    color = ext.lime
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.onInk
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.headlineMedium,
                color = ext.onInk
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = ext.onInkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onInkMuted
                    )
                }
                location?.let { loc ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = ext.onInkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = loc,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.onInkMuted
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(PillShape)
                    .background(ext.onInk.copy(alpha = 0.18f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(PillShape)
                        .background(ext.lime)
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    PriorityTag(priority = priority, modifier = modifier)
}

@Composable
fun BudgetProgressBar(
    modifier: Modifier = Modifier,
    progress: Float,
    spent: String,
    limit: String
) {
    val ext = MaterialTheme.extended
    val color = when {
        progress > 1f -> MaterialTheme.colorScheme.error
        progress >= 0.9f -> ext.warning
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "$spent / $limit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(PillShape)
                .background(ext.track)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(PillShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier, height: Dp = 16.dp, width: Dp = Dp.Unspecified) {
    Box(
        modifier = modifier
            .height(height)
            .width(width)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    )
}

@Composable
fun LoadingShimmer(modifier: Modifier = Modifier, itemCount: Int = 5) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(itemCount) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
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
    val ext = MaterialTheme.extended
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
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
                    .background(ext.lavender),
                contentAlignment = Alignment.Center
            ) {
                when (icon) {
                    is Int -> Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = ext.onLavender
                    )
                    is Painter -> Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = ext.onLavender
                    )
                    is ImageVector -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = ext.onLavender
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
                PrimaryPillButton(text = actionText, onClick = onAction)
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
    CampusChip(label = label, selected = selected, onClick = onClick, modifier = modifier)
}

@Composable
fun KeypadAmountDisplay(amount: String, modifier: Modifier = Modifier) {
    Text(
        text = amount,
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().padding(24.dp).wrapContentSize(Alignment.CenterEnd),
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelProvider: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    PillSegmentedControl(
        options = options,
        selected = selected,
        onSelect = onSelect,
        label = labelProvider,
        modifier = modifier
    )
}

internal val Priority.resourceString: Int
    get() = when (this) {
        Priority.HIGH -> R.string.task_priority_high
        Priority.MEDIUM -> R.string.task_priority_medium
        Priority.LOW -> R.string.task_priority_low
    }

@Composable
fun WeeklyScheduleGrid(
    modifier: Modifier = Modifier,
    days: List<DaySchedule>,
    onClassClick: (WeekEntry) -> Unit,
    timeRange: IntRange = 7..21
) {
    val hours = (timeRange.first..timeRange.last).toList()
    val dayLabels = days.map { it.day }
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            ) {
                dayLabels.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(day.shortLabelRes()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                hours.forEach { hour ->
                    TimeSlotRow(hour = hour, days = days, onClassClick = onClassClick)
                }
            }
        }
    }
}

@Composable
private fun TimeSlotRow(hour: Int, days: List<DaySchedule>, onClassClick: (WeekEntry) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = String.format("%02d:00", hour),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp).padding(top = 4.dp).fillMaxHeight(),
            textAlign = TextAlign.End
        )
        days.forEach { daySchedule ->
            val sessionsInHour = daySchedule.entries.filter { entry ->
                val startHour = entry.session.startTime.hour
                val endHour = entry.session.endTime.hour
                hour in startHour..endHour - 1
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().fillMaxWidth()) {
                if (sessionsInHour.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        sessionsInHour.forEach { entry ->
                            ClassBlock(entry = entry, onClick = { onClassClick(entry) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassBlock(entry: WeekEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val courseColor = Color(entry.courseColorArgb)
    val container = courseColor.copy(alpha = 0.14f)
        .compositeOver(MaterialTheme.colorScheme.surface)
    Card(
        modifier = modifier.fillMaxWidth().fillMaxHeight().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(courseColor)
            )
            Column(modifier = Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.courseName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.session_time_range,
                        TimeText.format(entry.session.startTime),
                        TimeText.format(entry.session.endTime)
                    ),
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
fun TaskFilterRow(
    modifier: Modifier = Modifier,
    selectedFilter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TaskFilter.entries.toTypedArray()) { filter ->
            CampusChip(
                label = stringResource(filter.label),
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) }
            )
        }
    }
}

enum class TaskFilter(val label: Int) {
    ALL(R.string.task_filter_all),
    TODAY(R.string.academic_today),
    THIS_WEEK(R.string.task_filter_week),
    BY_COURSE(R.string.task_course),
    COMPLETED(R.string.task_filter_completed)
}

@Composable
fun TaskItemCard(
    modifier: Modifier = Modifier,
    task: Task,
    courseName: String?,
    onClick: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSwipeToComplete: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    val ext = MaterialTheme.extended
    val isCompleted = task.completedAt != null
    BentoCard(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = if (isCompleted) ext.successContainer else MaterialTheme.colorScheme.surface,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundCheckbox(
                checked = isCompleted,
                onCheckedChange = onToggleCompleted,
                contentDescription = task.title
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDueAt(task) + (courseName?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    PriorityBadge(priority = task.priority)
                }
            }
        }
    }
}

@Composable
private fun RoundCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String
) {
    CampusCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription
    )
}

private fun formatDueAt(task: Task): String {
    val formatter = DateTimeFormatter.ofPattern("EEE d 'de' MMM · HH:mm", Locale.forLanguageTag("es"))
    return formatter.format(task.dueAt.atZone(ZoneId.systemDefault()))
}

@Composable
fun AccountsCarousel(
    modifier: Modifier = Modifier,
    accounts: List<AccountBalance>,
    onAccountClick: (AccountBalance) -> Unit
) {
    val ext = MaterialTheme.extended
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 20.dp)
    ) {
        items(accounts, key = { it.account.id ?: 0L }) { balance ->
            val index = accounts.indexOf(balance)
            val container = if (index % 2 == 0) ext.ink else ext.accent
            val content = if (index % 2 == 0) ext.onInk else ext.onAccent
            WalletCard(
                name = balance.account.name,
                typeLabel = stringResource(accountTypeLabelRes(balance.account.type)),
                amount = Money.format(balance.balanceCents),
                containerColor = container,
                contentColor = content,
                onClick = { onAccountClick(balance) }
            )
        }
    }
}

internal fun accountTypeLabelRes(type: AccountType): Int = when (type) {
    AccountType.CASH -> R.string.account_type_cash
    AccountType.DEBIT -> R.string.account_type_debit
    AccountType.SAVINGS -> R.string.account_type_savings
    AccountType.CREDIT_CARD -> R.string.account_type_credit
}

@Composable
fun MonthlyOverviewCard(modifier: Modifier = Modifier, income: Long, expense: Long, net: Long) {
    val ext = MaterialTheme.extended
    val total = (income + abs(expense)).toFloat()
    val incomePercent = if (total > 0) income.toFloat() / total else 0f
    val expensePercent = if (total > 0) abs(expense).toFloat() / total else 0f
    BentoCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.finance_dashboard),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.finance_income),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.success
                    )
                    Text(
                        text = Money.format(income),
                        style = MaterialTheme.typography.titleMedium,
                        color = ext.success
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.finance_expense),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = Money.format(expense),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.summary_available),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (net >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = Money.format(net),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (net >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(PillShape)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(incomePercent.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(ext.mint)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(expensePercent.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(ext.peach)
                )
            }
        }
    }
}

@Composable
fun CategoryExpenseChart(
    slices: List<Pair<Int, Long>>,
    modifier: Modifier = Modifier
) {
    val track = MaterialTheme.extended.track
    Canvas(modifier = modifier) {
        val total = slices.sumOf { it.second }.toFloat().coerceAtLeast(1f)
        var startAngle = -90f
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 24f)
        )
        slices.forEach { (colorArgb, value) ->
            val sweepAngle = (value.toFloat() / total) * 360f
            drawArc(
                color = Color(colorArgb),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun TransactionTypeSelector(
    modifier: Modifier = Modifier,
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    val types = listOf(
        TransactionType.EXPENSE,
        TransactionType.INCOME,
        TransactionType.TRANSFER,
        TransactionType.REFUND
    )
    PillSegmentedControl(
        options = types,
        selected = selectedType,
        onSelect = onTypeChange,
        label = { stringResource(transactionTypeLabelRes(it)) },
        modifier = modifier
    )
}

internal fun transactionTypeLabelRes(type: TransactionType): Int = when (type) {
    TransactionType.EXPENSE -> R.string.finance_expense
    TransactionType.INCOME -> R.string.finance_income
    TransactionType.TRANSFER -> R.string.finance_transfer
    TransactionType.REFUND -> R.string.finance_refund
}

@Composable
fun FolderChipsRow(
    modifier: Modifier = Modifier,
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    onFolderClick: (Long?) -> Unit,
    onAddFolder: (() -> Unit)? = null
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            CampusChip(
                label = stringResource(R.string.notes_all),
                selected = selectedFolderId == null,
                onClick = { onFolderClick(null) }
            )
        }
        items(folders, key = { "f${it.id}" }) { folder ->
            CampusChip(
                label = folder.name,
                selected = selectedFolderId == folder.id,
                onClick = { onFolderClick(folder.id) }
            )
        }
        if (onAddFolder != null) {
            item(key = "add") {
                CampusChip(
                    label = "+ " + stringResource(R.string.notes_add_folder),
                    selected = false,
                    onClick = onAddFolder
                )
            }
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
    val ext = MaterialTheme.extended
    val (container, content) = notePalette(note.id)
    PastelNoteCard(
        note = note,
        containerColor = container,
        contentColor = content,
        onClick = onClick,
        onPinToggle = onPinToggle,
        onMove = onMove,
        onDelete = onDelete,
        modifier = modifier
    )
}

@Composable
private fun notePalette(id: Long): Pair<Color, Color> {
    val ext = MaterialTheme.extended
    return when (id.mod(4)) {
        0 -> ext.lavender to ext.onLavender
        1 -> ext.limeSoft to ext.onLimeSoft
        2 -> ext.peach to ext.onPeach
        else -> ext.sky to ext.onSky
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
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
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
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        actions.forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.extended.track)
                    .clickable {
                        val result = action(currentContent, selectionStart, selectionEnd)
                        onContentChange(result.text)
                        onSelectionChange(result.selectionStart, result.selectionEnd)
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extended.onTrack
                )
            }
        }
    }
}

@Composable
fun EditorModeTab(
    showPreview: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    RoundIconButton(
        icon = if (showPreview) Icons.Filled.DateRange else Icons.Filled.Check,
        contentDescription = if (showPreview) {
            stringResource(R.string.markdown_edit)
        } else {
            stringResource(R.string.markdown_preview)
        },
        onClick = onToggle,
        modifier = modifier
    )
}

@Composable
@Preview(name = "TaskFilterRow", showBackground = true)
fun PreviewTaskFilterRow() {
    CampusTheme { Surface { TaskFilterRow(selectedFilter = TaskFilter.ALL, onFilterChange = {}) } }
}

@Composable
@Preview(name = "TransactionTypeSelector", showBackground = true)
fun PreviewTransactionTypeSelector() {
    CampusTheme {
        Surface {
            TransactionTypeSelector(selectedType = TransactionType.EXPENSE, onTypeChange = {})
        }
    }
}

@Composable
@Preview(name = "FolderChipsRow", showBackground = true)
fun PreviewFolderChipsRow() {
    CampusTheme {
        Surface {
            FolderChipsRow(folders = emptyList(), selectedFolderId = null, onFolderClick = {})
        }
    }
}

@Composable
@Preview(name = "NoteCard", showBackground = true)
fun PreviewNoteCard() {
    CampusTheme {
        Surface {
            NoteCard(
                note = NoteEntity(
                    id = 1,
                    title = "Sample Note",
                    content = "This is a sample note content that shows how the card looks.",
                    folderId = null,
                    courseId = null,
                    taskId = null,
                    pinned = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                onClick = {},
                onPinToggle = {},
                onDelete = {},
                onMove = {}
            )
        }
    }
}
