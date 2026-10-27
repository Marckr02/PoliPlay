package com.example.campuspocket.feature.finance.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.ColorPalette
import com.example.campuspocket.core.designsystem.ContrastUtil
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusButtonStyle
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusSelectField
import com.example.campuspocket.core.designsystem.components.CampusSelectItem
import com.example.campuspocket.core.designsystem.components.CampusSwitch
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.rememberDiscardChangesGuard
import com.example.campuspocket.feature.finance.data.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showArchiveDialog by remember { mutableStateOf(false) }
    val requestBack = rememberDiscardChangesGuard(
        hasUnsavedChanges = state.hasUnsavedChanges && !state.saved && !state.archivedDone,
        onDiscard = onDone
    )

    LaunchedEffect(state.saved, state.archivedDone) {
        if (state.saved || state.archivedDone) onDone()
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        CampusTopBar(
            title = stringResource(if (state.isEditing) R.string.account_name else R.string.finance_new_account),
            onBack = requestBack
        )
        if (!state.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
                label = stringResource(R.string.account_name),
                isError = state.nameError,
                supportingText = {
                    if (state.nameError) Text(stringResource(R.string.error_required))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Tipo de cuenta
            var typeExpanded by remember { mutableStateOf(false) }
            CampusSelectField(
                value = stringResource(accountTypeLabelRes(state.type)),
                label = stringResource(R.string.account_type),
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                AccountType.entries.forEach { type ->
                    CampusSelectItem(
                        text = stringResource(accountTypeLabelRes(type)),
                        selected = type == state.type,
                        onClick = {
                            viewModel.onTypeChange(type)
                            typeExpanded = false
                        }
                    )
                }
            }

            CampusTextField(
                value = state.initialAmountDisplay,
                onValueChange = viewModel::onInitialAmountChange,
                // En tarjeta de crédito el campo es la deuda inicial (positivo en pantalla).
                label = stringResource(
                    if (state.type == AccountType.CREDIT_CARD) {
                        R.string.account_debt_initial
                    } else {
                        R.string.account_initial_balance
                    }
                ),
                prefix = "$",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.account_include_in_total),
                    style = MaterialTheme.typography.bodyLarge
                )
                CampusSwitch(
                    checked = state.includeInTotal,
                    onCheckedChange = viewModel::onIncludeInTotalChange,
                    contentDescription = stringResource(R.string.account_include_in_total)
                )
            }

            // Color de la cuenta (paleta fija, sin iconos).
            Text(
                text = stringResource(R.string.category_color),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (0 until 10).forEach { index ->
                    val color = Color(ColorPalette[index])
                    val isSelected = state.colorArgb == ColorPalette[index]
                    val colorName = stringResource(ColorPalette.nameRes(index))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .selectable(
                                selected = isSelected,
                                onClick = { viewModel.onColorChange(ColorPalette[index]) },
                                role = Role.RadioButton
                            )
                            .semantics { contentDescription = colorName },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null, // decorativo: el selector ya anuncia el color
                                    tint = ContrastUtil.onColorFor(color),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

        }

        CampusButton(
            text = stringResource(R.string.save),
            onClick = viewModel::save,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (state.isEditing) {
            CampusButton(
                text = stringResource(R.string.account_archive),
                onClick = { showArchiveDialog = true },
                style = CampusButtonStyle.DESTRUCTIVE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            )
        }
    }

    if (showArchiveDialog) {
        CampusDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = state.name,
            message = stringResource(R.string.account_archived_hint),
            icon = Icons.Filled.Warning,
            destructive = true,
            confirmText = stringResource(R.string.account_archive),
            onConfirm = {
                showArchiveDialog = false
                viewModel.archive()
            }
        )
    }
}
