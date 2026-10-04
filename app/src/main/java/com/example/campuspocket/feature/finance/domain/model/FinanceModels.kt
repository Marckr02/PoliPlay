package com.example.campuspocket.feature.finance.domain.model

import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionEntity
import com.example.campuspocket.feature.finance.data.TransactionType
import java.time.LocalDate

/** Cuenta de dinero. id == null = aún no guardada. Todo el dinero viaja en centavos. */
data class Account(
    val id: Long? = null,
    val name: String,
    val type: AccountType,
    val initialBalanceCents: Long = 0,
    val colorArgb: Int = 0xFF4C8DFF.toInt(),
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int = 0
) {
    fun toEntity(): AccountEntity = AccountEntity(
        id = id ?: 0,
        name = name,
        type = type.name,
        initialBalanceCents = initialBalanceCents,
        colorArgb = colorArgb,
        includeInTotal = includeInTotal,
        archived = archived,
        sortOrder = sortOrder
    )

    companion object {
        fun fromEntity(entity: AccountEntity): Account = Account(
            id = entity.id,
            name = entity.name,
            type = AccountType.valueOf(entity.type),
            initialBalanceCents = entity.initialBalanceCents,
            colorArgb = entity.colorArgb,
            includeInTotal = entity.includeInTotal,
            archived = entity.archived,
            sortOrder = entity.sortOrder
        )
    }
}

data class Category(
    val id: Long? = null,
    val name: String,
    val kind: CategoryKind,
    val colorArgb: Int = 0xFF4C8DFF.toInt(),
    val archived: Boolean = false
) {
    fun toEntity(): CategoryEntity = CategoryEntity(
        id = id ?: 0,
        name = name,
        kind = kind.name,
        colorArgb = colorArgb,
        archived = archived
    )

    companion object {
        fun fromEntity(entity: CategoryEntity): Category = Category(
            id = entity.id,
            name = entity.name,
            kind = CategoryKind.valueOf(entity.kind),
            colorArgb = entity.colorArgb,
            archived = entity.archived
        )
    }
}

data class Transaction(
    val id: Long? = null,
    val type: TransactionType,
    val amountCents: Long,
    val date: LocalDate,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val categoryId: Long? = null,
    val description: String? = null,
    val refundOfId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): TransactionEntity = TransactionEntity(
        id = id ?: 0,
        type = type.name,
        amountCents = amountCents,
        date = date,
        accountId = accountId,
        targetAccountId = targetAccountId,
        categoryId = categoryId,
        description = description,
        refundOfId = refundOfId,
        createdAt = createdAt
    )

    companion object {
        fun fromEntity(entity: TransactionEntity): Transaction = Transaction(
            id = entity.id,
            type = TransactionType.valueOf(entity.type),
            amountCents = entity.amountCents,
            date = entity.date,
            accountId = entity.accountId,
            targetAccountId = entity.targetAccountId,
            categoryId = entity.categoryId,
            description = entity.description,
            refundOfId = entity.refundOfId,
            createdAt = entity.createdAt
        )
    }
}
