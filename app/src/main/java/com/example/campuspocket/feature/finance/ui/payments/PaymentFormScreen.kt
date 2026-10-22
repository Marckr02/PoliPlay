package com.example.campuspocket.feature.finance.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSelectField
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.CampusSwitch
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.PillSegmentedControl
import com.example.campuspocket.core.designsystem.components.SecondaryPillButton
import com.example.campuspocket.core.designsystem.components.campusDatePickerColors
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.Frequency
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        CampusTopBar(
            title = stringResource(if (state.isEditing) R.string.edit else R.string.scheduled_payment_name),
            onBack = onDone
        )
        if (!state.isLoaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampusTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.scheduled_payment_name),
                supportingText = { if (state.nameError) Text(stringResource(R.string.error_required)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            CampusTextField(
                value = state.amountDisplay,
                onValueChange = viewModel::onAmountChange,
                label = stringResource(R.string.scheduled_payment_amount),
                prefix = "$",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Cuenta
            var accExpanded by remember { mutableStateOf(false) }
            CampusSelectField(
                value = accounts.firstOrNull { it.id == state.accountId }?.name ?: "",
                label = stringResource(R.string.transaction_account),
                expanded = accExpanded,
                onExpandedChange = { accExpanded = it },
                isError = state.error == PaymentDomainError.MISSING_ACCOUNT,
                supportingText = if (state.error == PaymentDomainError.MISSING_ACCOUNT) {
                    stringResource(R.string.transaction_error_missing_account)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                accounts.forEach { acc ->
                    CampusSelectItem(
                        text = acc.name,
                        selected = acc.id == state.accountId,
                        onClick = {
                            viewModel.onAccountChange(acc.id ?: 0L)
                            accExpanded = false
                        }
                    )
                }
            }

            // Categoría (gasto)
            var catExpanded by remember { mutableStateOf(false) }
            CampusSelectField(
                value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "",
                label = stringResource(R.string.transaction_category),
                expanded = catExpanded,
                onExpandedChange = { catExpanded = it },
                isError = state.error == PaymentDomainError.MISSING_CATEGORY,
                supportingText = if (state.error == PaymentDomainError.MISSING_CATEGORY) {
                    stringResource(R.string.transaction_error_missing_category)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { c ->
                    CampusSelectItem(
                        text = c.name,
                        selected = c.id == state.categoryId,
                        onClick = {
                            viewModel.onCategoryChange(c.id ?: 0L)
                            catExpanded = false
                        }
                    )
                }
            }

            // Frecuencia
            Column {
                Text(stringResource(R.string.scheduled_payment_frequency), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val selectedFrequency = Frequency.entries.firstOrNull { it.name == state.frequency }
                    ?: Frequency.MONTHLY
                PillSegmentedControl(
                    options = Frequency.entries.toList(),
                    selected = selectedFrequency,
                    onSelect = { viewModel.onFrequencyChange(it.name) },
                    label = { frequency ->
                        stringResource(
                            when (frequency) {
                                Frequency.WEEKLY -> R.string.scheduled_payment_frequency_weekly
                                Frequency.MONTHLY -> R.string.scheduled_payment_frequency_monthly
                                Frequency.YEARLY -> R.string.scheduled_payment_frequency_yearly
                            }
                        )
                    }
                )
            }

            // Próxima fecha de pago
            Column {
                Text(stringResource(R.string.scheduled_payment_next_due), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SecondaryPillButton(
                    text = DATE_FMT.format(state.nextDueDate),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Recordar N días antes
            Column {
                Text(
                    text = stringResource(R.string.scheduled_payment_remind_days, state.remindDaysBefore),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = state.remindDaysBefore.toFloat(),
                    onValueChange = { viewModel.onRemindDaysChange(it.toInt()) },
                    valueRange = 0f..14f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.scheduled_payment_active), style = MaterialTheme.typography.bodyLarge)
                CampusSwitch(
                    checked = state.active,
                    onCheckedChange = viewModel::onActiveChange,
                    contentDescription = stringResource(R.string.scheduled_payment_active)
                )
            }
        }

        CampusButton(
            text = stringResource(R.string.save),
            onClick = viewModel::save,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        CampusDialog(
            onDismissRequest = { showDatePicker = false },
            title = stringResource(R.string.scheduled_payment_next_due),
            confirmText = stringResource(R.string.ok),
            onConfirm = {
                pickerState.selectedDateMillis?.let { ms ->
                    viewModel.onNextDueDateChange(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate())
                }
                showDatePicker = false
            },
            content = { DatePicker(state = pickerState, colors = campusDatePickerColors()) }
        )
    }
}

enum class PaymentDomainError { MISSING_ACCOUNT, MISSING_CATEGORY }

data class PaymentFormUiState(
    val paymentId: Long? = null,
    val name: String = "",
    val amountCents: Long = 0,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val frequency: String = Frequency.MONTHLY.name,
    val nextDueDate: LocalDate = LocalDate.now().plusDays(7),
    val anchorDayOfMonth: Int = 0,
    val remindDaysBefore: Int = 3,
    val active: Boolean = true,
    val nameError: Boolean = false,
    val error: PaymentDomainError? = null,
    val isLoaded: Boolean = false,
    val saved: Boolean = false,
    val notFound: Boolean = false
) {
    val isEditing: Boolean get() = paymentId != null
    val amountDisplay: String get() = Money.toLocalDecimalString(amountCents)
}

@HiltViewModel
class PaymentFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>(FinanceDestinations.PAYMENT_ID_ARG)?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(PaymentFormUiState())
    val uiState: StateFlow<PaymentFormUiState> = _uiState

    val accounts = MutableStateFlow<List<Account>>(emptyList())
    val categories = MutableStateFlow<List<Category>>(emptyList())

    init {
        viewModelScope.launch {
            financeRepository.observeActiveAccounts().collect { accounts.value = it }
        }
        viewModelScope.launch {
            financeRepository.observeActiveCategories().collect { list ->
                categories.value = list.filter { it.kind == com.example.campuspocket.feature.finance.data.CategoryKind.EXPENSE }
            }
        }
        if (editingId != null) {
            viewModelScope.launch {
                val p = financeRepository.getScheduledPayment(editingId)
                _uiState.value = if (p == null) {
                    _uiState.value.copy(notFound = true, isLoaded = true)
                } else {
                    _uiState.value.copy(
                        paymentId = p.id,
                        name = p.name,
                        amountCents = p.amountCents,
                        accountId = p.accountId,
                        categoryId = p.categoryId,
                        frequency = p.frequency,
                        nextDueDate = p.nextDueDate,
                        anchorDayOfMonth = p.anchorDayOfMonth,
                        remindDaysBefore = p.remindDaysBefore,
                        active = p.active,
                        isLoaded = true
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoaded = true)
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = false) }
    fun onAmountChange(v: String) = _uiState.update { it.copy(amountCents = v.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L) }
    fun onAccountChange(v: Long) = _uiState.update { it.copy(accountId = v, error = null) }
    fun onCategoryChange(v: Long) = _uiState.update { it.copy(categoryId = v, error = null) }
    fun onFrequencyChange(v: String) = _uiState.update { it.copy(frequency = v) }
    fun onNextDueDateChange(v: LocalDate) = _uiState.update { it.copy(nextDueDate = v, anchorDayOfMonth = v.dayOfMonth) }
    fun onRemindDaysChange(v: Int) = _uiState.update { it.copy(remindDaysBefore = v) }
    fun onActiveChange(v: Boolean) = _uiState.update { it.copy(active = v) }

    fun save() {
        val s = _uiState.value
        if (s.name.isBlank()) { _uiState.update { it.copy(nameError = true) }; return }
        if (s.amountCents <= 0) { _uiState.update { it.copy(error = null) }; return } // el botón se queda sin efecto y el campo marca 0
        if (s.accountId == null) { _uiState.update { it.copy(error = PaymentDomainError.MISSING_ACCOUNT) }; return }
        if (s.categoryId == null) { _uiState.update { it.copy(error = PaymentDomainError.MISSING_CATEGORY) }; return }
        viewModelScope.launch {
            val entity = ScheduledPaymentEntity(
                id = s.paymentId ?: 0,
                name = s.name.trim(),
                amountCents = s.amountCents,
                accountId = s.accountId,
                categoryId = s.categoryId,
                frequency = s.frequency,
                nextDueDate = s.nextDueDate,
                anchorDayOfMonth = s.anchorDayOfMonth,
                remindDaysBefore = s.remindDaysBefore,
                active = s.active
            )
            if (s.isEditing) financeRepository.updateScheduledPayment(entity)
            else financeRepository.insertScheduledPayment(entity)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
