package com.example.campuspocket.core.util

import android.util.Log
import com.example.campuspocket.BuildConfig

/**
 * Logs solo en builds de debug. Regla de privacidad: los mensajes NUNCA deben incluir
 * montos, nombres, descripciones ni contenido de notas; solo ids y eventos técnicos.
 */
object LogUtil {
    private const val TAG_PREFIX = "CampusPocket"

    fun d(message: String, tag: String = "App") {
        if (BuildConfig.DEBUG) Log.d("$TAG_PREFIX/$tag", message)
    }

    fun w(message: String, tag: String = "App") {
        if (BuildConfig.DEBUG) Log.w("$TAG_PREFIX/$tag", message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = "App") {
        if (BuildConfig.DEBUG) Log.e("$TAG_PREFIX/$tag", message, throwable)
    }
}
