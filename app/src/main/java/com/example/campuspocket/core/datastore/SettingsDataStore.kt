package com.example.campuspocket.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** DataStore de preferencias. La extensión debe declararse a nivel de archivo (una sola instancia por proceso). */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
