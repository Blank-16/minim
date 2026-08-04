package com.minim.launcher.util

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo

private const val HOST_ID = 1024

/**
 * Real widget hosting via AppWidgetHost/AppWidgetManager — the platform
 * ceremony a launcher can't avoid if it wants an actual widget, not just a
 * placeholder box. A launcher app can't bind widgets directly (BIND_APPWIDGET
 * is a system-protected permission); the supported path is
 * AppWidgetHost.allocateAppWidgetId() + the ACTION_APPWIDGET_PICK system
 * picker, handled by MainActivity's ActivityResultLauncher.
 *
 * startListening()/stopListening() must be called from the Activity's
 * onStart()/onStop() — this is what lets the host receive live widget
 * updates only while the launcher is actually visible.
 */
class WidgetHostManager(private val activity: Activity) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(activity, HOST_ID)

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()

    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteAppWidgetId(appWidgetId: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }

    fun getProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    /** Null if the widget was uninstalled or the id is otherwise no longer valid. */
    fun createHostView(appWidgetId: Int): AppWidgetHostView? {
        val info = getProviderInfo(appWidgetId) ?: return null
        return runCatching {
            appWidgetHost.createView(activity, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        }.getOrNull()
    }
}
