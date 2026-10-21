package com.example.campuspocket.core.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.extended

/** Teclas del teclado de montos, en el orden visual de 4 filas × 3 columnas. */
private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("00", "0", KEYPAD_BACKSPACE)
)

const val KEYPAD_BACKSPACE = "⌫"

/**
 * Calcula los dígitos que se envían al ViewModel tras pulsar una tecla.
 *
 * El ViewModel interpreta los dígitos como centavos (estilo cajero), por lo que
 * el texto visible se deriva de ahí y aquí solo se manipulan dígitos. Se quitan
 * los ceros a la izquierda y se limita el monto a 9 dígitos (9.999.999,99).
 * Esta función es pura para poder probarla sin Compose.
 */
fun keypadNextDigits(current: String, key: String): String {
    val digits = current.filter(Char::isDigit)
    if (key == KEYPAD_BACKSPACE) {
        return digits.dropLast(1).trimStart('0').take(9)
    }

    val appended = digits + key.filter(Char::isDigit)
    return appended.trimStart('0').take(9)
}

/**
 * Teclado de montos de 4 filas × 3 teclas. Se usa como cajero: cada dígito entra
 * por la derecha de los centavos. La tecla de borrar vacía todo al mantenerla
 * pulsada.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadAmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    label: String = ""
) {
    val ext = MaterialTheme.extended
    val haptics = LocalHapticFeedback.current
    val digits = amount.filter(Char::isDigit)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (digits.isEmpty()) "0" else amount,
            style = MaterialTheme.typography.displayMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KEYPAD_ROWS.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isBackspace = key == KEYPAD_BACKSPACE
                        val keyDescription = if (isBackspace) {
                            stringResource(R.string.keypad_backspace)
                        } else {
                            key
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(ext.track)
                                .semantics { contentDescription = keyDescription }
                                .combinedClickable(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAmountChange(keypadNextDigits(digits, key))
                                    },
                                    onLongClick = if (isBackspace) {
                                        {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onAmountChange("")
                                        }
                                    } else {
                                        null
                                    },
                                    role = Role.Button
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBackspace) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_backspace),
                                    contentDescription = null,
                                    tint = ext.onTrack,
                                    modifier = Modifier.height(24.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ext.onTrack
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(name = "KeypadAmountInput", showBackground = true)
fun PreviewKeypadAmountInput() {
    CampusTheme {
        Surface {
            KeypadAmountInput(
                amount = "12,50",
                onAmountChange = {},
                label = stringResource(R.string.transaction_amount)
            )
        }
    }
}
