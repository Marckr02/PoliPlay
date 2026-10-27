package com.example.campuspocket.core.designsystem

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.campuspocket.R
import com.example.campuspocket.core.util.Money

/**
 * Modo privado global (v5 §2.4). Inicio y Finanzas lo leen para sustituir los
 * montos por un marcador; los porcentajes y las barras siguen visibles.
 */
val LocalPrivacyMode = staticCompositionLocalOf { false }

private const val PLACEHOLDER_RES = R.string.amount_hidden_placeholder

/** Monto localizado, o el marcador de privacidad cuando el modo está activo. */
@Composable
fun formatMoneyOrHidden(cents: Long): String =
    if (LocalPrivacyMode.current) {
        stringResource(PLACEHOLDER_RES)
    } else {
        Money.format(cents)
    }

@Composable
fun formatMoneyOrHiddenSigned(cents: Long): String =
    if (LocalPrivacyMode.current) {
        stringResource(PLACEHOLDER_RES)
    } else {
        Money.formatSigned(cents)
    }

/**
 * Reemplaza el marcador de un texto ya compuesto por "Monto oculto" para que
 * TalkBack no lea el símbolo decorativo del modo privado.
 */
@Composable
fun privacyAwareDescription(text: String): String =
    if (LocalPrivacyMode.current) {
        text.replace(
            stringResource(PLACEHOLDER_RES),
            stringResource(R.string.amount_hidden)
        )
    } else {
        text
    }

/**
 * Texto de dinero con soporte de modo privado. Cuando está oculto muestra
 * "$ ••••" y expone "Monto oculto" a accesibilidad (nunca el importe real).
 */
@Composable
fun MoneyText(
    amountCents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    signed: Boolean = false
) {
    // Los montos siempre usan cifras tabulares, aunque el estilo recibido no las traiga.
    val moneyStyle = style.copy(fontFeatureSettings = "tnum")
    if (LocalPrivacyMode.current) {
        val placeholder = stringResource(PLACEHOLDER_RES)
        val hiddenLabel = stringResource(R.string.amount_hidden)
        Text(
            text = placeholder,
            modifier = modifier.clearAndSetSemantics {
                contentDescription = hiddenLabel
            },
            style = moneyStyle,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    } else {
        Text(
            text = if (signed) Money.formatSigned(amountCents) else Money.format(amountCents),
            modifier = modifier,
            style = moneyStyle,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    }
}
