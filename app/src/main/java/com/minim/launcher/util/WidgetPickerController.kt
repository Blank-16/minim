package com.minim.launcher.util

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Owns the entire widget-pick flow (allocate id -> system picker -> optional
 * configure activity -> persist) so MainActivity only needs to call
 * launchPicker() and provide an onSaved callback — it doesn't need to know
 * about AppWidgetManager, the configure-activity dance, or activity-result
 * plumbing at all.
 *
 * Must be constructed during onCreate, before the Activity reaches STARTED
 * — registerForActivityResult requires that, same as any other
 * ActivityResultLauncher.
 */
class WidgetPickerController(
    activity: ComponentActivity,
    private val widgetHostManager: WidgetHostManager,
    private val onSaved: (appWidgetId: Int) -> Unit
) {
    private var pendingWidgetId: Int = -1

    private val configureLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) {
            onSaved(pendingWidgetId)
        } else if (pendingWidgetId != -1) {
            widgetHostManager.deleteAppWidgetId(pendingWidgetId)
        }
        pendingWidgetId = -1
    }

    private val pickLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode != Activity.RESULT_OK || appWidgetId == -1) {
            if (appWidgetId != -1) widgetHostManager.deleteAppWidgetId(appWidgetId)
            return@registerForActivityResult
        }
        val providerInfo = widgetHostManager.getProviderInfo(appWidgetId)
        if (providerInfo?.configure != null) {
            pendingWidgetId = appWidgetId
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = providerInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            runCatching { configureLauncher.launch(configIntent) }
                .onFailure { onSaved(appWidgetId) } // no configure activity reachable — use it as-is
        } else {
            onSaved(appWidgetId)
        }
    }

    fun launchPicker() {
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        runCatching { pickLauncher.launch(pickIntent) }
            .onFailure { widgetHostManager.deleteAppWidgetId(appWidgetId) }
    }

    fun remove(appWidgetId: Int) {
        widgetHostManager.deleteAppWidgetId(appWidgetId)
        onSaved(-1)
    }
}
