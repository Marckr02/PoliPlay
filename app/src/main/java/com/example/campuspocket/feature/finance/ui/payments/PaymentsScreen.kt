package com.example.campuspocket.feature.finance.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.Frequency
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

private fun freqLabel(f: String) = when (f) {
    "WEEKLY" -> "Semanal"
    "YEARLY" -> "Anual"
    else -> "Mensual"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var payNow by remember { mutableStateOf<ScheduledPaymentEntity?>(null) }
    var deleting by remember { mutableStateOf<ScheduledPaymentEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.finance_scheduled)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPayment,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.scheduled_payment_name)) }
            )
        }
    ) { inner ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.payments.isEmpty() -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_scheduled_payments))
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.payments, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(Money.format(p.amountCents), style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "${freqLabel(p.frequency)} · ${stringResource(R.string.scheduled_payment_next_due)}: " +
                                    DATE_FMT.format(p.nextDueDate) +
                                    " · ${stringResource(R.string.scheduled_payment_remind_days, p.remindDaysBefore)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = p.active,
                                    onClick = { viewModel.toggleActive(p) },
                                    label = { Text(stringResource(if (p.active) R.string.yes else R.string.no)) }
                                )
                                TextButton(onClick = { payNow = p }) {
                                    Text(stringResource(R.string.scheduled_payment_mark_paid))
                                }
                                TextButton(onClick = { onEditPayment(p.id) }) {
                                    Text(stringResource(R.string.edit))
                                }
                                TextButton(onClick = { deleting = p }) {
                                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo "Marcar como pagado" con monto editable (estilo cajero).
    payNow?.let { payment ->
        var amountCents by remember(payment.id) { mutableStateOf(payment.amountCents) }
        AlertDialog(
            onDismissRequest = { payNow = null },
            title = { Text(payment.name) },
            text = {
                OutlinedTextField(
                    value = Money.toLocalDecimalString(amountCents),
                    onValueChange = { raw -> amountCents = raw.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L },
                    label = { Text(stringResource(R.string.transaction_amount)) },
                    prefix = { Text("$") },
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

    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(p.name) },
            text = { Text(stringResource(R.string.transaction_description)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(p.id) ; deleting = null },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
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
