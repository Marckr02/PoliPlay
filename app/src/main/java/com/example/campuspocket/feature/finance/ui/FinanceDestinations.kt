package com.example.campuspocket.feature.finance.ui

/** Rutas del grafo de finanzas (Fase 4: solo Cuentas e Historial en la pestaña). */
object FinanceDestinations {
    const val HOME = "finance/home"

    const val ACCOUNT_ID_ARG = "accountId"
    const val ACCOUNT_DETAIL_ROUTE = "finance/account/{$ACCOUNT_ID_ARG}"
    const val ACCOUNT_FORM_ROUTE = "finance/account_form?$ACCOUNT_ID_ARG={$ACCOUNT_ID_ARG}"

    const val CATEGORIES = "finance/categories"
    const val PAYMENT_ID_ARG = "paymentId"
    const val PAYMENT_FORM_ROUTE = "finance/payment_form?$PAYMENT_ID_ARG={$PAYMENT_ID_ARG}"
    const val CATEGORY_ID_ARG = "categoryId"
    const val CATEGORY_FORM_ROUTE = "finance/category_form?$CATEGORY_ID_ARG={$CATEGORY_ID_ARG}"

    const val TRANSACTION_TYPE_ARG = "type"
    const val TRANSACTION_ID_ARG = "transactionId"
    const val TRANSACTION_FORM_ROUTE =
        "finance/transaction_form/{$TRANSACTION_TYPE_ARG}?$TRANSACTION_ID_ARG={$TRANSACTION_ID_ARG}"

    fun accountDetail(accountId: Long): String = "finance/account/$accountId"

    fun accountForm(accountId: Long? = null): String =
        if (accountId == null) "finance/account_form" else "finance/account_form?$ACCOUNT_ID_ARG=$accountId"

    fun transactionForm(type: String, transactionId: Long? = null): String =
        if (transactionId == null) {
            "finance/transaction_form/$type"
        } else {
            "finance/transaction_form/$type?$TRANSACTION_ID_ARG=$transactionId"
        }

    fun categoryForm(categoryId: Long? = null): String =
        if (categoryId == null) {
            "finance/category_form"
        } else {
            "finance/category_form?$CATEGORY_ID_ARG=$categoryId"
        }

    fun paymentForm(paymentId: Long? = null): String =
        if (paymentId == null) {
            "finance/payment_form"
        } else {
            "finance/payment_form?$PAYMENT_ID_ARG=$paymentId"
        }
}
