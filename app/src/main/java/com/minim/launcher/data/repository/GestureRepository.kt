package com.minim.launcher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.minim.launcher.util.GestureAction
import com.minim.launcher.util.GestureType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gestureDataStore by preferencesDataStore(name = "minim_gestures")

/**
 * Every gesture the launcher recognizes maps to a GestureAction, all in one
 * small store. Kept separate from SettingsRepository since this is a
 * self-contained feature (gesture -> action bindings) rather than a general
 * preference.
 */
class GestureRepository(private val context: Context) {

    private fun keyFor(type: GestureType) = stringPreferencesKey("gesture_${type.name}")

    fun observe(type: GestureType): Flow<GestureAction> =
        context.gestureDataStore.data.map { prefs ->
            prefs[keyFor(type)]?.let { GestureAction.decode(it) } ?: type.defaultAction
        }

    suspend fun set(type: GestureType, action: GestureAction) {
        context.gestureDataStore.edit { it[keyFor(type)] = action.encode() }
    }

    /** All bindings at once, e.g. for the Gestures settings screen. */
    fun observeAll(): Flow<Map<GestureType, GestureAction>> =
        context.gestureDataStore.data.map { prefs ->
            GestureType.entries.associateWith { type ->
                prefs[keyFor(type)]?.let { GestureAction.decode(it) } ?: type.defaultAction
            }
        }
}
