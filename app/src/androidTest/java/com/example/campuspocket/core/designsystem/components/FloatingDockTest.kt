package com.example.campuspocket.core.designsystem.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresión de B2: al cambiar el alto fijo del dock por heightIn, las celdas
 * con fillMaxHeight se expandían hasta la altura de la pantalla. El dock debe
 * ocupar solo su franja inferior.
 */
class FloatingDockTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lasCeldasDelDockNoOcupanTodaLaPantalla() {
        composeRule.setContent {
            CampusTheme {
                FloatingDock(
                    destinations = listOf(
                        DockDestination(
                            route = "home",
                            label = "Inicio",
                            outlineRes = R.drawable.ic_dock_home_outline,
                            filledRes = R.drawable.ic_dock_home_filled
                        ),
                        DockDestination(
                            route = "notes",
                            label = "Notas",
                            outlineRes = R.drawable.ic_dock_notes_outline,
                            filledRes = R.drawable.ic_dock_notes_filled
                        )
                    ),
                    currentRoute = "home",
                    onNavigate = {}
                )
            }
        }

        val maxCellHeight = with(composeRule.density) { 100.dp.roundToPx() }
        listOf("Inicio", "Notas").forEach { label ->
            val height = composeRule.onNodeWithText(label).fetchSemanticsNode().size.height
            assertTrue("La celda '$label' mide $height px", height <= maxCellHeight)
        }
    }
}
