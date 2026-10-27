package com.example.campuspocket.core.designsystem.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.campuspocket.core.designsystem.CampusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Regresión del selector de fecha:
 * - Debe estar en español aunque el dispositivo use otro idioma.
 * - La columna del domingo debe estar visible y ser seleccionable.
 */
class CampusDatePickerDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun domingoVisibleYSeleccionable() {
        var confirmada: LocalDate? = null

        composeRule.setContent {
            CampusTheme {
                CampusDatePickerDialog(
                    title = "Fecha",
                    initialDate = LocalDate.of(2026, 10, 1),
                    onDismiss = {},
                    onConfirm = { confirmada = it }
                )
            }
        }

        // Material3 usa ClearAndSetSemantics en los encabezados de día: el
        // nombre accesible es "domingo", no el glifo "D".
        composeRule.onNodeWithContentDescription("domingo").assertIsDisplayed()

        // El 25 de octubre de 2026 es domingo: debe verse y poder elegirse.
        composeRule.onNodeWithText("domingo, 25 de octubre de 2026")
            .assertIsDisplayed()
            .performClick()
            .assertIsSelected()

        composeRule.onNodeWithText("Aceptar").performClick()
        assertEquals(LocalDate.of(2026, 10, 25), confirmada)
    }
}
