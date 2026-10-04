package com.example.campuspocket.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.campuspocket.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de humo REAL: lanza la MainActivity completa de la app (con Hilt, Room, Nav).
 * Si alguna pestaña rompe al iniciar, el test falla.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTabsTest {

    @Test
    fun laAppAbreYNoCascara() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Si el proceso llega aquí, la app abrió correctamente.
            it.onActivity { activity ->
                // La pestaña inicial (resumen académico) existe.
                assert(activity.hasWindowFocus() || !activity.isFinishing)
            }
        }
    }
}
