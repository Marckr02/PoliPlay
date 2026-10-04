package com.example.campuspocket.feature.finance.data.repository

import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.finance.data.AccountDao
import com.example.campuspocket.feature.finance.data.CategoryDao
import com.example.campuspocket.feature.finance.data.ScheduledPaymentDao
import com.example.campuspocket.feature.finance.data.TransactionDao
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import androidx.room.withTransaction
import com.example.campuspocket.feature.finance.data.BudgetDao
import com.example.campuspocket.feature.finance.domain.PaymentDomain
import com.example.campuspocket.feature.finance.domain.BudgetAlertChecker
import kotlinx.coroutines.flow.first
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val scheduledPaymentDao: ScheduledPaymentDao,
    private val db: AppDatabase,
    private val dataStore: DataStore<Preferences>,
    @dagger.hilt.android.qualifiers.ApplicationContext private val applicationContext: android.content.Context,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : FinanceRepository {

    // ---- Cuentas -----------------------------------------------------------

    override fun observeActiveAccounts(): Flow<List<Account>> =
        accountDao.observeActive().map { list -> list.map(Account::fromEntity) }

    override fun observeAccount(accountId: Long): Flow<Account?> =
        accountDao.observeAll().map { list ->
            list.firstOrNull { it.id == accountId }?.let(Account::fromEntity)
        }

    override suspend fun getAccount(accountId: Long): Account? =
        withContext(ioDispatcher) { accountDao.getById(accountId)?.let(Account::fromEntity) }

    override suspend fun insertAccount(account: Account): Long =
        withContext(ioDispatcher) { accountDao.insert(account.toEntity()) }

    override suspend fun updateAccount(account: Account) {
        withContext(ioDispatcher) { accountDao.update(account.toEntity()) }
    }

    override suspend fun archiveAccount(accountId: Long) {
        withContext(ioDispatcher) {
            accountDao.getById(accountId)?.let { accountDao.update(it.copy(archived = true)) }
        }
    }

    // ---- Categorías --------------------------------------------------------

    override fun observeActiveCategories(): Flow<List<Category>> =
        categoryDao.observeActive().map { list -> list.map(Category::fromEntity) }

    override suspend fun getCategory(categoryId: Long): Category? =
        withContext(ioDispatcher) { categoryDao.getById(categoryId)?.let(Category::fromEntity) }

    override suspend fun insertCategory(category: Category): Long =
        withContext(ioDispatcher) { categoryDao.insert(category.toEntity()) }

    override suspend fun updateCategory(category: Category) {
        withContext(ioDispatcher) { categoryDao.update(category.toEntity()) }
    }

    override suspend fun archiveCategory(categoryId: Long) {
        withContext(ioDispatcher) {
            categoryDao.getById(categoryId)?.let { categoryDao.update(it.copy(archived = true)) }
        }
    }

    // ---- Transacciones -----------------------------------------------------

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map(Transaction::fromEntity) }

    override fun observeTransactionsByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        transactionDao.observeByDateRange(start, end).map { list -> list.map(Transaction::fromEntity) }

    override fun observeTransactionsByAccount(accountId: Long): Flow<List<Transaction>> =
        transactionDao.observeByAccount(accountId).map { list -> list.map(Transaction::fromEntity) }

    override suspend fun getTransaction(id: Long): Transaction? =
        withContext(ioDispatcher) { transactionDao.getById(id)?.let(Transaction::fromEntity) }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val result = withContext(ioDispatcher) { transactionDao.insert(transaction.toEntity()) }
        // alerta de presupuesto solo si es gasto (la notificación se dispara fuera, con el umbral cruzado)
        maybeNotifyBudget(transaction)
        return result
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        withContext(ioDispatcher) { transactionDao.update(transaction.toEntity()) }
        maybeNotifyBudget(transaction)
    }

    /** Si al guardar este gasto se cruza el 80 % o el 100 % del presupuesto del mes, notifica una vez. */
    private suspend fun maybeNotifyBudget(tx: Transaction) {
        if (tx.type != com.example.campuspocket.feature.finance.data.TransactionType.EXPENSE) return
        val month = tx.date.toString().substring(0, 7)
        val categoryId = tx.categoryId ?: return
        val budget = budgetDao.getByMonthAndCategory(month, categoryId) ?: return

        val spent = transactionDao.observeNetSpentByCategory(tx.date.withDayOfMonth(1), tx.date)
            .first()
            .firstOrNull { it.categoryId == categoryId }?.netSpent ?: return

        val before = spent - tx.amountCents
        val threshold = BudgetAlertChecker.crossedThreshold(before, spent, budget.amountCents)
        if (threshold == 0) return

        val key = "budget_alert:${month}:${categoryId}"
        val prefs = dataStore.data.first()
        val already = prefs[intPreferencesKey(key)] ?: 0
        if (already >= threshold) return
        dataStore.edit { it[intPreferencesKey(key)] = threshold }

        // Disparo real por el canal budget_alerts
        val context = applicationContext
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        com.example.campuspocket.core.notifications.NotificationChannels.ensureCreated(context)
        val categoryName = categoryDao.getById(categoryId)?.name ?: return
        val textRes = if (threshold >= 100) {
            com.example.campuspocket.R.string.budget_alert_100
        } else {
            com.example.campuspocket.R.string.budget_alert_80
        }
        val notification = androidx.core.app.NotificationCompat.Builder(
            context,
            com.example.campuspocket.core.notifications.NotificationChannels.BUDGET
        )
            .setSmallIcon(com.example.campuspocket.R.drawable.ic_notification)
            .setContentTitle(categoryName)
            .setContentText(context.getString(textRes))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        androidx.core.app.NotificationManagerCompat.from(context).notify(("bud_".hashCode() * 31 + categoryId).toInt(), notification)
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        withContext(ioDispatcher) {
            transactionDao.getById(transactionId)?.let { transactionDao.delete(it) }
        }
    }

    // ---- Presupuestos (Fase 5A) -------------------------------------------

    override fun observeAllBudgets() = budgetDao.observeAll()

    override fun observeBudgetsForMonth(yearMonth: String) = budgetDao.observeByMonth(yearMonth)

    override suspend fun getBudgetsForMonth(yearMonth: String) =
        withContext(ioDispatcher) { budgetDao.getByMonth(yearMonth) }

    override suspend fun getBudget(yearMonth: String, categoryId: Long) =
        withContext(ioDispatcher) { budgetDao.getByMonthAndCategory(yearMonth, categoryId) }

    override suspend fun upsertBudget(yearMonth: String, categoryId: Long, amountCents: Long) {
        withContext(ioDispatcher) {
            val existing = budgetDao.getByMonthAndCategory(yearMonth, categoryId)
            if (existing != null) {
                budgetDao.update(existing.copy(amountCents = amountCents))
            } else {
                budgetDao.insert(
                    com.example.campuspocket.feature.finance.data.BudgetEntity(
                        yearMonth = yearMonth,
                        categoryId = categoryId,
                        amountCents = amountCents
                    )
                )
            }
        }
    }

    override suspend fun deleteBudget(yearMonth: String, categoryId: Long) {
        withContext(ioDispatcher) {
            budgetDao.getByMonthAndCategory(yearMonth, categoryId)?.let { budgetDao.delete(it) }
        }
    }

    // ---- Pagos programados (Fase 5B) ----------------------------------------

    override fun observeActiveScheduledPayments() = scheduledPaymentDao.observeActive()
    override fun observeAllScheduledPayments() = scheduledPaymentDao.observeAll()

    override suspend fun getScheduledPayment(id: Long) =
        withContext(ioDispatcher) { scheduledPaymentDao.getById(id) }

    override suspend fun getPaymentsDueUntil(date: LocalDate) =
        withContext(ioDispatcher) { scheduledPaymentDao.getDueUntil(date) }

    override suspend fun insertScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity) =
        withContext(ioDispatcher) { scheduledPaymentDao.insert(payment) }

    override suspend fun updateScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity) {
        withContext(ioDispatcher) { scheduledPaymentDao.update(payment) }
    }

    override suspend fun deleteScheduledPayment(id: Long) {
        withContext(ioDispatcher) {
            scheduledPaymentDao.getById(id)?.let { scheduledPaymentDao.delete(it) }
        }
    }

    override suspend fun markScheduledPaymentPaid(
        payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity,
        amountCents: Long,
        date: LocalDate
    ) {
        withContext(ioDispatcher) {
            db.withTransaction {
                transactionDao.insert(
                    com.example.campuspocket.feature.finance.data.TransactionEntity(
                        type = com.example.campuspocket.feature.finance.data.TransactionType.EXPENSE.name,
                        amountCents = amountCents,
                        date = date,
                        accountId = payment.accountId,
                        categoryId = payment.categoryId,
                        description = payment.name,
                        scheduledPaymentId = payment.id
                    )
                )
                val next = PaymentDomain.nextDueAfter(payment, date)
                scheduledPaymentDao.update(payment.copy(nextDueDate = next))
            }
        }
    }

    // ---- Agregados ---------------------------------------------------------

    override fun observeAccountDeltas(): Flow<List<TransactionDao.AccountDelta>> =
        transactionDao.observeAccountDeltas()

    override fun observeTransferInDeltas(): Flow<List<TransactionDao.AccountDelta>> =
        transactionDao.observeTransferInDeltas()

    override fun observeNetSpentByCategory(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<TransactionDao.CategorySpent>> =
        transactionDao.observeNetSpentByCategory(start, end)
}
