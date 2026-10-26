package com.example.campuspocket.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campuspocket.core.designsystem.PillShape
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.R

/**
 * Estado vacío reutilizable (Sección 8 v5).
 *
 * Tarjeta `surface` de radio 32 dp, icono de 72 dp en caja tintada
 * (24 dp de radio), título 20 sp ExtraBold, texto 14 sp, botón primario de 48 dp.
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    @androidx.annotation.DrawableRes iconRes: Int,
    @androidx.annotation.StringRes titleRes: Int,
    @androidx.annotation.StringRes messageRes: Int,
    @androidx.annotation.StringRes actionLabelRes: Int? = null,
    action: (() -> Unit)? = null
) {
    val ext = MaterialTheme.extended

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icono en caja tintada
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(ext.lavender),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                        tint = ext.onLavender,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                actionLabelRes?.let { labelRes ->
                    if (action != null) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Button(
                            onClick = action,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(PillShape),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Estado vacío específico para tareas.
 */
@Composable
fun EmptyTasksState(
    modifier: Modifier = Modifier,
    onNewTask: (() -> Unit)? = null
) {
    EmptyState(
        modifier = modifier,
        iconRes = R.drawable.ic_dock_school_outline,
        titleRes = R.string.empty_tasks_title,
        messageRes = R.string.empty_tasks_message,
        actionLabelRes = R.string.task_new,
        action = onNewTask
    )
}

/**
 * Estado vacío específico para movimientos/finanzas.
 */
@Composable
fun EmptyMovementsState(
    modifier: Modifier = Modifier,
    onNewMovement: (() -> Unit)? = null
) {
    EmptyState(
        modifier = modifier,
        iconRes = R.drawable.ic_dock_wallet_outline,
        titleRes = R.string.empty_movements_title,
        messageRes = R.string.empty_movements_message,
        actionLabelRes = R.string.finance_new_movement,
        action = onNewMovement
    )
}

/**
 * Estado vacío específico para notas.
 */
@Composable
fun EmptyNotesState(
    modifier: Modifier = Modifier,
    onNewNote: (() -> Unit)? = null
) {
    EmptyState(
        modifier = modifier,
        iconRes = R.drawable.ic_dock_notes_outline,
        titleRes = R.string.empty_notes_title,
        messageRes = R.string.empty_notes_message,
        actionLabelRes = R.string.note_new,
        action = onNewNote
    )
}

/**
 * Estado vacío específico para pagos programados.
 */
@Composable
fun EmptyPaymentsState(
    modifier: Modifier = Modifier,
    onNewPayment: (() -> Unit)? = null
) {
    EmptyState(
        modifier = modifier,
        iconRes = R.drawable.ic_schedule,
        titleRes = R.string.empty_payments_title,
        messageRes = R.string.empty_payments_message,
        actionLabelRes = R.string.scheduled_payment_new,
        action = onNewPayment
    )
}

/**
 * Estado vacío específico para materias.
 */
@Composable
fun EmptyCoursesState(
    modifier: Modifier = Modifier,
    onNewCourse: (() -> Unit)? = null
) {
    EmptyState(
        modifier = modifier,
        iconRes = R.drawable.ic_dock_school_outline,
        titleRes = R.string.empty_courses_title,
        messageRes = R.string.empty_courses_message,
        actionLabelRes = R.string.course_new,
        action = onNewCourse
    )
}