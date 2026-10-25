package com.example.campuspocket.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ámbito de corrutinas ligado a la aplicación. Se usa para tareas que deben
 * sobrevivir a la pantalla que las inició, como confirmar un borrado diferido
 * al salir de la lista (v5 §3.1).
 */
@Singleton
class AppScope @Inject constructor() {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
