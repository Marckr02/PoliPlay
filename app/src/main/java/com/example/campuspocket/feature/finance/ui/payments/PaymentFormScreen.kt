package com.example.campuspocket.feature.finance.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.edit else R.string.scheduled_payment_name)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { inner ->
        if (!state.isLoaded) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.scheduled_payment_name)) },
                isError = state.nameError,
                supportingText = { if (state.nameError) Text(stringResource(R.string.error_required)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.amountDisplay,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.scheduled_payment_amount)) },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Cuenta
            var accExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = accExpanded, onExpandedChange = { accExpanded = it }) {
                OutlinedTextField(
                    value = accounts.firstOrNull { it.id == state.accountId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.transaction_account)) },
                    isError = state.error == PaymentDomainError.MISSING_ACCOUNT,
                    supportingText = { if (state.error == PaymentDomainError.MISSING_ACCOUNT) Text(stringResource(R.string.transaction_error_missing_account)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name) },
                            onClick = { viewModel.onAccountChange(acc.id ?: 0L); accExpanded = false }
                        )
                    }
                }
            }

            // Categoría (gasto)
            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(
                    value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.transaction_category)) },
                    isError = state.error == PaymentDomainError.MISSING_CATEGORY,
                    supportingText = { if (state.error == PaymentDomainError.MISSING_CATEGORY) Text(stringResource(R.string.transaction_error_missing_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            onClick = { viewModel.onCategoryChange(c.id ?: 0L); catExpanded = false }
                        )
                    }
                }
            }

            // Frecuencia
            Column {
                Text(stringResource(R.string.scheduled_payment_frequency), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        Frequency.WEEKLY to "Semanal",
                        Frequency.MONTHLY to "Mensual",
                        Frequency.YEARLY to "Anual"
                    ).forEachIndexed { index, (f, label) ->
                        SegmentedButton(
                            selected = state.frequency == f.name,
                            onClick = { viewModel.onFrequencyChange(f.name) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) { Text(label) }
                    }
                }
            }

            // Próxima fecha de pago
            Column {
                Text(stringResource(R.string.scheduled_payment_next_due), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showDatePicker = true }) {
                    Text(DATE_FMT.format(state.nextDueDate))
                }
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
                    steps = 13
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.scheduled_payment_active), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.active, onCheckedChange = viewModel::onActiveChange)
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            viewModel.onNextDueDateChange(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate())
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = pickerState) }
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
