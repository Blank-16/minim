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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.ui.components.AdaptiveSurface
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage

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
    val language = LocalDesignLanguage.current
    val isGlass = language == DesignLanguage.GLASS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        val content = @Composable {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Profiles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isGlass) Color.Black else MaterialTheme.colorScheme.onSurface,
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
                    isGlass = isGlass,
                    onClick = { onSelect(null); onDismiss() }
                )

                spaces.forEach { space ->
                    ProfileRow(
                        title = space.name,
                        subtitle = space.designLanguage?.let { "Custom look" } ?: "Uses default look",
                        icon = null,
                        selected = space.id == activeSpaceId,
                        isGlass = isGlass,
                        onClick = { onSelect(space.id); onDismiss() }
                    )
                }

                TextButton(
                    onClick = { onManageSpaces(); onDismiss() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Manage spaces", color = if (isGlass) Color.Black else MaterialTheme.colorScheme.primary)
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

@Composable
private fun ProfileRow(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    selected: Boolean,
    isGlass: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isGlass) Color.Black else MaterialTheme.colorScheme.onSurface
    val subtextColor = if (isGlass) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtextColor
            )
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Active", tint = if (isGlass) Color.Black else MaterialTheme.colorScheme.primary)
        }
    }
}
