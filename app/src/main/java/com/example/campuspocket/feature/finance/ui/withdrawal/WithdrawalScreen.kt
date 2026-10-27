package com.example.campuspocket.feature.finance.ui.withdrawal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.campuspocket.core.designsystem.MoneyText
import com.example.campuspocket.core.designsystem.components.BentoCard
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSelectField
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.EmptyStateCard
import com.example.campuspocket.core.designsystem.components.rememberDiscardChangesGuard
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.designsystem.formatMoneyOrHidden
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
    val requestBack = rememberDiscardChangesGuard(
        hasUnsavedChanges = state.hasUnsavedChanges && state.savedAmountCents == null,
        onDiscard = onDone
    )

    Column(modifier = modifier.fillMaxSize()) {
        CampusTopBar(
            title = stringResource(R.string.finance_withdrawal),
            onBack = requestBack
        )
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.savingsAccounts.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
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

            else -> {
                val source = state.selectedAccount
                val insufficientBalance = source != null && state.amountCents > source.balanceCents
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
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

                    if (source != null && state.cashAccount != null && state.amountCents > 0L) {
                        AfterWithdrawalCard(
                            savingsAfterCents = (source.balanceCents - state.amountCents)
                                .coerceAtLeast(0L),
                            cashAfterCents = state.cashBalanceCents + state.amountCents
                        )
                    }

                    state.error?.let { error ->
                        if (error == WithdrawalError.MISSING_SOURCE_ACCOUNT) {
                            InlineError(errorText(error = error, accountName = state.selectedAccount?.account?.name))
                        }
                    }
                    if (insufficientBalance) {
                        InlineError(
                            stringResource(
                                R.string.withdrawal_insufficient_reason,
                                source?.account?.name.orEmpty()
                            )
                        )
                    }
                }
                CampusButton(
                    text = stringResource(R.string.withdrawal_confirm),
                    onClick = viewModel::save,
                    enabled = !state.isSaving && !insufficientBalance,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }

    state.savedAmountCents?.let { amount ->
        CampusDialog(
            onDismissRequest = onDone,
            title = stringResource(R.string.withdrawal_success_title),
            message = stringResource(R.string.withdrawal_success, formatMoneyOrHidden(amount)),
            icon = Icons.Filled.CheckCircle,
            confirmText = stringResource(R.string.ok),
            onConfirm = onDone,
            showDismiss = false
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

    CampusSelectField(
        value = selected?.account?.name.orEmpty(),
        label = stringResource(R.string.withdrawal_from),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        supportingText = selected?.let {
            stringResource(R.string.withdrawal_balance, formatMoneyOrHidden(it.balanceCents))
        },
        modifier = modifier
    ) {
        accounts.forEach { balance ->
            CampusSelectItem(
                text = balance.account.name + " · " + formatMoneyOrHidden(balance.balanceCents),
                selected = balance.account.id == selectedId,
                onClick = {
                    balance.account.id?.let(onSelect)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun AfterWithdrawalCard(
    savingsAfterCents: Long,
    cashAfterCents: Long,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = stringResource(R.string.withdrawal_after_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.withdrawal_savings_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoneyText(
                amountCents = savingsAfterCents,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.withdrawal_cash_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoneyText(
                amountCents = cashAfterCents,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun CashDestinationCard(
    cashName: String?,
    showError: Boolean,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = if (showError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.extended.track
            }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.extended.lavender, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wallet),
                        contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                        tint = MaterialTheme.extended.onLavender
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
                Text(
                    text = stringResource(R.string.finance_new_account),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onCreateAccount)
                        .padding(horizontal = 8.dp, vertical = 14.dp)
                )
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
            CampusTextField(
                value = Money.toLocalDecimalString(state.amountCents),
                onValueChange = onAmountChange,
                label = stringResource(R.string.withdrawal_other_amount),
                prefix = "$",
                singleLine = true,
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
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.extended.selected
        else MaterialTheme.extended.track
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                        tint = MaterialTheme.extended.onSelected,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.extended.onSelected
                    else MaterialTheme.colorScheme.onSurface
                )
            }
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
