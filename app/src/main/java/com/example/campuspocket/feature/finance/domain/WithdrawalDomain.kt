package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.domain.model.Account

/** Errores del retiro en cajero. La UI los traduce con strings.xml. */
enum class WithdrawalError {
    MISSING_AMOUNT,
    NOT_MULTIPLE_OF_5,
    INSUFFICIENT_BALANCE,
    MISSING_CASH_ACCOUNT,
    MISSING_SOURCE_ACCOUNT
}

/** Resultado atómico del retiro (lo devuelve el repositorio). */
sealed interface WithdrawalResult {
    data class Success(val transactionId: Long) : WithdrawalResult
    data class Failure(
        val error: WithdrawalError,
        val accountName: String? = null
    ) : WithdrawalResult
}

/**
 * Reglas puras del retiro en cajero (Fase 8A).
 *
 * El monto se expresa en centavos y debe ser múltiplo de 5 DÓLARES (500 centavos),
 * igual que los chips $5 / $10 / $15.
 */
object WithdrawalRules {

    const val MULTIPLE_OF_FIVE_DOLLARS_CENTS = 500L

    fun isMultipleOfFiveDollars(amountCents: Long): Boolean =
        amountCents > 0 && amountCents % MULTIPLE_OF_FIVE_DOLLARS_CENTS == 0L

    /**
     * Valida un retiro. null = correcto.
     * El orden coincide con la tabla de la especificación.
     */
    fun validate(
        amountCents: Long,
        sourceBalanceCents: Long?,
        sourceName: String?,
        hasCashAccount: Boolean
    ): WithdrawalError? {
        if (amountCents <= 0) return WithdrawalError.MISSING_AMOUNT
        if (!isMultipleOfFiveDollars(amountCents)) return WithdrawalError.NOT_MULTIPLE_OF_5
        if (sourceBalanceCents == null || sourceName == null) return WithdrawalError.MISSING_SOURCE_ACCOUNT
        if (!hasCashAccount) return WithdrawalError.MISSING_CASH_ACCOUNT
        if (amountCents > sourceBalanceCents) return WithdrawalError.INSUFFICIENT_BALANCE
        return null
    }
}

/**
 * El retiro siempre va a "Efectivo". Si hay varias cuentas de tipo efectivo,
 * se prefiere la que se llama exactamente "Efectivo" y, si no, la primera.
 */
object CashAccountRules {
    fun pick(accounts: List<Account>): Account? {
        val cashAccounts = accounts.filter { it.type == AccountType.CASH }
        return cashAccounts.firstOrNull { it.name.trim().equals("Efectivo", ignoreCase = true) }
            ?: cashAccounts.firstOrNull()
    }
}
