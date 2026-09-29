package com.minim.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Backs the dot-badge notification indicator. Requires the user to
 * explicitly grant notification-listener access via
 * Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS (triggered from the
 * Settings screen) — this is never requested silently, since it's one of
 * the more sensitive permissions Android exposes.
 *
 * The active-package set is exposed as a plain in-process StateFlow rather
 * than written to Room/DataStore: it's pure live UI state, changes on every
 * notification post/removal, and has zero value once the process dies —
 * persisting it would just be write amplification for no benefit.
 */
class MinimNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _activePackages = MutableStateFlow<Set<String>>(emptySet())
        val activePackages: StateFlow<Set<String>> = _activePackages
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Without this, revoking notification-listener access (or the
        // system killing this service) left the last-known package set
        // showing forever — badge dots stuck "on" for apps that may not
        // even have a notification anymore, with no way to refresh short of
        // a new notification arriving after the service reconnects.
        _activePackages.value = emptySet()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refresh()
    }

    private fun refresh() {
        _activePackages.value = runCatching {
            activeNotifications.map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
    }
}
