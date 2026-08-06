package com.minim.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.db.SpaceEntity

/**
 * Quick manual switch between Contextual Profiles (Spaces that carry their
 * own design language/accent/theme). Opened by long-pressing the clock.
 * Picking a profile here always overrides time-window auto-activation until
 * "Use automatic / default" is picked again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(
    spaces: List<SpaceEntity>,
    activeSpaceId: String?,
    autoActivateEnabled: Boolean,
    onSelect: (String?) -> Unit,
    onManageSpaces: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Profiles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            ProfileRow(
                title = if (autoActivateEnabled) "Automatic" else "Default",
                subtitle = if (autoActivateEnabled) {
                    "Switches by time window automatically"
                } else {
                    "Always use the global Settings appearance"
                },
                icon = Icons.Filled.Schedule,
                selected = activeSpaceId.isNullOrEmpty(),
                onClick = { onSelect(null); onDismiss() }
            )

            spaces.forEach { space ->
                ProfileRow(
                    title = space.name,
                    subtitle = space.designLanguage?.let { "Custom look" } ?: "Uses default look",
                    icon = null,
                    selected = space.id == activeSpaceId,
                    onClick = { onSelect(space.id); onDismiss() }
                )
            }

            TextButton(
                onClick = { onManageSpaces(); onDismiss() },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Manage spaces")
            }
        }
    }
}

@Composable
private fun ProfileRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
