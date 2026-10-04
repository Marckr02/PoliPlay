package com.example.campuspocket.feature.academic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import java.time.LocalTime

/**
 * Campo de hora como botón con icono de reloj: toca y abre un [TimePicker] (formato 24 h),
 * en vez de un teclado donde no cabe el separador ":".
 * El valor se guarda como texto "H:mm" (ver [TimeText]).
 */
@Composable
fun TimeField(
    labelRes: Int,
    value: String,
    errorRes: Int?,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { showPicker = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                tint = if (errorRes != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (value.isBlank()) stringResource(labelRes) else value,
                style = MaterialTheme.typography.labelLarge,
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        errorRes?.let { error ->
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showPicker) {
        TimePickerDialog(
            titleRes = labelRes,
            initialTime = TimeText.parse(value),
            onDismiss = { showPicker = false },
            onConfirm = { time ->
                showPicker = false
                onTimeSelected(time)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    titleRes: Int,
    initialTime: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime?.hour ?: 7,
        initialMinute = initialTime?.minute ?: 0,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(titleRes)) },
        text = { TimePicker(state = pickerState) }
    )
}
