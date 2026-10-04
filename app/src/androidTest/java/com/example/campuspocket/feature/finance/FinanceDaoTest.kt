package com.example.campuspocket.feature.finance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionEntity
import com.example.campuspocket.feature.finance.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Instrumentado (Room en memoria): verifica de verdad observeAccountDeltas,
 * observeTransferInDeltas y observeNetSpentByCategory con datos mixtos (spec 7.1).
 */
@RunWith(AndroidJUnit4::class)
class FinanceDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed(): Triple<Long, Long, Long> {
        val accountDao = db.accountDao()
        val categoryDao = db.categoryDao()
        val cash = accountDao.insert(AccountEntity(name = "Efectivo", type = AccountType.CASH.name, initialBalanceCents = 10_000))
        val bank = accountDao.insert(AccountEntity(name = "Banco", type = AccountType.DEBIT.name, initialBalanceCents = 5_000))
        val food = categoryDao.insert(CategoryEntity(name = "Comida", kind = CategoryKind.EXPENSE.name))
        val tx = db.transactionDao()
        // Cuenta 1: -gasto 300, +ingreso 1000, -transferencia 200, +reembolso 100 -> delta +600
        tx.insert(TransactionEntity(type = "EXPENSE", amountCents = 300, date = LocalDate.of(2026, 10, 1), accountId = cash, categoryId = food))
        tx.insert(TransactionEntity(type = "INCOME", amountCents = 1_000, date = LocalDate.of(2026, 10, 2), accountId = cash, categoryId = food))
        tx.insert(TransactionEntity(type = "TRANSFER", amountCents = 200, date = LocalDate.of(2026, 10, 3), accountId = cash, targetAccountId = bank))
        tx.insert(TransactionEntity(type = "REFUND", amountCents = 100, date = LocalDate.of(2026, 10, 4), accountId = cash, categoryId = food))
        // Cuenta 2: solo gasto 500
        tx.insert(TransactionEntity(type = "EXPENSE", amountCents = 500, date = LocalDate.of(2026, 10, 2), accountId = bank, categoryId = food))
        return Triple(cash, bank, food)
    }

    @Test
    fun accountDeltasMezclaLosCuatroTiposComoNulos() = runBlocking {
        val (cash, bank, _) = seed()

        val deltas = db.transactionDao().observeAccountDeltas().first()
            .associate { it.accountId to it.delta }

        // INCOME/REFUND suman; EXPENSE/TRANSFER restan en el origen.
        assertEquals(600L, deltas[cash])   // +1000 -300 -200 +100
        assertEquals(-500L, deltas[bank])
    }

    @Test
    fun transferInDeltasSoloCuentaEntradas() = runBlocking {
        val (cash, bank, _) = seed()

        val ins = db.transactionDao().observeTransferInDeltas().first()
            .associate { it.accountId to it.delta }

        assertEquals(200L, ins[bank])
        assertTrue(ins[cash] == null)
    }

    @Test
    fun netSpentByCategoryRestaLosReembolsos() = runBlocking {
        val (_, _, food) = seed()

        val spent = db.transactionDao()
            .observeNetSpentByCategory(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
            .first()
            .associate { it.categoryId to it.netSpent }

        // Gastos 300+500, reembolso -100 -> 700 en la categoría.
        assertEquals(700L, spent[food])
    }
}
