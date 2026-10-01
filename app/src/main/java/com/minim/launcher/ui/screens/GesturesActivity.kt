package com.minim.launcher.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.MinimApplication
import com.minim.launcher.data.AppInfo
import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.ui.components.AdaptiveSurface
import com.minim.launcher.ui.theme.AccentOptions
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
import com.minim.launcher.ui.theme.MinimTheme
import com.minim.launcher.ui.theme.MinimThemeMode
import com.minim.launcher.util.GestureAction
import com.minim.launcher.util.GestureType
import com.minim.launcher.util.WindowChromeController
import kotlinx.coroutines.launch

class GesturesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MinimApplication
        val gestures = app.gestureRepository
        val settings = app.settingsRepository
        val scope = lifecycleScope

        setContent {
            val bindings by gestures.observeAll().collectAsState(
                initial = GestureType.entries.associateWith { it.defaultAction }
            )
            val allApps by app.appRepository.observeApps().collectAsState(initial = emptyList())
            val allSpaces by app.spacesRepository.observeSpaces().collectAsState(initial = emptyList())
            val designLanguageStr by settings.designLanguage.collectAsState(initial = "nothing")
            val themeModeStr by settings.themeMode.collectAsState(initial = "system")
            val dynamicColor by settings.dynamicColor.collectAsState(initial = false)
            val accentName by settings.accentName.collectAsState(initial = "Red")

            var editingType by remember { mutableStateOf<GestureType?>(null) }
            var pickingAppFor by remember { mutableStateOf<GestureType?>(null) }
            var pickingProfileFor by remember { mutableStateOf<GestureType?>(null) }

            val designLanguage = DesignLanguage.fromRaw(designLanguageStr)
            val themeMode = MinimThemeMode.fromRaw(themeModeStr)
            val isDark = when (themeMode) {
                MinimThemeMode.SYSTEM -> isSystemInDarkTheme()
                MinimThemeMode.LIGHT -> false
                MinimThemeMode.DARK -> true
            }

            LaunchedEffect(designLanguage, isDark) {
                WindowChromeController.apply(this@GesturesActivity, designLanguage, isDark)
            }

            MinimTheme(
                designLanguage = designLanguage,
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                accentColor = AccentOptions[accentName] ?: AccentOptions.getValue("Red")
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (designLanguage == DesignLanguage.GLASS) Color.Transparent else MaterialTheme.colorScheme.background
                ) {
                    Column {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Text(
                                    "Gestures",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (designLanguage == DesignLanguage.GLASS) FontWeight.Bold else null
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                        Text(
                            "Assign what each gesture does anywhere on the home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            item {
                                AdaptiveSurface {
                                    Column {
                                        val gestureTypes = GestureType.entries.toList()
                                        gestureTypes.forEachIndexed { index, type ->
                                            GestureRow(
                                                type = type,
                                                action = bindings[type] ?: type.defaultAction,
                                                apps = allApps,
                                                spaces = allSpaces,
                                                onClick = { editingType = type }
                                            )
                                            if (index < gestureTypes.lastIndex) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                    thickness = 0.5.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    editingType?.let { type ->
                        GestureCategoryPickerSheet(
                            onDismiss = { editingType = null },
                            onPickSimple = { action ->
                                scope.launch { gestures.set(type, action) }
                                editingType = null
                            },
                            onPickApp = { pickingAppFor = type; editingType = null },
                            onPickProfile = { pickingProfileFor = type; editingType = null }
                        )
                    }

                    pickingAppFor?.let { type ->
                        AppPickerSheet(
                            apps = allApps,
                            onPick = { app ->
                                scope.launch { gestures.set(type, GestureAction.OpenApp(app.packageName)) }
                                pickingAppFor = null
                            },
                            onDismiss = { pickingAppFor = null }
                        )
                    }

                    pickingProfileFor?.let { type ->
                        ProfilePickerForGestureSheet(
                            spaces = allSpaces,
                            onPick = { spaceId ->
                                scope.launch { gestures.set(type, GestureAction.ActivateProfile(spaceId)) }
                                pickingProfileFor = null
                            },
                            onDismiss = { pickingProfileFor = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureRow(
    type: GestureType,
    action: GestureAction,
    apps: List<AppInfo>,
    spaces: List<SpaceEntity>,
    onClick: () -> Unit
) {
    val subtitle = when (action) {
        GestureAction.None -> "Not assigned"
        GestureAction.Lock -> "Lock screen"
        GestureAction.OpenSettings -> "Open Settings"
        GestureAction.ExpandNotifications -> "Expand notifications (best effort)"
        GestureAction.ToggleTorch -> "Toggle torch"
        is GestureAction.OpenApp -> apps.firstOrNull { it.packageName == action.packageName }?.label
            ?: "Open app"
        is GestureAction.ActivateProfile -> action.spaceId?.let { id ->
            "Activate " + (spaces.firstOrNull { it.id == id }?.name ?: "profile")
        } ?: "Return to automatic/default profile"
    }

    val isGlass = LocalDesignLanguage.current == DesignLanguage.GLASS

    val textColor = MaterialTheme.colorScheme.onBackground
    val subtitleColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
    val titleStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = if (isGlass) FontWeight.Bold else null
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                type.label,
                style = titleStyle,
                color = textColor
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureCategoryPickerSheet(
    onDismiss: () -> Unit,
    onPickSimple: (GestureAction) -> Unit,
    onPickApp: () -> Unit,
    onPickProfile: () -> Unit
) {
    val isGlass = LocalDesignLanguage.current == DesignLanguage.GLASS
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        val content = @Composable {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                listOf(
                    "Not assigned" to { onPickSimple(GestureAction.None) },
                    "Lock screen" to { onPickSimple(GestureAction.Lock) },
                    "Open Settings" to { onPickSimple(GestureAction.OpenSettings) },
                    "Expand notifications" to { onPickSimple(GestureAction.ExpandNotifications) },
                    "Toggle torch" to { onPickSimple(GestureAction.ToggleTorch) },
                    "Open an app…" to onPickApp,
                    "Activate a profile…" to onPickProfile
                ).forEach { (label, action) ->
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action() }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        }
        if (isGlass) {
            AdaptiveSurface { content() }
        } else {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(apps: List<AppInfo>, onPick: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    val isGlass = LocalDesignLanguage.current == DesignLanguage.GLASS
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        val content = @Composable {
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(app) }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        }
        if (isGlass) {
            AdaptiveSurface { content() }
        } else {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePickerForGestureSheet(
    spaces: List<SpaceEntity>,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val isGlass = LocalDesignLanguage.current == DesignLanguage.GLASS
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        val content = @Composable {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Automatic / default",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(null) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
                spaces.forEach { space ->
                    Text(
                        space.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(space.id) }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        }
        if (isGlass) {
            AdaptiveSurface { content() }
        } else {
            content()
        }
    }
}
