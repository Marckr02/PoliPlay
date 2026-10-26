package com.example.campuspocket.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.extended

/**
 * Diálogo propio de CampusPocket: tarjeta de radio 32 sobre un velo verde
 * azulado, icono opcional y botones apilados. Sustituye a AlertDialog.
 */
@Composable
fun CampusDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    dismissText: String = stringResource(R.string.cancel),
    onDismiss: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    showDismiss: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val interaction = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ext.scrim)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 340.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    if (destructive) ext.peach else ext.lavender,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null, // decorativo: el título ya lo anuncia
                                tint = if (destructive) ext.onPeach else ext.onLavender,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    content?.invoke(this)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showDismiss) {
                            CampusButton(
                                text = dismissText,
                                onClick = onDismiss ?: onDismissRequest,
                                style = CampusButtonStyle.SECONDARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        CampusButton(
                            text = confirmText,
                            onClick = onConfirm,
                            style = if (destructive) {
                                CampusButtonStyle.DESTRUCTIVE
                            } else {
                                CampusButtonStyle.PRIMARY
                            },
                            enabled = confirmEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/** Fila de acciones para diálogos que necesitan un botón extra. */
@Composable
fun CampusDialogActions(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}
