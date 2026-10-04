package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction

/**
 * Errores de validación de una transacción (spec 7.2). La UI los traduce con strings.xml.
 */
enum class TransactionError {
    AMOUNT_NOT_POSITIVE,
    MISSING_ACCOUNT,
    TRANSFER_SAME_ACCOUNT,
    MISSING_TARGET_ACCOUNT,
    MISSING_CATEGORY,
    WRONG_CATEGORY_KIND
}

/** Reglas de negocio puras de las transacciones. */
object FinanceValidators {

    /**
     * Valida la transacción; null = correcta.
     * - monto siempre > 0.
     * - transferencia: origen y destino distintos, sin categoría.
     * - gasto/reembolso: categoría obligatoria y de tipo gasto.
     * - ingreso: categoría obligatoria y de tipo ingreso.
     * - reembolso: vincular a un gasto original es opcional.
     */
    fun validate(tx: Transaction, categories: List<Category>): TransactionError? {
        if (tx.amountCents <= 0) return TransactionError.AMOUNT_NOT_POSITIVE
        if (tx.accountId <= 0) return TransactionError.MISSING_ACCOUNT

        return when (tx.type) {
            TransactionType.TRANSFER -> {
                val target = tx.targetAccountId ?: return TransactionError.MISSING_TARGET_ACCOUNT
                if (target == tx.accountId) TransactionError.TRANSFER_SAME_ACCOUNT else null
            }

            TransactionType.EXPENSE, TransactionType.REFUND -> {
                val category = tx.categoryId?.let { id -> categories.firstOrNull { it.id == id } }
                    ?: return TransactionError.MISSING_CATEGORY
                if (category.kind != CategoryKind.EXPENSE) TransactionError.WRONG_CATEGORY_KIND else null
            }

            TransactionType.INCOME -> {
                val category = tx.categoryId?.let { id -> categories.firstOrNull { it.id == id } }
                    ?: return TransactionError.MISSING_CATEGORY
                if (category.kind != CategoryKind.INCOME) TransactionError.WRONG_CATEGORY_KIND else null
            }
        }
    }
}
