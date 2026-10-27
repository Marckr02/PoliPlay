package com.example.campuspocket.feature.finance.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.ColorPalette
import com.example.campuspocket.core.designsystem.components.CampusButton
import com.example.campuspocket.core.designsystem.components.CampusButtonStyle
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.CampusTextField
import com.example.campuspocket.core.designsystem.components.CampusTopBar
import com.example.campuspocket.core.designsystem.components.PillSegmentedControl
import com.example.campuspocket.core.designsystem.components.rememberDiscardChangesGuard
import com.example.campuspocket.feature.finance.data.CategoryKind

@Composable
fun CategoryFormScreen(
    onDone: () -> Unit,
    onArchived: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showArchiveDialog by remember { mutableStateOf(false) }
    val requestBack = rememberDiscardChangesGuard(
        hasUnsavedChanges = state.hasUnsavedChanges && !state.saved,
        onDiscard = onDone
    )

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        CampusTopBar(
            title = stringResource(if (state.isEditing) R.string.category_edit else R.string.category_new),
            onBack = requestBack
        )
        if (!state.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
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
                label = stringResource(R.string.category_name),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    if (state.nameError) Text(stringResource(R.string.error_required))
                }
            )

            Column {
                Text(
                    text = stringResource(R.string.category_kind),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PillSegmentedControl(
                    options = CategoryKind.entries,
                    selected = state.kind,
                    onSelect = viewModel::onKindChange,
                    label = { kind ->
                        stringResource(
                            when (kind) {
                                CategoryKind.EXPENSE -> R.string.category_kind_expense
                                CategoryKind.INCOME -> R.string.category_kind_income
                            }
                        )
                    }
                )
            }

            Text(
                text = stringResource(R.string.category_color),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (0 until 10).forEach { index ->
                    val colorName = stringResource(ColorPalette.nameRes(index))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .selectable(
                                selected = state.colorArgb == ColorPalette[index],
                                onClick = { viewModel.onColorChange(ColorPalette[index]) },
                                role = Role.RadioButton
                            )
                            .semantics { contentDescription = colorName },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(ColorPalette[index]), CircleShape)
                                .then(
                                    if (state.colorArgb == ColorPalette[index]) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
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
                text = stringResource(R.string.category_archive),
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
            message = stringResource(R.string.dialog_delete_category),
            icon = Icons.Filled.Warning,
            destructive = true,
            confirmText = stringResource(R.string.category_archive),
            onConfirm = {
                showArchiveDialog = false
                viewModel.archive()
                onArchived()
            }
        )
    }
}
