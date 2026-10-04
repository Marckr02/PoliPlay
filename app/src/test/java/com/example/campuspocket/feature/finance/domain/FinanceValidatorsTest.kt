package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import com.example.campuspocket.feature.finance.testIncome
import com.example.campuspocket.feature.finance.testRefund
import com.example.campuspocket.feature.finance.testTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Reglas de validación de transacciones (spec 7.2). */
class FinanceValidatorsTest {

    private val expenseCategory = testCategory(id = 1, kind = CategoryKind.EXPENSE)
    private val incomeCategory = testCategory(id = 2, name = "Mesada", kind = CategoryKind.INCOME)
    private val categories = listOf(expenseCategory, incomeCategory)

    @Test
    fun `monto cero o negativo no vale`() {
        assertEquals(
            TransactionError.AMOUNT_NOT_POSITIVE,
            FinanceValidators.validate(testExpense(amountCents = 0), categories)
        )
        assertEquals(
            TransactionError.AMOUNT_NOT_POSITIVE,
            FinanceValidators.validate(testExpense(amountCents = -100), categories)
        )
    }

    @Test
    fun `transferencia exige cuenta destino distinta`() {
        assertEquals(
            TransactionError.MISSING_TARGET_ACCOUNT,
            FinanceValidators.validate(testTransfer(targetAccountId = 0).copy(targetAccountId = null), categories)
        )
        assertEquals(
            TransactionError.TRANSFER_SAME_ACCOUNT,
            FinanceValidators.validate(
                testTransfer(accountId = 1, targetAccountId = 1),
                categories
            )
        )
        assertNull(
            FinanceValidators.validate(testTransfer(accountId = 1, targetAccountId = 2), categories)
        )
    }

    @Test
    fun `gasto exige categoria de gasto`() {
        assertEquals(
            TransactionError.WRONG_CATEGORY_KIND,
            FinanceValidators.validate(testExpense(categoryId = 2), categories)
        )
        assertNull(FinanceValidators.validate(testExpense(categoryId = 1), categories))
    }

    @Test
    fun `ingreso exige categoria de ingreso`() {
        assertEquals(
            TransactionError.WRONG_CATEGORY_KIND,
            FinanceValidators.validate(testIncome(categoryId = 1), categories)
        )
        assertNull(FinanceValidators.validate(testIncome(categoryId = 2), categories))
    }

    @Test
    fun `reembolso exige categoria y permite no vincular gasto original`() {
        assertEquals(
            TransactionError.MISSING_CATEGORY,
            FinanceValidators.validate(testRefund(categoryId = null), categories)
        )
        assertNull(FinanceValidators.validate(testRefund(categoryId = 1, refundOfId = null), categories))
        assertNull(FinanceValidators.validate(testRefund(categoryId = 1, refundOfId = 9), categories))
    }
}
