package com.minim.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage

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
    val language = LocalDesignLanguage.current
    val toggleShape = if (language == DesignLanguage.NOTHING) RoundedCornerShape(4.dp) else CircleShape

    AdaptiveSurface(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            toggles.forEach { toggle ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(toggleShape)
                        .background(
                            if (toggle.active) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            } else {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = toggle.onClick) {
                        Icon(
                            imageVector = toggle.icon,
                            contentDescription = toggle.contentDescription,
                            tint = if (toggle.active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
        }
    }
}
