package com.example.campuspocket.feature.finance.domain

/**
 * Cruces de umbral del presupuesto (spec 7.3): notificar solo cuando se CRUZA
 * el 80 % o el 100 % por primera vez en ese mes para esa categoría.
 */
object BudgetAlertChecker {

    /**
     * Devuelve el umbral alcanzado (80 o 100) solo si `before` estaba por debajo y
     * `after` lo alcanzó o superó. 0 = nada nuevo que notificar.
     */
    fun crossedThreshold(beforeCents: Long, afterCents: Long, budgetCents: Long): Int {
        if (budgetCents <= 0) return 0
        fun percent(x: Long): Double = x.toDouble() / budgetCents.toDouble()
        return when {
            percent(beforeCents) < 1.0 && percent(afterCents) >= 1.0 -> 100
            percent(beforeCents) < 0.8 && percent(afterCents) >= 0.8 -> 80
            else -> 0
        }
    }
}
