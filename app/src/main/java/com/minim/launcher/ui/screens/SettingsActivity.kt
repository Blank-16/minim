package com.minim.launcher.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.MinimApplication
import com.minim.launcher.ui.theme.AccentOptions
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
import com.minim.launcher.ui.theme.MinimTheme
import com.minim.launcher.ui.theme.MinimThemeMode
import com.minim.launcher.util.CalendarPeek
import com.minim.launcher.util.WindowChromeController
import kotlinx.coroutines.launch

private val LocalIsDark = compositionLocalOf { false }

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
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
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

            val isDark = when (themeMode) {
                MinimThemeMode.SYSTEM -> isSystemInDarkTheme()
                MinimThemeMode.LIGHT -> false
                MinimThemeMode.DARK -> true
            }

            LaunchedEffect(designLanguage, isDark) {
                WindowChromeController.apply(
                    this@SettingsActivity,
                    designLanguage,
                    isDark
                )
            }

            CompositionLocalProvider(LocalIsDark provides isDark) {
                MinimTheme(
                    designLanguage = designLanguage,
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    accentColor = AccentOptions[accentName] ?: AccentOptions.getValue("Red")
                ) {
                    val surfaceColor = if (designLanguage == DesignLanguage.GLASS) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.background
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = surfaceColor
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(
                                title = {
                                    Text(
                                        "Settings",
                                        color = if (designLanguage == DesignLanguage.GLASS) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (designLanguage == DesignLanguage.GLASS) FontWeight.Bold else null
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            Icons.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = if (designLanguage == DesignLanguage.GLASS) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
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
                                    SettingsCard(
                                        title = "Hidden apps",
                                        subtitle = "View and unhide apps you've hidden from the list",
                                        onClick = {
                                            startActivity(Intent(this@SettingsActivity, HiddenAppsActivity::class.java))
                                        }
                                    )
                                }
                                item {
                                    SettingsCard(
                                        title = "Gestures",
                                        subtitle = "Assign double-tap, swipe, pinch, and edge-swipe actions",
                                        onClick = {
                                            startActivity(Intent(this@SettingsActivity, GesturesActivity::class.java))
                                        }
                                    )
                                }
                                item {
                                    SettingsCard(
                                        title = "Spaces & profiles",
                                        subtitle = "Group apps and give a Space its own look",
                                        onClick = {
                                            startActivity(Intent(this@SettingsActivity, SpacesActivity::class.java))
                                        }
                                    )
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
}

@Composable
private fun SettingsHeader(text: String) {
    val designLanguage = LocalDesignLanguage.current
    val isGlass = designLanguage == DesignLanguage.GLASS

    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (isGlass) FontWeight.Bold else null,
        color = if (isGlass) Color.White else MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun DesignLanguageSelector(current: DesignLanguage, onSelect: (DesignLanguage) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        SettingsHeader("Design")
        Spacer(modifier = Modifier.height(4.dp))
        val bodyMedium = MaterialTheme.typography.bodyMedium
        val subtextStyle = bodyMedium.copy(fontSize = (bodyMedium.fontSize.value - 2).sp)
        val designLanguage = LocalDesignLanguage.current
        val isGlass = designLanguage == DesignLanguage.GLASS
        val subtextColor = if (isGlass) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

        Text(
            "Each option changes color, shape, and type together — not just a tint",
            style = subtextStyle,
            color = subtextColor
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
    val designLanguage = LocalDesignLanguage.current
    val isGlass = designLanguage == DesignLanguage.GLASS

    val titleColor = if (isGlass) Color.White else MaterialTheme.colorScheme.onBackground
    val titleWeight = if (isGlass) FontWeight.Bold else null
    val subtextStyle = MaterialTheme.typography.bodyMedium
    val subtextColor = if (isGlass) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    val optionBgColor = if (isGlass) {
        if (selected) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
    } else {
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(optionBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = if (isGlass) RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.6f)
            ) else RadioButtonDefaults.colors()
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = titleWeight, color = titleColor)
            Text(
                subtitle,
                style = subtextStyle,
                color = subtextColor
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(current: MinimThemeMode, onSelect: (MinimThemeMode) -> Unit) {
    val isGlass = LocalDesignLanguage.current == DesignLanguage.GLASS
    val chipColors = if (isGlass) FilterChipDefaults.filterChipColors(
        containerColor = Color.White.copy(alpha = 0.12f),
        labelColor = Color.White.copy(alpha = 0.8f),
        selectedContainerColor = Color.White.copy(alpha = 0.35f),
        selectedLabelColor = Color.White
    ) else FilterChipDefaults.filterChipColors()

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        SettingsHeader("Light / dark")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MinimThemeMode.SYSTEM to "System", MinimThemeMode.LIGHT to "Light", MinimThemeMode.DARK to "Dark")
                .forEach { (mode, label) ->
                    FilterChip(
                        selected = current == mode,
                        onClick = { onSelect(mode) },
                        label = { Text(label) },
                        colors = chipColors
                    )
                }
        }
    }
}

@Composable
private fun AccentPicker(current: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        SettingsHeader("Accent color")
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
private fun SettingsCard(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val designLanguage = LocalDesignLanguage.current
    val shape = MaterialTheme.shapes.medium
    val isDark = LocalIsDark.current
    val isGlass = designLanguage == DesignLanguage.GLASS

    var modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .clip(shape)

    if (isGlass) {
        modifier = modifier.background(Color.White.copy(alpha = 0.16f))
    } else if (!isDark) {
        val borderColor = when (designLanguage) {
            DesignLanguage.ANDROID_16 -> MaterialTheme.colorScheme.primary
            DesignLanguage.NOTHING -> Color(0xFFFF6B6B).copy(alpha = 0.7f)
            else -> Color.Transparent
        }
        modifier = modifier
            .background(Color.Transparent)
            .border(1.dp, borderColor, shape)
    } else {
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
        if (designLanguage == DesignLanguage.NOTHING) {
            modifier = modifier.border(1.dp, Color.White, shape)
        }
    }

    if (onClick != null) {
        modifier = modifier.clickable(onClick = onClick)
    }

    modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp)

    val titleColor = if (isGlass) Color.White else MaterialTheme.colorScheme.onBackground
    val titleWeight = if (isGlass) FontWeight.Bold else null
    val subtextStyle = MaterialTheme.typography.bodyMedium
    val subtextColor = if (isGlass) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = titleWeight,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = subtextStyle,
                color = subtextColor
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun BackupRow(onExport: () -> Unit) {
    SettingsCard(
        title = "Export settings",
        subtitle = "Save your setup as a file so it's never trapped on one device",
        onClick = onExport
    )
}

@Composable
private fun IconShapeSelector(current: String, onSelect: (String) -> Unit) {
    val isGlass = LocalDesignLanguage.current == DesignLanguage.GLASS
    val chipColors = if (isGlass) FilterChipDefaults.filterChipColors(
        containerColor = Color.White.copy(alpha = 0.12f),
        labelColor = Color.White.copy(alpha = 0.8f),
        selectedContainerColor = Color.White.copy(alpha = 0.35f),
        selectedLabelColor = Color.White
    ) else FilterChipDefaults.filterChipColors()

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        SettingsHeader("Icon shape")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("circle" to "Circle", "squircle" to "Squircle", "roundedSquare" to "Rounded square")
                .forEach { (value, label) ->
                    FilterChip(
                        selected = current == value,
                        onClick = { onSelect(value) },
                        label = { Text(label) },
                        colors = chipColors
                    )
                }
        }
    }
}

@Composable
private fun NotificationBadgesRow(listenerGranted: Boolean, onOpenSystemSettings: () -> Unit) {
    SettingsCard(
        title = "Notification badge access",
        subtitle = if (listenerGranted) {
            "Granted — dot badges will show on apps with notifications"
        } else {
            "Not granted — tap to open system settings and allow access"
        },
        onClick = onOpenSystemSettings
    )
}

@Composable
private fun NothingSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val designLanguage = LocalDesignLanguage.current
    if (designLanguage == DesignLanguage.NOTHING) {
        val thumbOffset by animateDpAsState(
            targetValue = if (checked) 24.dp else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "switchThumb"
        )
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onCheckedChange(!checked) }
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color.White else MaterialTheme.colorScheme.outline)
            )
        }
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = if (designLanguage == DesignLanguage.GLASS) SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0A84FF)
            ) else SwitchDefaults.colors()
        )
    }
}

@Composable
private fun SettingsToggleRow(item: ToggleItem) {
    SettingsCard(
        title = item.title,
        subtitle = item.subtitle,
        trailing = {
            NothingSwitch(checked = item.checked, onCheckedChange = item.onToggle)
        }
    )
}
