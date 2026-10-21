package com.example.campuspocket.feature.finance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.components.AccountsCarousel
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CategoryExpenseChart
import com.example.campuspocket.core.designsystem.components.PillSegmentedControl
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.ui.accounts.AccountsViewModel
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsScreen
import com.example.campuspocket.feature.finance.ui.history.HistoryScreen
import com.example.campuspocket.feature.finance.ui.payments.PaymentsScreen
import com.example.campuspocket.feature.finance.ui.summary.SummaryUiState
import com.example.campuspocket.feature.finance.ui.summary.SummaryViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class FinanceTab { MOVEMENTS, BUDGETS, PAYMENTS }

/**
 * Finanzas: resumen y cuentas en la cabecera, cuatro acciones rápidas y tres
 * pestañas. Sin barra de pestañas Material ni FAB: la creación vive en el dock contextual
 * y en las acciones de esta pantalla.
 */
@Composable
fun FinanceHomeScreen(
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onManageCategories: () -> Unit,
    onOpenTransaction: (Long, TransactionType) -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onWithdrawal: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    summaryViewModel: SummaryViewModel = hiltViewModel(),
    accountsViewModel: AccountsViewModel = hiltViewModel()
) {
    val summary by summaryViewModel.uiState.collectAsStateWithLifecycle()
    val accounts by accountsViewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(FinanceTab.MOVEMENTS.name) }
    var summaryExpanded by rememberSaveable { mutableStateOf(false) }
    val currentTab = FinanceTab.entries.firstOrNull { it.name == tab } ?: FinanceTab.MOVEMENTS
    val ext = MaterialTheme.extended
    val monthLabel = YearMonth.now()
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es")))
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("es")) }

    var headerHeightPx by remember { mutableIntStateOf(0) }
    var headerCollapsePx by remember { mutableFloatStateOf(0f) }

    // Presupuestos y Pagos necesitan toda la altura; Movimientos conserva el
    // resumen a la vista. El usuario puede volver a desplegarlo deslizando.
    LaunchedEffect(currentTab, headerHeightPx > 0) {
        if (headerHeightPx > 0) {
            headerCollapsePx = if (currentTab == FinanceTab.MOVEMENTS) {
                0f
            } else {
                -headerHeightPx.toFloat()
            }
        }
    }

    val headerScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val limit = headerHeightPx.toFloat()
                if (available.y >= 0f || limit <= 0f) return Offset.Zero
                val target = (headerCollapsePx + available.y).coerceAtLeast(-limit)
                val consumed = target - headerCollapsePx
                if (consumed == 0f) return Offset.Zero
                headerCollapsePx = target
                return Offset(0f, consumed)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y <= 0f || headerCollapsePx >= 0f) return Offset.Zero
                val target = (headerCollapsePx + available.y).coerceAtMost(0f)
                val consumedY = target - headerCollapsePx
                if (consumedY == 0f) return Offset.Zero
                headerCollapsePx = target
                return Offset(0f, consumedY)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().nestedScroll(headerScrollConnection)) {
        CollapsibleHeader(
            collapsedPx = headerCollapsePx,
            onHeightChanged = { measured ->
                if (headerHeightPx != measured) {
                    headerHeightPx = measured
                    if (headerCollapsePx < -measured) {
                        headerCollapsePx = -measured.toFloat()
                    }
                }
            },
            onVerticalDrag = { dragAmount ->
                headerCollapsePx = (headerCollapsePx + dragAmount)
                    .coerceIn(-headerHeightPx.toFloat(), 0f)
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.finance_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(ext.track)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = ext.onTrack
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.finance_balance_total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Money.format(summary.totalBalanceCents),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BalanceChip(
                            iconRes = R.drawable.ic_arrow_down,
                            amountText = "+" + Money.format(summary.incomeCents),
                            contentDescription = stringResource(
                                R.string.finance_income_amount,
                                Money.format(summary.incomeCents)
                            ),
                            container = ext.mint,
                            content = ext.onMint
                        )
                        BalanceChip(
                            iconRes = R.drawable.ic_arrow_up,
                            amountText = "−" + Money.format(summary.expenseCents),
                            contentDescription = stringResource(
                                R.string.finance_expense_amount,
                                Money.format(summary.expenseCents)
                            ),
                            container = ext.peach,
                            content = ext.onPeach
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.finance_accounts),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    RoundIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.finance_add_account),
                        onClick = onAddAccount,
                        size = 48.dp,
                        containerColor = ext.track,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (accounts.balances.isEmpty()) {
                    BentoCard(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.finance_no_accounts),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        PrimaryPillButton(
                            text = stringResource(R.string.finance_add_account),
                            onClick = onAddAccount
                        )
                    }
                } else {
                    AccountsCarousel(
                        modifier = Modifier.padding(start = 20.dp),
                        accounts = accounts.balances,
                        onAccountClick = { onOpenAccount(it.account.id ?: 0L) }
                    )
                }

                Spacer(Modifier.height(14.dp))
                FinanceSummaryRow(
                    summary = summary,
                    expanded = summaryExpanded,
                    onToggle = { summaryExpanded = !summaryExpanded },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                if (summaryExpanded) {
                    Spacer(Modifier.height(10.dp))
                    FinanceSummaryCard(
                        summary = summary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickFinanceAction(
                        iconRes = R.drawable.ic_arrow_up,
                        label = stringResource(R.string.finance_quick_expense),
                        container = ext.peach,
                        content = ext.onPeach,
                        onClick = { onAddTransaction(TransactionType.EXPENSE) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickFinanceAction(
                        iconRes = R.drawable.ic_arrow_down,
                        label = stringResource(R.string.finance_quick_income),
                        container = ext.mint,
                        content = ext.onMint,
                        onClick = { onAddTransaction(TransactionType.INCOME) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickFinanceAction(
                        iconRes = R.drawable.ic_swap,
                        label = stringResource(R.string.finance_quick_transfer),
                        container = ext.sky,
                        content = ext.onSky,
                        onClick = { onAddTransaction(TransactionType.TRANSFER) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickFinanceAction(
                        iconRes = R.drawable.ic_wallet,
                        label = stringResource(R.string.finance_quick_withdraw),
                        container = ext.lavender,
                        content = ext.onLavender,
                        onClick = onWithdrawal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        PillSegmentedControl(
            options = FinanceTab.entries,
            selected = currentTab,
            onSelect = { tab = it.name },
            label = { option ->
                stringResource(
                    when (option) {
                        FinanceTab.MOVEMENTS -> R.string.finance_tab_movements
                        FinanceTab.BUDGETS -> R.string.finance_tab_budgets
                        FinanceTab.PAYMENTS -> R.string.finance_tab_payments
                    }
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp)
        ) {
            when (currentTab) {
                FinanceTab.MOVEMENTS -> HistoryScreen(
                    onOpenTransaction = onOpenTransaction,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                )
                FinanceTab.BUDGETS -> BudgetsScreen(onManageCategories = onManageCategories)
                FinanceTab.PAYMENTS -> PaymentsScreen(
                    onAddPayment = onAddPayment,
                    onEditPayment = onEditPayment,
                    onOpenCalendar = onOpenCalendar
                )
            }
        }
    }
}

/**
 * Cabecera que se recoge al hacer scroll en la pestaña activa. Así el resumen
 * sigue estando a la vista al entrar y las listas recuperan toda la altura útil
 * en cuanto el usuario desliza.
 */
@Composable
private fun CollapsibleHeader(
    collapsedPx: Float,
    onHeightChanged: (Int) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    content: @Composable () -> Unit
) {
    SubcomposeLayout(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    onVerticalDrag(dragAmount)
                }
            }
    ) { constraints ->
        val header = subcompose("finance_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { onHeightChanged(it.height) }
            ) {
                content()
            }
        }.first().measure(Constraints(maxWidth = constraints.maxWidth))
        val visibleHeight = (header.height + collapsedPx)
            .roundToInt()
            .coerceIn(0, header.height)
        layout(constraints.maxWidth, visibleHeight) {
            header.place(0, visibleHeight - header.height)
        }
    }
}

/** Fila compacta que mantiene el resumen accesible sin comerse la altura de las pestañas. */
@Composable
private fun FinanceSummaryRow(
    summary: SummaryUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    BentoCard(
        modifier = modifier,
        onClick = onToggle,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 13.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.finance_month_summary),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = Money.format(summary.availableCents),
                style = MaterialTheme.typography.titleSmall,
                color = if (summary.availableCents < 0) ext.danger else ext.success
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(
                    if (expanded) R.string.finance_summary_hide
                    else R.string.finance_summary_show
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Resumen del mes: categorías con más gasto, próximos pagos y proyección. */
@Composable
private fun FinanceSummaryCard(
    summary: SummaryUiState,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryExpenseChart(
                slices = summary.donutSlices,
                modifier = Modifier.size(78.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.summary_top_categories),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (summary.topCategories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_budgets),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    summary.topCategories.take(3).forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(Color(category.colorArgb), CircleShape)
                            )
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Money.format(category.netSpentCents),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryStat(
                label = stringResource(R.string.finance_projected_balance),
                value = Money.format(summary.projectedBalanceCents),
                modifier = Modifier.weight(1f)
            )
            SummaryStat(
                label = stringResource(R.string.finance_projected_expense),
                value = Money.format(summary.projectedSpentCents),
                modifier = Modifier.weight(1f)
            )
        }

        if (summary.upcomingPayments.isNotEmpty()) {
            Text(
                text = stringResource(R.string.finance_upcoming_payments),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
            summary.upcomingPayments.take(3).forEach { payment ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = payment.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = dateFormatter.format(payment.nextDueDate) + " · " + Money.format(payment.amountCents),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BalanceChip(
    iconRes: Int,
    amountText: String,
    contentDescription: String,
    container: Color,
    content: Color
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(iconRes),
            contentDescription = null,
            tint = content,
            modifier = Modifier.width(14.dp).height(14.dp)
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.labelSmall,
            color = content
        )
    }
}

@Composable
private fun QuickFinanceAction(
    iconRes: Int,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoundIconButton(
            iconRes = iconRes,
            contentDescription = label,
            onClick = onClick,
            size = 56.dp,
            containerColor = container,
            contentColor = content
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}