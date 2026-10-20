package com.example.campuspocket.feature.finance.ui.withdrawal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.domain.AccountBalance
import com.example.campuspocket.feature.finance.domain.WithdrawalError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onDone: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WithdrawalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.finance_withdrawal)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.savingsAccounts.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = stringResource(R.string.withdrawal_no_savings_title),
                    message = stringResource(R.string.withdrawal_no_savings_message),
                    icon = painterResource(R.drawable.ic_wallet),
                    actionText = stringResource(R.string.withdrawal_create_savings),
                    onAction = onCreateAccount
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SavingsAccountDropdown(
                    accounts = state.savingsAccounts,
                    selectedId = state.selectedAccountId,
                    onSelect = viewModel::selectAccount
                )

                CashDestinationCard(
                    cashName = state.cashAccount?.name,
                    showError = state.error == WithdrawalError.MISSING_CASH_ACCOUNT,
                    onCreateAccount = onCreateAccount
                )

                AmountSection(
                    state = state,
                    onPreset = viewModel::selectPreset,
                    onOther = viewModel::selectOtherAmount,
                    onAmountChange = viewModel::onAmountChange
                )

                state.error?.let { error ->
                    if (error == WithdrawalError.MISSING_SOURCE_ACCOUNT) {
                        InlineError(errorText(error = error, accountName = state.selectedAccount?.account?.name))
                    }
                }

                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.withdrawal_confirm))
                }
            }
        }
    }

    state.savedAmountCents?.let { amount ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.withdrawal_success_title)) },
            text = {
                Text(stringResource(R.string.withdrawal_success, Money.format(amount)))
            },
            confirmButton = {
                TextButton(onClick = onDone) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsAccountDropdown(
    accounts: List<AccountBalance>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.account.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.account?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.withdrawal_from)) },
            supportingText = selected?.let {
                { Text(stringResource(R.string.withdrawal_balance, Money.format(it.balanceCents))) }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { balance ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(balance.account.name)
                            Text(
                                text = Money.format(balance.balanceCents),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        balance.account.id?.let(onSelect)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CashDestinationCard(
    cashName: String?,
    showError: Boolean,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wallet),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.withdrawal_to),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cashName ?: stringResource(R.string.account_type_cash),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        if (showError) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.withdrawal_error_cash),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCreateAccount) {
                    Text(stringResource(R.string.finance_new_account))
                }
            }
        }
    }
}

@Composable
private fun AmountSection(
    state: WithdrawalUiState,
    onPreset: (Int) -> Unit,
    onOther: () -> Unit,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.transaction_amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5, 10, 15).forEach { dollars ->
                AmountChip(
                    label = "$$dollars",
                    selected = !state.isCustomAmount && state.presetDollars == dollars,
                    onClick = { onPreset(dollars) },
                    modifier = Modifier.weight(1f)
                )
            }
            AmountChip(
                label = stringResource(R.string.withdrawal_other),
                selected = state.isCustomAmount,
                onClick = onOther,
                modifier = Modifier.weight(1f)
            )
        }

        if (state.isCustomAmount) {
            val amountError = state.error?.takeIf {
                it == WithdrawalError.MISSING_AMOUNT ||
                    it == WithdrawalError.NOT_MULTIPLE_OF_5 ||
                    it == WithdrawalError.INSUFFICIENT_BALANCE
            }
            OutlinedTextField(
                value = Money.toLocalDecimalString(state.amountCents),
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.withdrawal_other_amount)) },
                prefix = { Text("$") },
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let {
                    { Text(errorText(it, state.selectedAccount?.account?.name)) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            state.error?.takeIf {
                it == WithdrawalError.MISSING_AMOUNT ||
                    it == WithdrawalError.NOT_MULTIPLE_OF_5 ||
                    it == WithdrawalError.INSUFFICIENT_BALANCE
            }?.let { error ->
                InlineError(errorText(error, state.selectedAccount?.account?.name))
            }
        }
    }
}

@Composable
private fun AmountChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InlineError(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun errorText(error: WithdrawalError, accountName: String?): String = when (error) {
    WithdrawalError.MISSING_AMOUNT -> stringResource(R.string.withdrawal_error_amount)
    WithdrawalError.NOT_MULTIPLE_OF_5 -> stringResource(R.string.withdrawal_error_multiple)
    WithdrawalError.INSUFFICIENT_BALANCE ->
        stringResource(R.string.withdrawal_error_balance, accountName.orEmpty())
    WithdrawalError.MISSING_CASH_ACCOUNT -> stringResource(R.string.withdrawal_error_cash)
    WithdrawalError.MISSING_SOURCE_ACCOUNT -> stringResource(R.string.withdrawal_error_savings)
}
