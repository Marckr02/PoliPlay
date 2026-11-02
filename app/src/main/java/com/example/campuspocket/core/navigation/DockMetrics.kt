package com.example.campuspocket.core.navigation

import androidx.compose.ui.unit.dp

/** Medidas compartidas del espacio sobre el dock. Valores idénticos a los anteriores. */
internal object DockMetrics {
    /** Reserva del contenido sobre el dock para que el FAB y el snackbar no tapen la última tarjeta. */
    val ContentBottomClearance = 68.dp
    /** Separación inferior del botón de acción contextual. */
    val ContextActionBottomOffset = 93.dp
    /** Separación inferior del snackbar cuando hay dock. */
    val SnackbarBottomOffset = 176.dp
}
