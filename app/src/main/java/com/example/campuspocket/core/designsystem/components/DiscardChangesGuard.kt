package com.example.campuspocket.core.designsystem.components

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.campuspocket.R

/**
 * Protege un formulario con cambios sin guardar.
 *
 * Devuelve la acción que debe usar la flecha de la barra superior; el gesto o
 * botón Atrás del sistema se intercepta con [BackHandler]. Si no hay cambios,
 * la acción navega directamente.
 */
@Composable
fun rememberDiscardChangesGuard(
    hasUnsavedChanges: Boolean,
    onDiscard: () -> Unit
): () -> Unit {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = hasUnsavedChanges) { showDialog = true }

    val requestBack: () -> Unit = {
        if (hasUnsavedChanges) showDialog = true else onDiscard()
    }

    if (showDialog) {
        CampusDialog(
            onDismissRequest = { showDialog = false },
            title = stringResource(R.string.discard_changes_title),
            message = stringResource(R.string.discard_changes_message),
            icon = Icons.Filled.Warning,
            destructive = true,
            confirmText = stringResource(R.string.discard_changes_confirm),
            dismissText = stringResource(R.string.discard_changes_keep_editing),
            onConfirm = {
                showDialog = false
                onDiscard()
            }
        )
    }

    return requestBack
}
