package com.minim.launcher.util

/**
 * What a gesture does, decoupled from which gesture triggers it. Stored as a
 * plain string in GestureRepository (e.g. "app:com.spotify.music",
 * "profile:<spaceId>") so it round-trips through DataStore without needing a
 * custom type converter.
 */
sealed class GestureAction {
    object None : GestureAction()
    object Lock : GestureAction()
    object OpenSettings : GestureAction()
    object ExpandNotifications : GestureAction()
    object ToggleTorch : GestureAction()
    data class OpenApp(val packageName: String) : GestureAction()

    /** null spaceId means "return to automatic/default", same as the profile switcher's top option. */
    data class ActivateProfile(val spaceId: String?) : GestureAction()

    fun encode(): String = when (this) {
        None -> "none"
        Lock -> "lock"
        OpenSettings -> "settings"
        ExpandNotifications -> "notifications"
        ToggleTorch -> "torch"
        is OpenApp -> "app:$packageName"
        is ActivateProfile -> "profile:${spaceId ?: ""}"
    }

    companion object {
        fun decode(raw: String): GestureAction = when {
            raw == "lock" -> Lock
            raw == "settings" -> OpenSettings
            raw == "notifications" -> ExpandNotifications
            raw == "torch" -> ToggleTorch
            raw.startsWith("app:") -> OpenApp(raw.removePrefix("app:"))
            raw.startsWith("profile:") -> ActivateProfile(raw.removePrefix("profile:").ifEmpty { null })
            else -> None
        }
    }
}

enum class GestureType(val label: String, val defaultAction: GestureAction) {
    DOUBLE_TAP("Double-tap", GestureAction.Lock),
    SWIPE_DOWN("Swipe down", GestureAction.ExpandNotifications),
    SWIPE_UP("Swipe up", GestureAction.None),
    PINCH_IN("Pinch in", GestureAction.None),
    TWO_FINGER_TAP("Two-finger tap", GestureAction.None),
    EDGE_SWIPE_LEFT("Swipe in from right edge", GestureAction.None),
    EDGE_SWIPE_RIGHT("Swipe in from left edge", GestureAction.None)
}
