package com.example.campuspocket.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferences
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.home.ui.HomeScreen
import com.example.campuspocket.feature.home.ui.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI Compose de Inicio: muestra solo módulos activos, la tarjeta abre su módulo
 * y el estado sin módulos ofrece la recuperación desde Ajustes.
 */
class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun muestraSoloLosModulosActivos() {
        composeRule.setContent {
            CampusTheme {
                HomeScreen(
                    preferences = UserPreferences(
                        academicEnabled = true,
                        financeEnabled = false,
                        notesEnabled = true
                    ),
                    onOpenModule = {},
                    onOpenSettings = {}
                )
            }
        }
        composeRule.onNodeWithText("TAREAS").assertIsDisplayed()
        composeRule.onNodeWithText("+ Tarea").assertIsDisplayed()
        composeRule.onNodeWithText("+ Nota").assertIsDisplayed()
        composeRule.onNodeWithText("+ Gasto").assertDoesNotExist()
    }

    @Test
    fun laTarjetaAbreElModuloCorrecto() {
        var abierto: CampusModule? = null
        composeRule.setContent {
            CampusTheme {
                HomeScreen(
                    preferences = UserPreferences(),
                    onOpenModule = { abierto = it },
                    onOpenSettings = {}
                )
            }
        }
        composeRule.onNodeWithText("PRESUPUESTO RESTANTE").performClick()
        assertEquals(CampusModule.FINANCE, abierto)
    }

    @Test
    fun tarjetaMuestraElRemanenteDelPresupuestoMensual() {
        composeRule.setContent {
            CampusTheme {
                HomeScreen(
                    preferences = UserPreferences(),
                    onOpenModule = {},
                    onOpenSettings = {},
                    state = HomeUiState(
                        monthlyBudgetCents = 16_000L,
                        monthlySpentCents = 5_000L,
                        monthlyRemainingCents = 11_000L
                    )
                )
            }
        }

        composeRule.onNodeWithText("PRESUPUESTO RESTANTE").assertIsDisplayed()
        composeRule.onNodeWithText(Money.format(11_000L)).assertIsDisplayed()
        composeRule.onNodeWithText(
            "de ${Money.format(16_000L)} · Gastado ${Money.format(5_000L)}"
        ).assertIsDisplayed()
    }

    @Test
    fun saludoUsaElNombreLocalCuandoExiste() {
        composeRule.setContent {
            CampusTheme {
                HomeScreen(
                    preferences = UserPreferences(displayName = "Marco"),
                    onOpenModule = {},
                    onOpenSettings = {}
                )
            }
        }
        composeRule.onNodeWithText("Hola, Marco").assertIsDisplayed()
    }

    @Test
    fun sinNombreMuestraSaludoNeutral() {
        composeRule.setContent {
            CampusTheme {
                HomeScreen(
                    preferences = UserPreferences(displayName = ""),
                    onOpenModule = {},
                    onOpenSettings = {}
                )
            }
        }
        composeRule.onNodeWithText("Tu espacio de estudio y organización").assertIsDisplayed()
    }

    @Test
    fun sinModulosOfreceElegirModulos() {
        var ajustes = false
        composeRule.setContent {
            CampusTheme {
                HomeScreen(
                    preferences = UserPreferences(
                        academicEnabled = false,
                        financeEnabled = false,
                        notesEnabled = false
                    ),
                    onOpenModule = {},
                    onOpenSettings = { ajustes = true }
                )
            }
        }
        composeRule.onNodeWithText("Sin módulos activos").assertIsDisplayed()
        composeRule.onNodeWithText("Elegir módulos").performClick()
        assertEquals(true, ajustes)
    }
}
