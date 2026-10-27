package com.example.campuspocket.core.designsystem.components

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.campuspocket.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun fromDatePickerMillis(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // La app es solo en español: el selector debe hablar el mismo idioma aunque
    // el dispositivo esté configurado en otro. Material3 lee la configuración
    // local, no Locale.getDefault(), así que se provee una configuración española.
    val configuration = LocalConfiguration.current
    val spanishConfiguration = remember(configuration) {
        Configuration(configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag("es")))
        }
    }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toDatePickerMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onConfirm(fromDatePickerMillis(millis))
                    } ?: onDismiss()
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
    ) {
        // El proveedor va dentro del contenido: el diálogo compone en su propia
        // ventana y no hereda los CompositionLocal del llamador.
        CompositionLocalProvider(LocalConfiguration provides spanishConfiguration) {
            Box(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                DatePicker(
                    state = pickerState,
                    title = { Text(text = title) },
                    colors = campusDatePickerColors(),
                    modifier = Modifier.requiredWidth(360.dp)
                )
            }
        }
    }
}