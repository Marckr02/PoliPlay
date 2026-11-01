package com.example.campuspocket.core.designsystem.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.CampusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresión: dentro de Scaffold(bottomBar), el dock debe medir solo su franja
 * (72-120 dp) y dejar el resto de la pantalla al contenido.
 */
class FloatingDockTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun elDockDentroDeScaffoldNoOcupaLaPantalla() {
        composeRule.setContent {
            CampusTheme {
                Scaffold(
                    bottomBar = {
                        FloatingDock(
                            destinations = listOf(
                                DockDestination("home", "Inicio", R.drawable.ic_dock_home_outline, R.drawable.ic_dock_home_filled),
                                DockDestination("notes", "Notas", R.drawable.ic_dock_notes_outline, R.drawable.ic_dock_notes_filled)
                            ),
                            currentRoute = "home",
                            onNavigate = {}
                        )
                    }
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .testTag("contenido")
                    )
                }
            }
        }

        val density = composeRule.density
        val minDock = with(density) { 72.dp.roundToPx() }
        val maxDock = with(density) { 120.dp.roundToPx() }
        val dockHeight = composeRule.onNodeWithTag("floating_dock").fetchSemanticsNode().size.height
        assertTrue("El dock mide $dockHeight px", dockHeight in minDock..maxDock)

        val screenHeight = composeRule.activity.resources.displayMetrics.heightPixels
        val contentHeight = composeRule.onNodeWithTag("contenido").fetchSemanticsNode().size.height
        assertTrue("El contenido mide $contentHeight px de $screenHeight", contentHeight > screenHeight / 2)
    }
}
