package com.minim.launcher.ui.overlays

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo
import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.ui.screens.AppContextSheet
import com.minim.launcher.ui.screens.ProfileSwitcherSheet
import com.minim.launcher.ui.screens.QuickActionPickerSheet
import com.minim.launcher.ui.screens.SpacePickerSheet

/**
 * Everything that floats on top of the home screen — the four contextual
 * bottom sheets and the settings entry point — bundled into one composable
 * so MainActivity's Box content is "HomeScreen() + HomeOverlays()" instead
 * of ~150 lines of inline sheet wiring. An extension on BoxScope since the
 * settings button needs Modifier.align, and this is always called from
 * inside the same root Box as HomeScreen.
 */
@Composable
fun BoxScope.HomeOverlays(
    contextMenuApp: AppInfo?,
    onDismissContextMenu: () -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onHide: (AppInfo) -> Unit,
    onRequestQuickAction: (AppInfo) -> Unit,
    onRequestSpacePicker: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,

    quickActionPickerApp: AppInfo?,
    onDismissQuickActionPicker: () -> Unit,
    onSaveQuickAction: (AppInfo, String?) -> Unit,

    spacePickerApp: AppInfo?,
    spaces: List<SpaceEntity>,
    onDismissSpacePicker: () -> Unit,
    onAddToSpace: (AppInfo, String) -> Unit,
    onCreateAndAddToSpace: (AppInfo, String) -> Unit,

    showProfileSwitcher: Boolean,
    activeSpaceId: String,
    autoActivateEnabled: Boolean,
    onSelectProfile: (String?) -> Unit,
    onManageSpaces: () -> Unit,
    onDismissProfileSwitcher: () -> Unit,

    onOpenSettings: () -> Unit
) {
    contextMenuApp?.let { target ->
        AppContextSheet(
            app = target,
            onDismiss = onDismissContextMenu,
            onToggleFavorite = { onToggleFavorite(target) },
            onHide = { onHide(target) },
            onSetQuickAction = { onRequestQuickAction(target) },
            onAddToSpace = { onRequestSpacePicker(target) },
            onUninstall = { onUninstall(target) }
        )
    }

    quickActionPickerApp?.let { target ->
        QuickActionPickerSheet(
            app = target,
            onDismiss = onDismissQuickActionPicker,
            onSave = { action -> onSaveQuickAction(target, action) }
        )
    }

    spacePickerApp?.let { target ->
        SpacePickerSheet(
            app = target,
            spaces = spaces,
            onAddToSpace = { spaceId -> onAddToSpace(target, spaceId) },
            onCreateAndAdd = { name -> onCreateAndAddToSpace(target, name) },
            onDismiss = onDismissSpacePicker
        )
    }

    if (showProfileSwitcher) {
        ProfileSwitcherSheet(
            spaces = spaces,
            activeSpaceId = activeSpaceId,
            autoActivateEnabled = autoActivateEnabled,
            onSelect = onSelectProfile,
            onManageSpaces = onManageSpaces,
            onDismiss = onDismissProfileSwitcher
        )
    }

    // Deliberately understated — a launcher's settings entry point
    // shouldn't compete visually with the app list.
    IconButton(
        onClick = onOpenSettings,
        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
        )
    }
}
