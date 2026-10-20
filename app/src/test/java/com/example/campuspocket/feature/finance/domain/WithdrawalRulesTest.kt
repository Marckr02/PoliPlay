package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.domain.model.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas JVM de las reglas del retiro en cajero (Fase 8A). */
class WithdrawalRulesTest {

    private fun account(id: Long, name: String, type: AccountType) = Account(
        id = id,
        name = name,
        type = type,
        initialBalanceCents = 0
    )

    @Test
    fun `monto vacio o cero pide elegir monto`() {
        assertEquals(
            WithdrawalError.MISSING_AMOUNT,
            WithdrawalRules.validate(0, 10_000, "Ahorros", hasCashAccount = true)
        )
        assertEquals(
            WithdrawalError.MISSING_AMOUNT,
            WithdrawalRules.validate(-500, 10_000, "Ahorros", hasCashAccount = true)
        )
    }

    @Test
    fun `el monto debe ser multiplo de 5 dolares`() {
        assertEquals(
            WithdrawalError.NOT_MULTIPLE_OF_5,
            WithdrawalRules.validate(450, 10_000, "Ahorros", hasCashAccount = true)
        )
        assertEquals(
            WithdrawalError.NOT_MULTIPLE_OF_5,
            WithdrawalRules.validate(5_001, 10_000, "Ahorros", hasCashAccount = true)
        )
        assertTrue(WithdrawalRules.isMultipleOfFiveDollars(500))
        assertTrue(WithdrawalRules.isMultipleOfFiveDollars(1_500))
        assertFalse(WithdrawalRules.isMultipleOfFiveDollars(0))
        assertNull(
            WithdrawalRules.validate(500, 10_000, "Ahorros", hasCashAccount = true)
        )
    }

    @Test
    fun `no permite sobregirar y si permite retirar el saldo exacto`() {
        assertEquals(
            WithdrawalError.INSUFFICIENT_BALANCE,
            WithdrawalRules.validate(1_000, 999, "Ahorros", hasCashAccount = true)
        )
        assertNull(
            WithdrawalRules.validate(1_000, 1_000, "Ahorros", hasCashAccount = true)
        )
    }

    @Test
    fun `exige cuenta de origen y cuenta efectivo`() {
        assertEquals(
            WithdrawalError.MISSING_SOURCE_ACCOUNT,
            WithdrawalRules.validate(500, null, null, hasCashAccount = true)
        )
        assertEquals(
            WithdrawalError.MISSING_CASH_ACCOUNT,
            WithdrawalRules.validate(500, 10_000, "Ahorros", hasCashAccount = false)
        )
    }

    @Test
    fun `prefiere la cuenta llamada Efectivo sin importar mayusculas`() {
        val accounts = listOf(
            account(1, "Caja chica", AccountType.CASH),
            account(2, "efectivo", AccountType.CASH),
            account(3, "Ahorros", AccountType.SAVINGS)
        )

        assertEquals(2L, CashAccountRules.pick(accounts)?.id)
    }

    @Test
    fun `si no existe Efectivo usa la primera cuenta de efectivo`() {
        val accounts = listOf(
            account(1, "Caja chica", AccountType.CASH),
            account(2, "Billetera", AccountType.CASH)
        )

        assertEquals(1L, CashAccountRules.pick(accounts)?.id)
        assertNull(CashAccountRules.pick(listOf(account(3, "Ahorros", AccountType.SAVINGS))))
    }
}
