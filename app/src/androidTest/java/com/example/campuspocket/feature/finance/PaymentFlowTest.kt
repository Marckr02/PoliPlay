package com.example.campuspocket.feature.finance

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.data.repository.FinanceRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Instrumentado (emulador): marcar como pagado en UNA transacción y que avance
 * la fecha siguiente respetando el anchorDayOfMonth.
 */
@RunWith(AndroidJUnit4::class)
class PaymentFlowTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepositoryImpl

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
            budgetDao = db.budgetDao(),
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

    @Test
    fun marcarComoPagadoEsAtomicoYAavanzaSinDeriva() = runBlocking {
        val accountId = db.accountDao().insert(AccountEntity(name = "Efectivo", type = AccountType.CASH.name))
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "Comida", kind = CategoryKind.EXPENSE.name, iconKey = "category"))
        val jan31 = LocalDate.of(2026, 1, 31)
        val payment = db.scheduledPaymentDao().insert(
            ScheduledPaymentEntity(
                name = "Arriendo",
                amountCents = 500_00,
                accountId = accountId,
                categoryId = categoryId,
                frequency = "MONTHLY",
                nextDueDate = jan31,
                anchorDayOfMonth = 31,
                remindDaysBefore = 3,
                active = true
            )
        )

        repo.markScheduledPaymentPaid(db.scheduledPaymentDao().getById(payment)!!, 500_00, jan31)

        // 1 pago sched -> 1 gasto con scheduledPaymentId, fecha de hoy
        val expenses = db.transactionDao().observeAll().first()
        assertEquals(1, expenses.size)
        assertEquals("EXPENSE", expenses.single().type)
        assertEquals(payment, expenses.single().scheduledPaymentId)

        // y la próxima fecha avanzó a 28 feb (anchor 31 + clamp mes corto)
        val updated = db.scheduledPaymentDao().getById(payment)!!
        assertEquals(LocalDate.of(2026, 2, 28), updated.nextDueDate)
        // y el anchor NO se perdió
        assertEquals(31, updated.anchorDayOfMonth)
    }
}
