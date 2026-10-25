package com.example.campuspocket.feature.finance

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.TransactionSubtype
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.data.repository.FinanceRepositoryImpl
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.WithdrawalError
import com.example.campuspocket.feature.finance.domain.WithdrawalResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Instrumentado (Room en memoria): retiro desde ahorros a Efectivo en UNA transacción,
 * validaciones sin sobregiro y sin dejar rastros cuando falla.
 */
@RunWith(AndroidJUnit4::class)
class WithdrawalFlowTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepositoryImpl

    private val date = LocalDate.of(2026, 10, 20)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val prefs = PreferenceDataStoreFactory.create(
            produceFile = { File(context.cacheDir, "test_prefs_" + System.nanoTime() + ".preferences_pb") }
        )
        repo = FinanceRepositoryImpl(
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            templateDao = db.transactionTemplateDao(),
            budgetDao = db.budgetDao(),
            monthlyBudgetDao = db.monthlyBudgetDao(),
            scheduledPaymentDao = db.scheduledPaymentDao(),
            db = db,
            dataStore = prefs,
            applicationContext = context,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun account(name: String, type: AccountType, balance: Long): Long =
        db.accountDao().insert(
            AccountEntity(name = name, type = type.name, initialBalanceCents = balance)
        )

    @Test
    fun retiroValidoMueveSaldoYQuedaComoTransferenciaSinGasto() = runBlocking {
        val cashId = account("Efectivo", AccountType.CASH, 0)
        val savingsId = account("Ahorros", AccountType.SAVINGS, 20_000)

        val result = repo.withdrawToCash(savingsId, 1_500, date)

        assertTrue(result is WithdrawalResult.Success)
        val tx = db.transactionDao().observeAll().first().single()
        assertEquals(TransactionType.TRANSFER.name, tx.type)
        assertEquals(TransactionSubtype.RETIRO_CAJERO.name, tx.subtype)
        assertEquals(1_500L, tx.amountCents)
        assertEquals(savingsId, tx.accountId)
        assertEquals(cashId, tx.targetAccountId)
        assertNull(tx.categoryId)

        assertEquals(18_500L, db.accountDao().getBalanceCents(savingsId)!!)
        assertEquals(1_500L, db.accountDao().getBalanceCents(cashId)!!)

        // No es gasto: el resumen del mes sigue en cero.
        val summary = GetMonthSummary.compute(
            yearMonth = java.time.YearMonth.from(date),
            transactions = repo.observeTransactions().first(),
            budgets = emptyMap(),
            categories = emptyMap(),
            monthStart = date.withDayOfMonth(1),
            monthEnd = date.withDayOfMonth(date.lengthOfMonth())
        )
        assertEquals(0L, summary.totalSpentCents)
        assertEquals(0, summary.rows.size)
    }

    @Test
    fun montoNoMultiploDeCincoFallaSinInsertarNada() = runBlocking {
        val cashId = account("Efectivo", AccountType.CASH, 0)
        val savingsId = account("Ahorros", AccountType.SAVINGS, 20_000)

        val result = repo.withdrawToCash(savingsId, 1_200, date)

        assertEquals(
            WithdrawalError.NOT_MULTIPLE_OF_5,
            (result as WithdrawalResult.Failure).error
        )
        assertTrue(db.transactionDao().observeAll().first().isEmpty())
        assertEquals(20_000L, db.accountDao().getBalanceCents(savingsId)!!)
        assertEquals(0L, db.accountDao().getBalanceCents(cashId)!!)
    }

    @Test
    fun sobregiroFallaYDejaSaldosIntactos() = runBlocking {
        val cashId = account("Efectivo", AccountType.CASH, 0)
        val savingsId = account("Ahorros", AccountType.SAVINGS, 1_000)

        val result = repo.withdrawToCash(savingsId, 1_500, date)

        assertEquals(
            WithdrawalError.INSUFFICIENT_BALANCE,
            (result as WithdrawalResult.Failure).error
        )
        assertTrue(db.transactionDao().observeAll().first().isEmpty())
        assertEquals(1_000L, db.accountDao().getBalanceCents(savingsId)!!)
        assertEquals(0L, db.accountDao().getBalanceCents(cashId)!!)
    }

    @Test
    fun sinCuentaEfectivoFallaYNoInserta() = runBlocking {
        val savingsId = account("Ahorros", AccountType.SAVINGS, 20_000)

        val result = repo.withdrawToCash(savingsId, 1_500, date)

        assertEquals(
            WithdrawalError.MISSING_CASH_ACCOUNT,
            (result as WithdrawalResult.Failure).error
        )
        assertTrue(db.transactionDao().observeAll().first().isEmpty())
    }

    @Test
    fun unaCuentaQueNoEsDeAhorrosNoSirveComoOrigen() = runBlocking {
        account("Efectivo", AccountType.CASH, 0)
        val debitId = account("Débito", AccountType.DEBIT, 20_000)

        val result = repo.withdrawToCash(debitId, 1_500, date)

        assertEquals(
            WithdrawalError.MISSING_SOURCE_ACCOUNT,
            (result as WithdrawalResult.Failure).error
        )
        assertTrue(db.transactionDao().observeAll().first().isEmpty())
    }

    @Test
    fun retirarElSaldoExactoDejaLaCuentaEnCero() = runBlocking {
        val cashId = account("Efectivo", AccountType.CASH, 0)
        val savingsId = account("Ahorros", AccountType.SAVINGS, 20_000)

        val result = repo.withdrawToCash(savingsId, 20_000, date)

        assertTrue(result is WithdrawalResult.Success)
        assertEquals(0L, db.accountDao().getBalanceCents(savingsId)!!)
        assertEquals(20_000L, db.accountDao().getBalanceCents(cashId)!!)
    }
}
