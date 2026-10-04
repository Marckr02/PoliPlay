package com.example.campuspocket.core.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Utilidades de dinero. Todo el dinero de la app es un [Long] en centavos.
 * Nunca se usa Double ni Float, ni siquiera de forma intermedia.
 */
object Money {
    const val DEFAULT_CURRENCY_CODE = "USD"

    /** Locale por defecto: español de Ecuador. */
    val DEFAULT_LOCALE: Locale = Locale.Builder().setLanguage("es").setRegion("EC").build()

    private val INPUT_PATTERN = Regex("""^\d+(\.\d{1,2})?$|^\.\d{1,2}$""")

    /**
     * Convierte texto escrito por el usuario a centavos. Devuelve null si el texto no es válido.
     *
     * Reglas:
     * - Acepta coma o punto como separador decimal, con máximo 2 decimales.
     * - Si aparecen ambos separadores, el último es el decimal y el otro es de miles
     *   ("1.234,56" y "1,234.56" valen 1234.56).
     * - Si aparece un solo tipo de separador una vez, es el decimal ("1,5" vale 1.50).
     *   Por eso "1.234" se rechaza (3 decimales): para miles escribe "1234" o "1.234,00".
     * - Si un mismo separador aparece varias veces, se toma como separador de miles ("1.234.567").
     * - Acepta signo "-" o "+" al inicio y el símbolo "$".
     */
    fun parseToCents(input: String): Long? {
        var text = input.trim().replace(" ", "").replace("$", "")
        if (text.isEmpty()) return null

        var negative = false
        when (text.first()) {
            '-' -> { negative = true; text = text.substring(1) }
            '+' -> text = text.substring(1)
        }
        if (text.isEmpty()) return null

        val lastDot = text.lastIndexOf('.')
        val lastComma = text.lastIndexOf(',')
        val normalized = when {
            lastDot >= 0 && lastComma >= 0 -> {
                val decimalIsDot = lastDot > lastComma
                val thousands = if (decimalIsDot) ',' else '.'
                text.replace(thousands.toString(), "").replace(',', '.')
            }
            lastDot >= 0 || lastComma >= 0 -> {
                val sep = if (lastDot >= 0) '.' else ','
                val count = text.count { it == sep }
                if (count > 1) text.replace(sep.toString(), "") else text.replace(sep, '.')
            }
            else -> text
        }

        if (!INPUT_PATTERN.matches(normalized)) return null

        return try {
            val withLeadingZero = if (normalized.startsWith(".")) "0$normalized" else normalized
            val cents = BigDecimal(withLeadingZero).movePointRight(2).longValueExact()
            if (negative) -cents else cents
        } catch (e: ArithmeticException) {
            null // número demasiado grande o con decimales sobrantes
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Texto plano editable, con punto decimal: 1999 -> "19.99". */
    fun toPlainString(cents: Long): String =
        BigDecimal.valueOf(cents, 2).toPlainString()

    /**
     * Texto decimal local SIN símbolo de moneda, con 2 decimales fijos:
     * 0 -> "0,00"; 1234 -> "12,34" (locale es_EC: coma decimal y punto de miles).
     * Sirve para la entrada de montos estilo cajero (solo dígitos).
     */
    fun toLocalDecimalString(
        cents: Long,
        locale: Locale = DEFAULT_LOCALE
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        formatter.isGroupingUsed = false
        return formatter.format(BigDecimal.valueOf(cents, 2))
    }

    /** Texto con formato de moneda: 123456 -> "$1.234,56" (según el locale). */
    fun format(
        cents: Long,
        currencyCode: String = DEFAULT_CURRENCY_CODE,
        locale: Locale = DEFAULT_LOCALE
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(currencyCode)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.format(BigDecimal.valueOf(cents, 2))
    }

    /** Igual que [format] pero con "+" explícito para valores positivos. */
    fun formatSigned(
        cents: Long,
        currencyCode: String = DEFAULT_CURRENCY_CODE,
        locale: Locale = DEFAULT_LOCALE
    ): String {
        val text = format(cents, currencyCode, locale)
        return if (cents > 0) "+$text" else text
    }
}
