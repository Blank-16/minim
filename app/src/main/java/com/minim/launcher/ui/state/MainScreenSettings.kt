package com.minim.launcher.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.minim.launcher.data.repository.GestureRepository
import com.minim.launcher.data.repository.SettingsRepository
import com.minim.launcher.util.GestureAction
import com.minim.launcher.util.GestureType

/** Every home-screen-relevant Settings value, bundled so call sites need one line, not nineteen. */
data class MainScreenSettings(
    val widgetCollapsed: Boolean,
    val showClock: Boolean,
    val showQuickSettings: Boolean,
    val themeModeRaw: String,
    val designLanguageRaw: String,
    val dynamicColor: Boolean,
    val accentName: String,
    val activeSpaceId: String,
    val autoActivateEnabled: Boolean,
    val appWidgetId: Int,
    val showCalendarPeek: Boolean,
    val notificationBadgesEnabled: Boolean
)

@Composable
fun rememberMainScreenSettings(settings: SettingsRepository): MainScreenSettings {
    val widgetCollapsed by settings.widgetCollapsed.collectAsState(initial = true)
    val showClock by settings.showClock.collectAsState(initial = true)
    val showQuickSettings by settings.showQuickSettings.collectAsState(initial = true)
    val themeModeRaw by settings.themeMode.collectAsState(initial = "system")
    val designLanguageRaw by settings.designLanguage.collectAsState(initial = "nothing")
    val dynamicColor by settings.dynamicColor.collectAsState(initial = false)
    val accentName by settings.accentName.collectAsState(initial = "Red")
    val activeSpaceId by settings.activeSpaceId.collectAsState(initial = "")
    val autoActivateEnabled by settings.autoActivateProfiles.collectAsState(initial = true)
    val appWidgetId by settings.widgetAppWidgetId.collectAsState(initial = -1)
    val showCalendarPeek by settings.showCalendarPeek.collectAsState(initial = false)
    val notificationBadgesEnabled by settings.notificationBadgesEnabled.collectAsState(initial = false)

    return MainScreenSettings(
        widgetCollapsed, showClock, showQuickSettings, themeModeRaw, designLanguageRaw,
        dynamicColor, accentName, activeSpaceId, autoActivateEnabled, appWidgetId,
        showCalendarPeek, notificationBadgesEnabled
    )
}

/** All seven assignable gesture bindings, bundled the same way. */
data class GestureBindings(
    val doubleTap: GestureAction,
    val swipeUp: GestureAction,
    val swipeDown: GestureAction,
    val pinchIn: GestureAction,
    val twoFingerTap: GestureAction,
    val edgeSwipeLeft: GestureAction,
    val edgeSwipeRight: GestureAction
)

@Composable
fun rememberGestureBindings(gestures: GestureRepository): GestureBindings {
    val doubleTap by gestures.observe(GestureType.DOUBLE_TAP).collectAsState(initial = GestureType.DOUBLE_TAP.defaultAction)
    val swipeUp by gestures.observe(GestureType.SWIPE_UP).collectAsState(initial = GestureType.SWIPE_UP.defaultAction)
    val swipeDown by gestures.observe(GestureType.SWIPE_DOWN).collectAsState(initial = GestureType.SWIPE_DOWN.defaultAction)
    val pinchIn by gestures.observe(GestureType.PINCH_IN).collectAsState(initial = GestureType.PINCH_IN.defaultAction)
    val twoFingerTap by gestures.observe(GestureType.TWO_FINGER_TAP).collectAsState(initial = GestureType.TWO_FINGER_TAP.defaultAction)
    val edgeSwipeLeft by gestures.observe(GestureType.EDGE_SWIPE_LEFT).collectAsState(initial = GestureType.EDGE_SWIPE_LEFT.defaultAction)
    val edgeSwipeRight by gestures.observe(GestureType.EDGE_SWIPE_RIGHT).collectAsState(initial = GestureType.EDGE_SWIPE_RIGHT.defaultAction)

    return GestureBindings(doubleTap, swipeUp, swipeDown, pinchIn, twoFingerTap, edgeSwipeLeft, edgeSwipeRight)
}
