package com.minim.launcher.ui.screens

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.MinimApplication
import com.minim.launcher.ui.theme.AccentOptions
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.MinimTheme
import com.minim.launcher.ui.theme.MinimThemeMode
import com.minim.launcher.util.CalendarPeek
import kotlinx.coroutines.launch

private data class ToggleItem(
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onToggle: (Boolean) -> Unit
)

class SettingsActivity : ComponentActivity() {

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // If denied, flip the setting back off rather than leaving it on
        // with no data behind it.
        if (!granted) {
            lifecycleScope.launch { (application as MinimApplication).settingsRepository.setShowCalendarPeek(false) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MinimApplication
        val settings = app.settingsRepository
        val scope = lifecycleScope

        setContent {
            val showIcons by settings.showIcons.collectAsState(initial = false)
            val showSuggestions by settings.showSuggestions.collectAsState(initial = true)
            val locationSuggestions by settings.locationSuggestions.collectAsState(initial = false)
            val dotBadges by settings.dotBadges.collectAsState(initial = true)
            val hapticFeedback by settings.hapticFeedback.collectAsState(initial = true)
            val showClock by settings.showClock.collectAsState(initial = true)
            val showQuickSettings by settings.showQuickSettings.collectAsState(initial = true)
            val themeModeStr by settings.themeMode.collectAsState(initial = "system")
            val designLanguageStr by settings.designLanguage.collectAsState(initial = "nothing")
            val dynamicColor by settings.dynamicColor.collectAsState(initial = false)
            val accentName by settings.accentName.collectAsState(initial = "Red")
            val iconShape by settings.iconShape.collectAsState(initial = "circle")
            val showCalendarPeek by settings.showCalendarPeek.collectAsState(initial = false)
            val notificationBadgesEnabled by settings.notificationBadgesEnabled.collectAsState(initial = false)
            val hiddenAppsLocked by settings.hiddenAppsLocked.collectAsState(initial = false)
            val localContext = LocalContext.current

            val designLanguage = DesignLanguage.fromRaw(designLanguageStr)
            val themeMode = MinimThemeMode.fromRaw(themeModeStr)

            val toggles = listOf(
                ToggleItem("Show icons", "Off keeps the list text-only and lighter on battery", showIcons) {
                    scope.launch { settings.setShowIcons(it) }
                },
                ToggleItem("Smart suggestions", "On-device only — frequency and time of day, no cloud", showSuggestions) {
                    scope.launch { settings.setShowSuggestions(it) }
                },
                ToggleItem("Location-aware suggestions", "Uses coarse location only; requires the permission", locationSuggestions) {
                    scope.launch { settings.setLocationSuggestions(it) }
                },
                ToggleItem("Home screen clock", "Live clock and date above the app list", showClock) {
                    scope.launch { settings.setShowClock(it) }
                },
                ToggleItem("Quick settings row", "Torch, Wi-Fi, Bluetooth, and DND shortcuts", showQuickSettings) {
                    scope.launch { settings.setShowQuickSettings(it) }
                },
                ToggleItem("Haptic feedback", "Short tick on tap, long-press, and swipe actions", hapticFeedback) {
                    scope.launch { settings.setHapticFeedback(it) }
                },
                ToggleItem("Dot badges", "Show a small dot instead of a numeric count", dotBadges) {
                    scope.launch { settings.setDotBadges(it) }
                },
                ToggleItem(
                    "Show next calendar event",
                    "On-device only, from your calendar app — never uploaded anywhere",
                    showCalendarPeek
                ) { turnOn ->
                    scope.launch { settings.setShowCalendarPeek(turnOn) }
                    if (turnOn && !CalendarPeek.hasPermission(localContext)) {
                        calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                    }
                },
                ToggleItem(
                    "Lock hidden apps",
                    "Require your device's screen lock (biometric/PIN) to view hidden apps",
                    hiddenAppsLocked
                ) { scope.launch { settings.setHiddenAppsLocked(it) } },
                ToggleItem(
                    "Show notification badges",
                    "Requires the notification badge access granted below",
                    notificationBadgesEnabled
                ) { scope.launch { settings.setNotificationBadgesEnabled(it) } }
            )

            MinimTheme(
                designLanguage = designLanguage,
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                accentColor = AccentOptions[accentName] ?: AccentOptions.getValue("Red")
            ) {
                Surface {
                    Column {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(title = { Text("Settings") })
                        LazyColumn {
                            item {
                                DesignLanguageSelector(
                                    current = designLanguage,
                                    onSelect = { lang ->
                                        scope.launch {
                                            settings.setDesignLanguage(lang.toRaw())
                                        }
                                    }
                                )
                            }
                            item {
                                ThemeModeSelector(
                                    current = themeMode,
                                    onSelect = { mode ->
                                        scope.launch {
                                            settings.setThemeMode(mode.toRaw())
                                        }
                                    }
                                )
                            }
                            if (designLanguage == DesignLanguage.ANDROID_16 && !dynamicColor) {
                                item {
                                    AccentPicker(
                                        current = accentName,
                                        onSelect = { name -> scope.launch { settings.setAccentName(name) } }
                                    )
                                }
                            }
                            if (designLanguage == DesignLanguage.ANDROID_16) {
                                item {
                                    ToggleItem(
                                        "Match wallpaper colors",
                                        "Material You dynamic color (Android 12+); overrides the accent above",
                                        dynamicColor
                                    ) { scope.launch { settings.setDynamicColor(it) } }.let { SettingsToggleRow(it) }
                                }
                            }
                            item {
                                IconShapeSelector(
                                    current = iconShape,
                                    onSelect = { shape -> scope.launch { settings.setIconShape(shape) } }
                                )
                            }
                            items(toggles) { item -> SettingsToggleRow(item) }
                            item {
                                NotificationBadgesRow(
                                    listenerGranted = NotificationManagerCompat.getEnabledListenerPackages(localContext)
                                        .contains(localContext.packageName),
                                    onOpenSystemSettings = {
                                        startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    }
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            startActivity(Intent(this@SettingsActivity, HiddenAppsActivity::class.java))
                                        }
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Hidden apps", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "View and unhide apps you've hidden from the list",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            startActivity(Intent(this@SettingsActivity, GesturesActivity::class.java))
                                        }
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Gestures", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Assign double-tap, swipe, pinch, and edge-swipe actions",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            startActivity(Intent(this@SettingsActivity, SpacesActivity::class.java))
                                        }
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Spaces & profiles", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Group apps and give a Space its own look",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            item {
                                val autoActivate by settings.autoActivateProfiles.collectAsState(initial = true)
                                SettingsToggleRow(
                                    ToggleItem(
                                        "Auto-activate profiles",
                                        "Switch to a Space's look automatically during its time window",
                                        autoActivate
                                    ) { scope.launch { settings.setAutoActivateProfiles(it) } }
                                )
                            }
                            item {
                                BackupRow(
                                    onExport = {
                                        scope.launch {
                                            val json = settings.exportToJson()
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_TEXT, json)
                                            }
                                            startActivity(Intent.createChooser(sendIntent, "Export Minim settings"))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignLanguageSelector(current: DesignLanguage, onSelect: (DesignLanguage) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Design", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Each option changes color, shape, and type together — not just a tint",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignLanguageOption(
                title = "Nothing OS",
                subtitle = "Monochrome + red, dot-matrix type, sharp edges",
                selected = current == DesignLanguage.NOTHING,
                onClick = { onSelect(DesignLanguage.NOTHING) }
            )
            DesignLanguageOption(
                title = "Android 16",
                subtitle = "Material 3 Expressive — bold color, large rounded shapes",
                selected = current == DesignLanguage.ANDROID_16,
                onClick = { onSelect(DesignLanguage.ANDROID_16) }
            )
            DesignLanguageOption(
                title = "iOS 26 Glass",
                subtitle = "Frosted translucent panels over your live wallpaper",
                selected = current == DesignLanguage.GLASS,
                onClick = { onSelect(DesignLanguage.GLASS) }
            )
        }
    }
}

@Composable
private fun DesignLanguageOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(current: MinimThemeMode, onSelect: (MinimThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Light / dark", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MinimThemeMode.SYSTEM to "System", MinimThemeMode.LIGHT to "Light", MinimThemeMode.DARK to "Dark")
                .forEach { (mode, label) ->
                    FilterChip(selected = current == mode, onClick = { onSelect(mode) }, label = { Text(label) })
                }
        }
    }
}

@Composable
private fun AccentPicker(current: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Accent color", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentOptions.forEach { (name, color) ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (name == current) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        )
                        .clickable { onSelect(name) }
                )
            }
        }
    }
}

@Composable
private fun BackupRow(onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExport)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Export settings", style = MaterialTheme.typography.titleMedium)
            Text(
                "Save your setup as a file so it's never trapped on one device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun IconShapeSelector(current: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Icon shape", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("circle" to "Circle", "squircle" to "Squircle", "roundedSquare" to "Rounded square")
                .forEach { (value, label) ->
                    FilterChip(selected = current == value, onClick = { onSelect(value) }, label = { Text(label) })
                }
        }
    }
}

@Composable
private fun NotificationBadgesRow(listenerGranted: Boolean, onOpenSystemSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSystemSettings)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Notification badge access", style = MaterialTheme.typography.titleMedium)
            Text(
                if (listenerGranted) {
                    "Granted — dot badges will show on apps with notifications"
                } else {
                    "Not granted — tap to open system settings and allow access"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(item: ToggleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = item.checked, onCheckedChange = item.onToggle)
    }
}
