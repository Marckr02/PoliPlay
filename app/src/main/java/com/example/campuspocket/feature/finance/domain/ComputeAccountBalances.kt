package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.TransactionDao
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Cuenta con su saldo calculado (centavos). */
data class AccountBalance(
    val account: Account,
    val balanceCents: Long
)

/**
 * Saldos calculados combinando consultas del DAO (spec 7.1):
 * saldo = inicial + Σ(ingresos+reembolsos) − Σ(gastos+transferencias salientes) + Σ(transferencias entrantes).
 * Las tarjetas de crédito pueden quedar en negativo (deuda).
 * Es un Flow: la UI se actualiza sola al cambiar cualquier dato.
 */
class ComputeAccountBalances @Inject constructor(
    private val financeRepository: FinanceRepository
) {
    fun observe(): Flow<List<AccountBalance>> = combine(
        financeRepository.observeActiveAccounts(),
        financeRepository.observeAccountDeltas(),
        financeRepository.observeTransferInDeltas()
    ) { accounts, deltas, transferIns ->
        compute(accounts, deltas, transferIns)
    }

    companion object {
        /** Cálculo puro (probado en JVM sin Room). */
        fun compute(
            accounts: List<Account>,
            deltas: List<TransactionDao.AccountDelta>,
            transferIns: List<TransactionDao.AccountDelta>
        ): List<AccountBalance> {
            val deltaByAccount = deltas.associate { it.accountId to it.delta }
            val inByAccount = transferIns.associate { it.accountId to it.delta }
            return accounts.map { account ->
                val id = account.id ?: 0L
                AccountBalance(
                    account = account,
                    balanceCents = account.initialBalanceCents +
                        (deltaByAccount[id] ?: 0L) +
                        (inByAccount[id] ?: 0L)
                )
            }
        }
    }
}

/**
 * Gasto neto por categoría en un rango, combinando consultas del DAO (spec 7.2 on budgets, Fase 5 lo usará;
 * la consulta se prueba ya en instrumentado).
 */
class ComputeNetSpentByCategory @Inject constructor(
    private val financeRepository: FinanceRepository
) {
    fun observe(start: LocalDate, end: LocalDate): Flow<List<CategorySpent>> = combine(
        financeRepository.observeNetSpentByCategory(start, end),
        financeRepository.observeActiveCategories()
    ) { spent, categories ->
        val byId = categories.associateBy { it.id }
        spent.mapNotNull { row ->
            byId[row.categoryId]?.let { CategorySpent(it, row.netSpent) }
        }
    }
}

data class CategorySpent(
    val category: Category,
    val netSpentCents: Long
)
