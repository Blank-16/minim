package com.minim.launcher.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.MinimApplication
import com.minim.launcher.data.AppInfo
import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.MinimTheme
import com.minim.launcher.util.GestureAction
import com.minim.launcher.util.GestureType
import kotlinx.coroutines.launch

class GesturesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MinimApplication
        val gestures = app.gestureRepository
        val scope = lifecycleScope

        setContent {
            val bindings by gestures.observeAll().collectAsState(
                initial = GestureType.entries.associateWith { it.defaultAction }
            )
            val allApps by app.appRepository.observeApps().collectAsState(initial = emptyList())
            val allSpaces by app.spacesRepository.observeSpaces().collectAsState(initial = emptyList())
            val designLanguageStr by app.settingsRepository.designLanguage.collectAsState(initial = "nothing")

            var editingType by remember { mutableStateOf<GestureType?>(null) }
            var pickingAppFor by remember { mutableStateOf<GestureType?>(null) }
            var pickingProfileFor by remember { mutableStateOf<GestureType?>(null) }

            MinimTheme(
                designLanguage = DesignLanguage.fromRaw(designLanguageStr)
            ) {
                Surface {
                    Column {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(title = { Text("Gestures") })
                        Text(
                            "Assign what each gesture does anywhere on the home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        LazyColumn {
                            items(GestureType.entries.toList()) { type ->
                                GestureRow(
                                    type = type,
                                    action = bindings[type] ?: type.defaultAction,
                                    apps = allApps,
                                    spaces = allSpaces,
                                    onClick = { editingType = type }
                                )
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(type.label, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { action() }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(apps: List<AppInfo>, onPick: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(app) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Automatic / default",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(null) }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )
            spaces.forEach { space ->
                Text(
                    space.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(space.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}
