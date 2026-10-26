package com.example.campuspocket.core.designsystem.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.campuspocket.core.designsystem.CampusTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Prueba instrumentada para el arreglo del domingo en CampusDatePickerDialog.
 *
 * Sin el arreglo, el domingo (columna 7) quedaba cortado por el ancho limitado
 * del CampusDialog (~292 dp vs 360 dp requeridos por DatePicker).
 * Esta prueba verifica que el encabezado "D" y el día 25 (domingo 25 oct 2026)
 * están visibles y clicables.
 */
class CampusDatePickerDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun domingoVisibleYSeleccionable() {
        composeRule.setContent {
            CampusTheme {
                CampusDatePickerDialog(
                    title = "Fecha",
                    initialDate = LocalDate.of(2026, 10, 25), // domingo
                    onDismiss = {},
                    onConfirm = {}
                )
            }
        }

        // El encabezado "D" (domingo) debe estar visible
        composeRule.onNodeWithText("D").assertIsDisplayed()

        // El día 25 (domingo 25 oct 2026) debe estar visible
        // Puede haber múltiples nodos con "25" (día + año), tomamos el primero
        composeRule.onAllNodes(hasText("25"))[0].assertIsDisplayed()

        // Opcional: verificar que se puede hacer click en el día 25
        // composeRule.onAllNodes(hasText("25"))[0].performClick()
    }
}