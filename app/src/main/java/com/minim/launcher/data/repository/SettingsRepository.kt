package com.minim.launcher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "minim_settings")

/** All flags are opt-in and default to the lightest-weight behavior. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val SHOW_ICONS = booleanPreferencesKey("show_icons")
        val SHOW_SUGGESTIONS = booleanPreferencesKey("show_suggestions")
        val LOCATION_SUGGESTIONS = booleanPreferencesKey("location_suggestions")
        val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        // DOUBLE_TAP_LOCK / SWIPE_DOWN_NOTIFICATIONS superseded by the
        // Gestures screen (GestureRepository) — all gestures are now
        // individually assignable rather than fixed booleans.
        val DOT_BADGES = booleanPreferencesKey("dot_badges")
        val WIDGET_COLLAPSED = booleanPreferencesKey("widget_collapsed")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system" | "light" | "dark"
        val DESIGN_LANGUAGE = stringPreferencesKey("design_language") // "nothing" | "android16" | "glass"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_NAME = stringPreferencesKey("accent_name")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val CLOCK_TAP_APP = stringPreferencesKey("clock_tap_app") // package name, empty = default clock app
        val SHOW_QUICK_SETTINGS = booleanPreferencesKey("show_quick_settings")
        val HIDDEN_APPS_LOCKED = booleanPreferencesKey("hidden_apps_locked")
        val RECENTLY_INSTALLED_DAYS = longPreferencesKey("recently_installed_days")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val APP_DENSITY = stringPreferencesKey("app_density") // "compact" | "comfortable" | "spacious"
        val ACTIVE_SPACE_ID = stringPreferencesKey("active_space_id") // manual profile override, "" = none
        val AUTO_ACTIVATE_PROFILES = booleanPreferencesKey("auto_activate_profiles")
        val WIDGET_APP_WIDGET_ID = intPreferencesKey("widget_app_widget_id") // -1 = none configured
        val ICON_SHAPE = stringPreferencesKey("icon_shape") // "circle" | "squircle" | "roundedSquare"
        val SHOW_CALENDAR_PEEK = booleanPreferencesKey("show_calendar_peek")
        val NOTIFICATION_BADGES_ENABLED = booleanPreferencesKey("notification_badges_enabled")
    }

    val showIcons: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_ICONS] ?: false }
    val showSuggestions: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_SUGGESTIONS] ?: true }
    val locationSuggestions: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCATION_SUGGESTIONS] ?: false }
    val showStatusBar: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_STATUS_BAR] ?: true }
    val dotBadges: Flow<Boolean> = context.dataStore.data.map { it[Keys.DOT_BADGES] ?: true }
    val widgetCollapsed: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIDGET_COLLAPSED] ?: true }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    val designLanguage: Flow<String> = context.dataStore.data.map { it[Keys.DESIGN_LANGUAGE] ?: "nothing" }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }
    val accentName: Flow<String> = context.dataStore.data.map { it[Keys.ACCENT_NAME] ?: "Red" }
    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTIC_FEEDBACK] ?: true }
    val showClock: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_CLOCK] ?: true }
    val clockTapApp: Flow<String> = context.dataStore.data.map { it[Keys.CLOCK_TAP_APP] ?: "" }
    val showQuickSettings: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_QUICK_SETTINGS] ?: true }
    val hiddenAppsLocked: Flow<Boolean> = context.dataStore.data.map { it[Keys.HIDDEN_APPS_LOCKED] ?: false }
    val recentlyInstalledDays: Flow<Long> = context.dataStore.data.map { it[Keys.RECENTLY_INSTALLED_DAYS] ?: 3L }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val appDensity: Flow<String> = context.dataStore.data.map { it[Keys.APP_DENSITY] ?: "comfortable" }
    val activeSpaceId: Flow<String> = context.dataStore.data.map { it[Keys.ACTIVE_SPACE_ID] ?: "" }
    val autoActivateProfiles: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_ACTIVATE_PROFILES] ?: true }
    val widgetAppWidgetId: Flow<Int> = context.dataStore.data.map { it[Keys.WIDGET_APP_WIDGET_ID] ?: -1 }
    val iconShape: Flow<String> = context.dataStore.data.map { it[Keys.ICON_SHAPE] ?: "circle" }
    val showCalendarPeek: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_CALENDAR_PEEK] ?: false }
    val notificationBadgesEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATION_BADGES_ENABLED] ?: false }

    suspend fun setShowIcons(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_ICONS] = value }
    suspend fun setShowSuggestions(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_SUGGESTIONS] = value }
    suspend fun setLocationSuggestions(value: Boolean) = context.dataStore.edit { it[Keys.LOCATION_SUGGESTIONS] = value }
    suspend fun setShowStatusBar(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_STATUS_BAR] = value }
    suspend fun setDotBadges(value: Boolean) = context.dataStore.edit { it[Keys.DOT_BADGES] = value }
    suspend fun setWidgetCollapsed(value: Boolean) = context.dataStore.edit { it[Keys.WIDGET_COLLAPSED] = value }
    suspend fun setThemeMode(value: String) = context.dataStore.edit { it[Keys.THEME_MODE] = value }
    suspend fun setDesignLanguage(value: String) = context.dataStore.edit { it[Keys.DESIGN_LANGUAGE] = value }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setAccentName(value: String) = context.dataStore.edit { it[Keys.ACCENT_NAME] = value }
    suspend fun setHapticFeedback(value: Boolean) = context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = value }
    suspend fun setShowClock(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_CLOCK] = value }
    suspend fun setClockTapApp(value: String) = context.dataStore.edit { it[Keys.CLOCK_TAP_APP] = value }
    suspend fun setShowQuickSettings(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_QUICK_SETTINGS] = value }
    suspend fun setHiddenAppsLocked(value: Boolean) = context.dataStore.edit { it[Keys.HIDDEN_APPS_LOCKED] = value }
    suspend fun setRecentlyInstalledDays(value: Long) = context.dataStore.edit { it[Keys.RECENTLY_INSTALLED_DAYS] = value }
    suspend fun setOnboardingComplete(value: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    suspend fun setAppDensity(value: String) = context.dataStore.edit { it[Keys.APP_DENSITY] = value }
    suspend fun setActiveSpaceId(value: String) = context.dataStore.edit { it[Keys.ACTIVE_SPACE_ID] = value }
    suspend fun setAutoActivateProfiles(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_ACTIVATE_PROFILES] = value }
    suspend fun setWidgetAppWidgetId(value: Int) = context.dataStore.edit { it[Keys.WIDGET_APP_WIDGET_ID] = value }
    suspend fun setIconShape(value: String) = context.dataStore.edit { it[Keys.ICON_SHAPE] = value }
    suspend fun setShowCalendarPeek(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_CALENDAR_PEEK] = value }
    suspend fun setNotificationBadgesEnabled(value: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATION_BADGES_ENABLED] = value }

    /**
     * Exports every preference to a JSON string the user can save wherever they
     * like (Files app, cloud drive, etc). Reliability principle: the launcher's
     * personalization should never be trapped only in this device's storage.
     */
    suspend fun exportToJson(): String {
        val prefs = context.dataStore.data.first()
        val json = JSONObject()
        prefs.asMap().forEach { (key, value) -> json.put(key.name, value) }
        return json.toString(2)
    }

    /**
     * Restores every preference from an exported JSON string.
     *
     * Each key must be written back with the *same* Preferences.Key type it
     * was declared with elsewhere in this file — DataStore looks keys up by
     * name only, so writing an Int-typed preference (like
     * WIDGET_APP_WIDGET_ID) back in as a Long silently corrupts it: the very
     * next read through `intPreferencesKey` throws a ClassCastException,
     * crashing whatever was collecting that flow. `org.json` already parses
     * a plain integer literal back into an `Int` (only values outside the
     * Int range become `Long`), so writing Ints back through
     * `intPreferencesKey` round-trips correctly.
     */
    suspend fun importFromJson(jsonString: String) {
        val json = JSONObject(jsonString)
        context.dataStore.edit { prefs ->
            json.keys().forEach { key ->
                when (val value = json.get(key)) {
                    is Boolean -> prefs[booleanPreferencesKey(key)] = value
                    is String -> prefs[stringPreferencesKey(key)] = value
                    is Int -> prefs[intPreferencesKey(key)] = value
                    is Long -> prefs[longPreferencesKey(key)] = value
                }
            }
        }
    }
}
