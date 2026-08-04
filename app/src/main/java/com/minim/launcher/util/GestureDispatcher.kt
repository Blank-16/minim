package com.minim.launcher.util

/**
 * Turns a GestureAction into an actual effect. Takes plain callbacks rather
 * than an Activity/Context reference directly, so the dispatch logic itself
 * (which action maps to which callback) is decoupled from Android framework
 * specifics and could be unit-tested with fake lambdas. MainActivity wires
 * the callbacks once in onCreate; this class just does the routing.
 */
class GestureDispatcher(
    private val onLock: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onExpandNotifications: () -> Unit,
    private val onToggleTorch: () -> Unit,
    private val onOpenApp: (packageName: String) -> Unit,
    private val onActivateProfile: (spaceId: String?) -> Unit
) {
    fun execute(action: GestureAction) {
        when (action) {
            GestureAction.None -> Unit
            GestureAction.Lock -> onLock()
            GestureAction.OpenSettings -> onOpenSettings()
            GestureAction.ExpandNotifications -> onExpandNotifications()
            GestureAction.ToggleTorch -> onToggleTorch()
            is GestureAction.OpenApp -> onOpenApp(action.packageName)
            is GestureAction.ActivateProfile -> onActivateProfile(action.spaceId)
        }
    }
}
