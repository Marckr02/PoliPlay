package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Transaction

/** Neto del día: ingresos y reembolsos suman, gastos restan, transferencias no cuentan. */
object DayNet {
    fun netCents(transactions: List<Transaction>): Long = transactions.sumOf { tx ->
        when (tx.type) {
            TransactionType.INCOME, TransactionType.REFUND -> tx.amountCents
            TransactionType.EXPENSE -> -tx.amountCents
            TransactionType.TRANSFER -> 0L
        }
    }

    /** Si el día solo tiene transferencias no hay total que mostrar. */
    fun hasTotal(transactions: List<Transaction>): Boolean =
        transactions.any { it.type != TransactionType.TRANSFER }
}
