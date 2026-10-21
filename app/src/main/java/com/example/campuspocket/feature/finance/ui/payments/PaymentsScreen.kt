package com.example.campuspocket.feature.finance.ui.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusChip
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.LoadingShimmer
import com.example.campuspocket.core.designsystem.components.PrimaryPillButton
import com.example.campuspocket.core.designsystem.components.RoundIconButton
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

private fun freqLabelRes(f: String): Int = when (f) {
    "WEEKLY" -> R.string.scheduled_payment_frequency_weekly
    "YEARLY" -> R.string.scheduled_payment_frequency_yearly
    else -> R.string.scheduled_payment_frequency_monthly
}

/**
 * Pagos programados dentro de la pestaña Pagos: lista sin FAB, con calendario
 * y creación en la cabecera. La lógica del ViewModel se conserva intacta.
 */
@Composable
fun PaymentsScreen(
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = MaterialTheme.extended
    var payNow by remember { mutableStateOf<ScheduledPaymentEntity?>(null) }
    var deleting by remember { mutableStateOf<ScheduledPaymentEntity?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.finance_scheduled),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (state.payments.size == 1) {
                        stringResource(R.string.payments_count_one)
                    } else {
                        stringResource(R.string.payments_count, state.payments.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(
                    icon = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.finance_open_calendar),
                    onClick = onOpenCalendar,
                    size = 44.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                RoundIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.scheduled_payment_new),
                    onClick = onAddPayment,
                    size = 44.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingShimmer()
            }

            state.payments.isEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item(key = "empty") {
                        BentoCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(ext.lavender),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_schedule),
                                        contentDescription = null,
                                        tint = ext.onLavender,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.empty_scheduled_payments),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.empty_scheduled_payments_message),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            PrimaryPillButton(
                                text = stringResource(R.string.scheduled_payment_new),
                                onClick = onAddPayment,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.payments, key = { it.id }) { payment ->
                    PaymentCard(
                        payment = payment,
                        onToggleActive = { viewModel.toggleActive(payment) },
                        onMarkPaid = { payNow = payment },
                        onEdit = { onEditPayment(payment.id) },
                        onDelete = { deleting = payment }
                    )
                }
            }
        }
    }

    payNow?.let { payment ->
        var amountCents by remember(payment.id) { mutableStateOf(payment.amountCents) }
        AlertDialog(
            onDismissRequest = { payNow = null },
            title = { Text(payment.name) },
            text = {
                CampusTextField(
                    value = Money.toLocalDecimalString(amountCents),
                    onValueChange = { raw ->
                        amountCents = raw.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L
                    },
                    label = stringResource(R.string.transaction_amount),
                    prefix = "$",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (amountCents > 0) {
                            viewModel.markPaid(payment, amountCents, LocalDate.now())
                            payNow = null
                        }
                    },
                    enabled = amountCents > 0
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { payNow = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    deleting?.let { payment ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(payment.name) },
            text = { Text(stringResource(R.string.scheduled_payment_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(payment.id)
                        deleting = null
                    }
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun PaymentCard(
    payment: ScheduledPaymentEntity,
    onToggleActive: () -> Unit,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = MaterialTheme.extended
    BentoCard(
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = payment.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = Money.format(payment.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(freqLabelRes(payment.frequency)) + " · " +
                stringResource(R.string.scheduled_payment_next_due) + ": " +
                DATE_FMT.format(payment.nextDueDate) + " · " +
                stringResource(R.string.scheduled_payment_remind_days, payment.remindDaysBefore),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CampusChip(
                label = stringResource(
                    if (payment.active) R.string.scheduled_payment_active
                    else R.string.scheduled_payment_paused
                ),
                selected = payment.active,
                onClick = onToggleActive
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onMarkPaid) {
                Text(stringResource(R.string.scheduled_payment_mark_paid))
            }
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.edit))
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

data class PaymentsUiState(val payments: List<ScheduledPaymentEntity> = emptyList(), val isLoading: Boolean = true)

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val uiState: StateFlow<PaymentsUiState> = financeRepository.observeAllScheduledPayments()
        .map { list -> PaymentsUiState(list, false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PaymentsUiState())

    fun toggleActive(p: ScheduledPaymentEntity) {
        viewModelScope.launch {
            financeRepository.updateScheduledPayment(p.copy(active = !p.active))
        }
    }

    fun markPaid(p: ScheduledPaymentEntity, amountCents: Long, date: LocalDate) {
        viewModelScope.launch {
            financeRepository.markScheduledPaymentPaid(p, amountCents, date)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            financeRepository.deleteScheduledPayment(id)
        }
    }
}
