package com.example.campuspocket.core.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusKeypadLabel
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.designsystem.extended
import com.example.campuspocket.core.util.Haptics

const val KEYPAD_BACKSPACE = "⌫"
const val KEYPAD_DOUBLE_ZERO = "00"
const val KEYPAD_TRIPLE_ZERO = "000"

private val KEYPAD_FIRST_ROW = listOf("1", "2", "3")
private val KEYPAD_OTHER_ROWS = listOf(
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf(KEYPAD_DOUBLE_ZERO, "0", KEYPAD_TRIPLE_ZERO)
)

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
 * Panel del teclado del formulario de movimiento (v6, Parte 2).
 *
 * Rejilla fija de 4 columnas: fila 1 con 1-3 y borrar; filas 2-4 con los
 * dígitos y, en la cuarta columna, un botón Guardar que ocupa esas tres filas.
 * Vibra en cada tecla y en el guardado.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionKeypadPanel(
    amountDigits: String,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = true
) {
    val ext = MaterialTheme.extended
    val view = LocalView.current
    val digits = amountDigits.filter(Char::isDigit)
    val digitWords = stringArrayResource(R.array.keypad_digit_words)
    val backspaceDescription = stringResource(R.string.keypad_backspace_desc)
    val doubleZeroDescription = stringResource(R.string.keypad_double_zero_desc)
    val tripleZeroDescription = stringResource(R.string.keypad_triple_zero_desc)
    val saveLabel = stringResource(R.string.keypad_save)

    fun descriptionOf(key: String): String = when (key) {
        KEYPAD_BACKSPACE -> backspaceDescription
        KEYPAD_DOUBLE_ZERO -> doubleZeroDescription
        KEYPAD_TRIPLE_ZERO -> tripleZeroDescription
        else -> digitWords.getOrNull(key.toIntOrNull() ?: -1) ?: key
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 3.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val spacing = 8.dp
            val keyWidth = (maxWidth - spacing * 3) / 4f
            val saveHeight = 48.dp * 3f + spacing * 2f

            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    KEYPAD_FIRST_ROW.forEach { key ->
                        KeypadKey(
                            label = key,
                            description = descriptionOf(key),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Haptics.tick(view)
                                onAmountChange(keypadNextDigits(digits, key))
                            }
                        )
                    }
                    KeypadKey(
                        label = KEYPAD_BACKSPACE,
                        description = descriptionOf(KEYPAD_BACKSPACE),
                        modifier = Modifier.weight(1f),
                        isBackspace = true,
                        onLongClick = {
                            Haptics.reject(view)
                            onAmountChange("")
                        },
                        onClick = {
                            Haptics.tick(view)
                            onAmountChange(keypadNextDigits(digits, KEYPAD_BACKSPACE))
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                        KEYPAD_OTHER_ROWS.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                row.forEach { key ->
                                    KeypadKey(
                                        label = key,
                                        description = descriptionOf(key),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            Haptics.tick(view)
                                            onAmountChange(keypadNextDigits(digits, key))
                                        }
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Guardar ocupa las tres filas de la cuarta columna.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .width(keyWidth)
                            .height(saveHeight)
                            .alpha(if (saveEnabled) 1f else 0.45f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ext.accent)
                            .semantics { contentDescription = saveLabel }
                            .combinedClickable(
                                enabled = saveEnabled,
                                role = Role.Button,
                                onClick = {
                                    Haptics.confirm(view)
                                    onSave()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                                tint = ext.onAccent,
                                modifier = Modifier.height(24.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = saveLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.onAccent,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBackspace: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ext.track)
            .semantics { contentDescription = description }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            Icon(
                painter = painterResource(R.drawable.ic_backspace),
                contentDescription = null, // decorativo: la etiqueta contigua describe la accion
                tint = ext.onTrack,
                modifier = Modifier.height(24.dp)
            )
        } else {
            Text(
                text = label,
                style = CampusKeypadLabel,
                color = ext.onTrack,
                maxLines = 1
            )
        }
    }
}

@Composable
@Preview(name = "TransactionKeypadPanel", showBackground = true)
fun PreviewTransactionKeypadPanel() {
    CampusTheme {
        Surface {
            TransactionKeypadPanel(
                amountDigits = "12,50",
                onAmountChange = {},
                onSave = {}
            )
        }
    }
}
