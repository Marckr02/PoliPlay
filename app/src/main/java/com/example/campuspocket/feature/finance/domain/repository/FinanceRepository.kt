package com.example.campuspocket.feature.finance.domain.repository

import com.example.campuspocket.feature.finance.data.TransactionDao
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Finanzas: cuentas, categorías y transacciones (Fase 4; pagos programados llegan en la 5).
 * Cuentas y categorías NO se borran: se archivan (`archive*`).
 */
interface FinanceRepository {

    // Cuentas
    fun observeActiveAccounts(): Flow<List<Account>>
    fun observeAccount(accountId: Long): Flow<Account?>
    suspend fun getAccount(accountId: Long): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun archiveAccount(accountId: Long)

    // Categorías
    fun observeActiveCategories(): Flow<List<Category>>
    suspend fun getCategory(categoryId: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun archiveCategory(categoryId: Long)

    // Transacciones
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransactionsByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>>
    fun observeTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    suspend fun getTransaction(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: Long)

    // Presupuestos (Fase 5A). Entidad a nivel DAO: la del mes es (yearMonth, categoryId).
    fun observeAllBudgets(): Flow<List<com.example.campuspocket.feature.finance.data.BudgetEntity>>
    fun observeBudgetsForMonth(yearMonth: String): Flow<List<com.example.campuspocket.feature.finance.data.BudgetEntity>>
    suspend fun getBudgetsForMonth(yearMonth: String): List<com.example.campuspocket.feature.finance.data.BudgetEntity>
    suspend fun getBudget(yearMonth: String, categoryId: Long): com.example.campuspocket.feature.finance.data.BudgetEntity?
    suspend fun upsertBudget(yearMonth: String, categoryId: Long, amountCents: Long)
    suspend fun deleteBudget(yearMonth: String, categoryId: Long)

    // Pagos programados (Fase 5B).
    fun observeActiveScheduledPayments(): Flow<List<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>>
    fun observeAllScheduledPayments(): Flow<List<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>>
    suspend fun getScheduledPayment(id: Long): com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity?
    suspend fun getPaymentsDueUntil(date: LocalDate): List<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>
    suspend fun insertScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity): Long
    suspend fun updateScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity)
    suspend fun deleteScheduledPayment(id: Long)

    /**
     * "Marcar como pagado" en UNA transacción de Room: crea el EXPENSE con scheduledPaymentId
     * (fecha de hoy) y avanza la próxima fecha. Si algo falla, no cambia nada.
     */
    suspend fun markScheduledPaymentPaid(
        payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity,
        amountCents: Long,
        date: LocalDate
    )

    // Agregados (consultas ya existentes del DAO)
    fun observeAccountDeltas(): Flow<List<TransactionDao.AccountDelta>>
    fun observeTransferInDeltas(): Flow<List<TransactionDao.AccountDelta>>
    fun observeNetSpentByCategory(start: LocalDate, end: LocalDate): Flow<List<TransactionDao.CategorySpent>>
}
