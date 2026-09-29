package com.minim.launcher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class QuickToggle(
    val icon: ImageVector,
    val contentDescription: String,
    val active: Boolean,
    val onClick: () -> Unit
)

/**
 * A single row of the handful of toggles people reach for dozens of times a
 * day. Not a full quick-settings panel clone — most radios can't be flipped
 * directly by a third-party app on modern Android, so each icon is a fast
 * shortcut to the real system panel (see SystemToggleController).
 */
@Composable
fun QuickSettingsRow(
    toggles: List<QuickToggle>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        toggles.forEach { toggle ->
            IconButton(
                onClick = toggle.onClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = toggle.icon,
                    contentDescription = toggle.contentDescription,
                    modifier = Modifier.size(20.dp),
                    tint = if (toggle.active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    }
                )
            }
        }
    }
}
