package com.example.campuspocket.core.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.campuspocket.MainActivity
import com.example.campuspocket.R
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Publica los atajos dinámicos del ícono además de los estáticos de
 * `res/xml/shortcuts.xml`. "Repetir último gasto" solo aparece cuando existe
 * al menos un gasto guardado; "Nota rápida" está siempre disponible.
 */
@Singleton
class ShortcutPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val financeRepository: FinanceRepository
) {

    /**
     * Mantiene los atajos sincronizados mientras la actividad está visible.
     * Se vuelve a llamar al volver a primer plano para recoger cambios hechos
     * mientras la app estaba en segundo plano.
     */
    suspend fun keepPublished() {
        financeRepository.observeTransactions()
            .map { transactions -> transactions.any { it.type == TransactionType.EXPENSE } }
            .distinctUntilChanged()
            .collect(::publish)
    }

    private fun publish(hasExpenses: Boolean) {
        val shortcuts = buildList {
            if (hasExpenses) add(repeatExpenseShortcut())
            add(quickNoteShortcut())
        }
        // En dispositivos sin soporte de atajos el compat devuelve false; un
        // fallo del launcher no debe tumbar la app.
        runCatching {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    private fun repeatExpenseShortcut(): ShortcutInfoCompat =
        shortcut(
            id = SHORTCUT_REPEAT_EXPENSE,
            labelRes = R.string.shortcut_repeat_expense,
            iconRes = R.drawable.ic_wallet,
            route = REPEAT_EXPENSE_ROUTE,
            rank = 0
        )

    private fun quickNoteShortcut(): ShortcutInfoCompat =
        shortcut(
            id = SHORTCUT_QUICK_NOTE,
            labelRes = R.string.shortcut_quick_note,
            iconRes = R.drawable.ic_action_edit_note,
            route = QUICK_NOTE_ROUTE,
            rank = 1
        )

    private fun shortcut(
        id: String,
        labelRes: Int,
        iconRes: Int,
        route: String,
        rank: Int
    ): ShortcutInfoCompat {
        val label = context.getString(labelRes)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_ROUTE, route)
        }
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }

    companion object {
        const val EXTRA_ROUTE = "route"

        const val SHORTCUT_REPEAT_EXPENSE = "shortcut_repeat_expense"
        const val SHORTCUT_QUICK_NOTE = "shortcut_quick_note"

        private const val REPEAT_EXPENSE_ROUTE =
            "finance/transaction_form/EXPENSE?repeatLast=true"
        private const val QUICK_NOTE_ROUTE = "notes/editor/-1"
    }
}
