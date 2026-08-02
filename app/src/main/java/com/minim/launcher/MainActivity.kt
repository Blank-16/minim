package com.minim.launcher

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.data.AppInfo
import com.minim.launcher.service.MinimNotificationListenerService
import com.minim.launcher.ui.components.QuickActionType
import com.minim.launcher.ui.components.QuickToggle
import com.minim.launcher.ui.overlays.HomeOverlays
import com.minim.launcher.ui.screens.HomeScreen
import com.minim.launcher.ui.screens.SettingsActivity
import com.minim.launcher.ui.screens.SpacesActivity
import com.minim.launcher.ui.state.rememberCalendarPeekText
import com.minim.launcher.ui.state.rememberGestureBindings
import com.minim.launcher.ui.state.rememberMainScreenSettings
import com.minim.launcher.ui.theme.AccentOptions
import com.minim.launcher.ui.theme.MinimTheme
import com.minim.launcher.ui.theme.MinimThemeMode
import com.minim.launcher.util.AppearanceResolver
import com.minim.launcher.util.GestureDispatcher
import com.minim.launcher.util.SystemToggleController
import com.minim.launcher.util.WidgetHostManager
import com.minim.launcher.util.WidgetPickerController
import com.minim.launcher.util.WindowChromeController
import com.minim.launcher.util.detectAllGestures
import com.minim.launcher.viewmodel.MainViewModel
import com.minim.launcher.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Single Activity, launchMode="singleTask", stateNotNeeded="true" (manifest)
 * — a launcher is torn down/recreated far more than a normal app, so state
 * lives in the ViewModel/Flow layer, not Activity fields.
 *
 * This class is intentionally thin: it wires Android-framework ceremony
 * (activity results, lifecycle callbacks, intents) to plain collaborators —
 * GestureDispatcher, WidgetPickerController, AppearanceResolver,
 * SystemToggleController — and to Composables that own their own state
 * (rememberMainScreenSettings, rememberGestureBindings,
 * rememberCalendarPeekText, HomeOverlays). None of those collaborators know
 * this Activity exists, which is what makes them independently testable/
 * reusable instead of being trapped in one god file.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(application as MinimApplication)
    }

    private lateinit var systemToggles: SystemToggleController
    private lateinit var widgetHostManager: WidgetHostManager
    private lateinit var widgetPicker: WidgetPickerController
    private lateinit var gestureDispatcher: GestureDispatcher

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onStart() {
        super.onStart()
        widgetHostManager.startListening()
    }

    override fun onStop() {
        widgetHostManager.stopListening()
        super.onStop()
    }

    // Requested only when the user turns on "Show next calendar event" in
    // Settings; the toggle itself reverts on denial (see SettingsActivity).
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* SettingsActivity reads CalendarPeek.hasPermission on its own next check */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = application as MinimApplication
        systemToggles = SystemToggleController(this)
        widgetHostManager = WidgetHostManager(this)
        widgetPicker = WidgetPickerController(this, widgetHostManager) { appWidgetId ->
            lifecycleScope.launch { app.settingsRepository.setWidgetAppWidgetId(appWidgetId) }
        }
        gestureDispatcher = GestureDispatcher(
            onLock = ::lockScreen,
            onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
            onExpandNotifications = ::promptNotificationShade,
            onToggleTorch = { systemToggles.toggleTorch() },
            onOpenApp = ::launchAppByPackage,
            onActivateProfile = { spaceId -> viewModel.activateProfile(spaceId) }
        )

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val screenSettings = rememberMainScreenSettings(app.settingsRepository)
            val gestures = rememberGestureBindings(app.gestureRepository)
            val activeProfile by viewModel.activeProfile.collectAsState()
            val allSpaces by app.spacesRepository.observeSpaces().collectAsState(initial = emptyList())
            val notificationPackages by MinimNotificationListenerService.activePackages.collectAsState()
            val calendarPeekText = rememberCalendarPeekText(screenSettings.showCalendarPeek)

            val appearance = AppearanceResolver.resolve(
                activeProfile = activeProfile,
                globalDesignLanguageRaw = screenSettings.designLanguageRaw,
                globalAccentName = screenSettings.accentName,
                globalThemeModeRaw = screenSettings.themeModeRaw
            )

            LaunchedEffect(appearance.designLanguage) {
                WindowChromeController.apply(this@MainActivity, appearance.designLanguage)
            }

            var torchActive by remember { mutableStateOf(false) }
            var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }
            var quickActionPickerApp by remember { mutableStateOf<AppInfo?>(null) }
            var spacePickerApp by remember { mutableStateOf<AppInfo?>(null) }
            var showProfileSwitcher by remember { mutableStateOf(false) }

            MinimTheme(
                designLanguage = appearance.designLanguage,
                themeMode = MinimThemeMode.fromRaw(appearance.themeModeRaw),
                dynamicColor = screenSettings.dynamicColor,
                accentColor = AccentOptions[appearance.accentName] ?: AccentOptions.getValue("Red")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(gestures) {
                            detectAllGestures(
                                onDoubleTap = { gestureDispatcher.execute(gestures.doubleTap) },
                                onSwipeUp = { gestureDispatcher.execute(gestures.swipeUp) },
                                onSwipeDown = { gestureDispatcher.execute(gestures.swipeDown) },
                                onPinchIn = { gestureDispatcher.execute(gestures.pinchIn) },
                                onTwoFingerTap = { gestureDispatcher.execute(gestures.twoFingerTap) },
                                // Edge naming follows the direction traveled, not
                                // the edge started from — see GestureType labels.
                                onEdgeSwipeFromLeftEdge = { gestureDispatcher.execute(gestures.edgeSwipeRight) },
                                onEdgeSwipeFromRightEdge = { gestureDispatcher.execute(gestures.edgeSwipeLeft) }
                            )
                        }
                ) {
                    HomeScreen(
                        state = uiState,
                        widgetCollapsed = screenSettings.widgetCollapsed,
                        showClock = screenSettings.showClock,
                        showQuickSettings = screenSettings.showQuickSettings,
                        activeProfileName = activeProfile?.name,
                        onClockLongClick = { showProfileSwitcher = true },
                        calendarPeekText = calendarPeekText,
                        appWidgetId = screenSettings.appWidgetId,
                        createWidgetHostView = { id -> widgetHostManager.createHostView(id) },
                        onAddWidget = widgetPicker::launchPicker,
                        onRemoveWidget = { widgetPicker.remove(screenSettings.appWidgetId) },
                        quickToggles = buildQuickToggles(torchActive) { torchActive = systemToggles.isTorchOn },
                        onToggleWidget = {
                            lifecycleScope.launch {
                                app.settingsRepository.setWidgetCollapsed(!screenSettings.widgetCollapsed)
                            }
                        },
                        onClockClick = ::openDefaultClockApp,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onAppClick = ::launchApp,
                        onAppLongClick = { contextMenuApp = it },
                        onQuickAction = ::runQuickAction,
                        quickActionFor = { info -> parseQuickActionType(info.quickAction) },
                        notificationPackages = if (screenSettings.notificationBadgesEnabled) notificationPackages else emptySet(),
                        iconLoader = { pkg -> app.iconLoader.load(pkg) }
                    )

                    HomeOverlays(
                        contextMenuApp = contextMenuApp,
                        onDismissContextMenu = { contextMenuApp = null },
                        onToggleFavorite = { target -> viewModel.onToggleFavorite(target.packageName, !target.favorite) },
                        onHide = { target -> viewModel.onHideApp(target.packageName, true) },
                        onRequestQuickAction = { target -> quickActionPickerApp = target },
                        onRequestSpacePicker = { target -> spacePickerApp = target },
                        onUninstall = { target -> requestUninstall(target.packageName) },

                        quickActionPickerApp = quickActionPickerApp,
                        onDismissQuickActionPicker = { quickActionPickerApp = null },
                        onSaveQuickAction = { target, action -> viewModel.onSetQuickAction(target.packageName, action) },

                        spacePickerApp = spacePickerApp,
                        spaces = allSpaces,
                        onDismissSpacePicker = { spacePickerApp = null },
                        onAddToSpace = { target, spaceId ->
                            lifecycleScope.launch { app.spacesRepository.addAppToSpace(spaceId, target.packageName) }
                        },
                        onCreateAndAddToSpace = { target, name ->
                            lifecycleScope.launch {
                                val id = app.spacesRepository.createSpace(name, allSpaces.size)
                                app.spacesRepository.addAppToSpace(id, target.packageName)
                            }
                        },

                        showProfileSwitcher = showProfileSwitcher,
                        activeSpaceId = screenSettings.activeSpaceId,
                        autoActivateEnabled = screenSettings.autoActivateEnabled,
                        onSelectProfile = { spaceId -> viewModel.activateProfile(spaceId) },
                        onManageSpaces = { startActivity(Intent(this@MainActivity, SpacesActivity::class.java)) },
                        onDismissProfileSwitcher = { showProfileSwitcher = false },

                        onOpenSettings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
                    )
                }
            }
        }
    }

    private fun buildQuickToggles(torchActive: Boolean, refreshTorch: () -> Unit): List<QuickToggle> = listOf(
        QuickToggle(Icons.Filled.FlashlightOn, "Torch", torchActive) {
            systemToggles.toggleTorch(); refreshTorch()
        },
        QuickToggle(Icons.Filled.Wifi, "Wi-Fi settings", false) { systemToggles.openWifiPanel() },
        QuickToggle(Icons.Filled.Bluetooth, "Bluetooth settings", false) { systemToggles.openBluetoothPanel() },
        QuickToggle(Icons.Filled.DoNotDisturb, "Do Not Disturb", systemToggles.isDndActive()) {
            systemToggles.openDndSettings()
        }
    )

    private fun launchApp(app: AppInfo) {
        val intent = Intent().apply {
            setClassName(app.packageName, app.activityClassName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
        viewModel.onAppLaunched(app.packageName)
    }

    private fun launchAppByPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return
        runCatching { startActivity(intent) }
        viewModel.onAppLaunched(packageName)
    }

    private fun parseQuickActionType(raw: String?): QuickActionType? = when {
        raw == null -> null
        raw.startsWith("call:") -> QuickActionType.CALL
        raw.startsWith("sms:") -> QuickActionType.MESSAGE
        raw == "open" -> QuickActionType.DEEP_LINK
        else -> null
    }

    private fun runQuickAction(app: AppInfo) {
        val raw = app.quickAction ?: return
        when {
            raw.startsWith("call:") -> {
                val number = raw.removePrefix("call:")
                runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
            }
            raw.startsWith("sms:") -> {
                val number = raw.removePrefix("sms:")
                runCatching { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))) }
            }
            raw == "open" -> launchApp(app)
        }
    }

    private fun requestUninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }
    }

    private fun openDefaultClockApp() {
        val intent = Intent("android.intent.action.SHOW_ALARMS")
        runCatching { startActivity(intent) }
    }

    private fun lockScreen() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        runCatching { dpm?.lockNow() }
    }

    private fun promptNotificationShade() {
        // Genuinely not reliably possible on modern Android without an
        // Accessibility Service (the old StatusBarManager reflection trick is
        // blocked on recent API levels). See README for the tradeoff.
    }
}
