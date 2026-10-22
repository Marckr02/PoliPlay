package com.example.campuspocket.feature.academic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.CampusDialog
import com.example.campuspocket.core.designsystem.components.campusTimePickerColors
import com.example.campuspocket.core.designsystem.extended
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.extended.track)
                .clickable(role = Role.Button) { showPicker = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            Spacer(Modifier.width(2.dp))
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

    CampusDialog(
        onDismissRequest = onDismiss,
        title = stringResource(titleRes),
        confirmText = stringResource(R.string.ok),
        onConfirm = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) },
        content = {
            TimePicker(state = pickerState, colors = campusTimePickerColors())
        }
    )
}
