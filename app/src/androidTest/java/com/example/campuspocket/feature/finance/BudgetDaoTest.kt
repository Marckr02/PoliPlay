package com.example.campuspocket.feature.finance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.finance.data.BudgetEntity
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionEntity
import com.example.campuspocket.feature.finance.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Instrumentado (Room en memoria, en EMULADOR): el índice único (yearMonth, categoryId)
 * y las consultas de presupuesto/gasto neto con datos mixtos.
 */
@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {

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

    @Test
    fun mesMasCategoriaEsUnico() = runBlocking {
        val catId = db.categoryDao().insert(CategoryEntity(name = "Comida", kind = CategoryKind.EXPENSE.name, iconKey = "category"))
        db.budgetDao().insert(BudgetEntity(yearMonth = "2026-10", categoryId = catId, amountCents = 100_000))

        val ex = try {
            db.budgetDao().insert(BudgetEntity(yearMonth = "2026-10", categoryId = catId, amountCents = 200_000))
            null
        } catch (e: Exception) {
            e
        }
        assertNotNull(ex) // SQLiteConstraintException por el índice único
    }

    @Test
    fun consultasDePresupuestoYGastoNetoConDatosMixtos() = runBlocking {
        // cuentas requeridas por la FK (RESTRICT)
        val account1 = db.accountDao().insert(com.example.campuspocket.feature.finance.data.AccountEntity(name = "Efectivo", type = com.example.campuspocket.feature.finance.data.AccountType.CASH.name))
        val account2 = db.accountDao().insert(com.example.campuspocket.feature.finance.data.AccountEntity(name = "Banco", type = com.example.campuspocket.feature.finance.data.AccountType.DEBIT.name))

        val catFood = db.categoryDao().insert(CategoryEntity(name = "Comida", kind = CategoryKind.EXPENSE.name, iconKey = "category"))
        val catTrans = db.categoryDao().insert(CategoryEntity(name = "Transporte", kind = CategoryKind.EXPENSE.name, iconKey = "category"))
        val catSalary = db.categoryDao().insert(CategoryEntity(name = "Mesada", kind = CategoryKind.INCOME.name, iconKey = "category"))

        // Presupuestos octubre
        db.budgetDao().insert(BudgetEntity(yearMonth = "2026-10", categoryId = catFood, amountCents = 100_000))
        db.budgetDao().insert(BudgetEntity(yearMonth = "2026-10", categoryId = catTrans, amountCents = 50_000))
        // Presupuesto noviembre (fuera del mes consultado)
        db.budgetDao().insert(BudgetEntity(yearMonth = "2026-11", categoryId = catFood, amountCents = 110_000))

        val budgets = db.budgetDao().getByMonth("2026-10")
        assertEquals(2, budgets.size)
        assertEquals(100_000L, budgets.first { it.categoryId == catFood }.amountCents)
        assertEquals(50_000L, budgets.first { it.categoryId == catTrans }.amountCents)

        // Transacciones de octubre mixtas: gasto, reembolso, ingreso, transferencia
        db.transactionDao().insert(TransactionEntity(type = TransactionType.EXPENSE.name, amountCents = 5_000, date = LocalDate.of(2026, 10, 3), accountId = account1, categoryId = catFood))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.EXPENSE.name, amountCents = 2_000, date = LocalDate.of(2026, 10, 5), accountId = account1, categoryId = catFood))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.REFUND.name, amountCents = 500, date = LocalDate.of(2026, 10, 6), accountId = account1, categoryId = catFood))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.EXPENSE.name, amountCents = 1_000, date = LocalDate.of(2026, 10, 7), accountId = account1, categoryId = catTrans))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.INCOME.name, amountCents = 20_000, date = LocalDate.of(2026, 10, 1), accountId = account1, categoryId = catSalary))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.TRANSFER.name, amountCents = 3_400, date = LocalDate.of(2026, 10, 2), accountId = account1, targetAccountId = account2))

        // Gasto neto de octubre por categoría (spec 7.3: expense - refund; sin transfer)
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val net = db.transactionDao().observeNetSpentByCategory(start, end).first()
            .associate { it.categoryId to it.netSpent }

        assertEquals(6_500L, net[catFood])    // 5000 + 2000 - 500
        assertEquals(1_000L, net[catTrans])
        // el ingreso suma 0 (no cuenta como gasto), la transferencia (sin categoría) no aparece
        assertEquals(0L, net[catSalary])
        assertEquals(3, net.size)
    }

    @Test
    fun upsertNoPisaElDelMesDistinto() = runBlocking {
        val catId = db.categoryDao().insert(CategoryEntity(name = "Comida", kind = CategoryKind.EXPENSE.name, iconKey = "category"))
        val a = db.budgetDao().insert(BudgetEntity(yearMonth = "2026-10", categoryId = catId, amountCents = 100_000))
        val b = db.budgetDao().insert(BudgetEntity(yearMonth = "2026-11", categoryId = catId, amountCents = 120_000))
        assertEquals(2, db.budgetDao().getAll().size)
        // november existe separado
        assertNotNull(db.budgetDao().getByMonthAndCategory("2026-11", catId))
    }
}
