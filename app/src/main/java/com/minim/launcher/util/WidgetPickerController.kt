package com.minim.launcher.util

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
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
    private var pendingProviderInfo: AppWidgetProviderInfo? = null

    private val configureLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) {
            onSaved(pendingWidgetId)
        } else if (pendingWidgetId != -1) {
            widgetHostManager.deleteAppWidgetId(pendingWidgetId)
        }
        pendingWidgetId = -1
        pendingProviderInfo = null
    }

    private val bindLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingWidgetId
        val info = pendingProviderInfo
        if (result.resultCode == Activity.RESULT_OK && id != -1 && info != null) {
            if (info.configure != null) {
                launchConfigure(id, info)
            } else {
                onSaved(id)
                pendingWidgetId = -1
                pendingProviderInfo = null
            }
        } else {
            if (id != -1) widgetHostManager.deleteAppWidgetId(id)
            pendingWidgetId = -1
            pendingProviderInfo = null
        }
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
            launchConfigure(appWidgetId, providerInfo)
        } else {
            onSaved(appWidgetId)
        }
    }

    fun onWidgetSelected(providerInfo: AppWidgetProviderInfo) {
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        val bound = widgetHostManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo)
        if (bound) {
            if (providerInfo.configure != null) {
                launchConfigure(appWidgetId, providerInfo)
            } else {
                onSaved(appWidgetId)
            }
        } else {
            pendingWidgetId = appWidgetId
            pendingProviderInfo = providerInfo
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            }
            runCatching { bindLauncher.launch(bindIntent) }
                .onFailure {
                    widgetHostManager.deleteAppWidgetId(appWidgetId)
                    pendingWidgetId = -1
                    pendingProviderInfo = null
                }
        }
    }

    private fun launchConfigure(appWidgetId: Int, providerInfo: AppWidgetProviderInfo) {
        pendingWidgetId = appWidgetId
        pendingProviderInfo = providerInfo
        val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = providerInfo.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        runCatching { configureLauncher.launch(configIntent) }
            .onFailure { onSaved(appWidgetId) } // no configure activity reachable — use it as-is
    }

    fun launchPicker() {
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList<AppWidgetProviderInfo>())
            putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList<Bundle>())
        }
        runCatching { pickLauncher.launch(pickIntent) }
            .onFailure { widgetHostManager.deleteAppWidgetId(appWidgetId) }
    }

    fun remove(appWidgetId: Int) {
        widgetHostManager.deleteAppWidgetId(appWidgetId)
    }
}
