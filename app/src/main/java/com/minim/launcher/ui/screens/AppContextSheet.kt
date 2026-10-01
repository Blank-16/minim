package com.minim.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo
import com.minim.launcher.ui.components.AdaptiveSurface
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onSetQuickAction: () -> Unit,
    onAddToSpace: () -> Unit,
    onUninstall: () -> Unit
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
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isGlass) Color.Black else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                ContextSheetRow(
                    icon = if (app.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    label = if (app.favorite) "Remove from favorites" else "Add to favorites",
                    isGlass = isGlass,
                    onClick = { onToggleFavorite(); onDismiss() }
                )
                ContextSheetRow(
                    icon = Icons.Filled.TouchApp,
                    label = "Set swipe quick action",
                    isGlass = isGlass,
                    onClick = { onSetQuickAction(); onDismiss() }
                )
                ContextSheetRow(
                    icon = Icons.Filled.Widgets,
                    label = "Add to a space",
                    isGlass = isGlass,
                    onClick = { onAddToSpace(); onDismiss() }
                )
                ContextSheetRow(
                    icon = Icons.Filled.VisibilityOff,
                    label = "Hide from app list",
                    isGlass = isGlass,
                    onClick = { onHide(); onDismiss() }
                )
                ContextSheetRow(
                    icon = Icons.Filled.Delete,
                    label = "Uninstall",
                    destructive = true,
                    isGlass = isGlass,
                    onClick = { onUninstall(); onDismiss() }
                )
            }
        }

        if (isGlass) {
            AdaptiveSurface {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun ContextSheetRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    isGlass: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) MaterialTheme.colorScheme.error else if (isGlass) Color.Black else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            color = if (destructive) MaterialTheme.colorScheme.error else if (isGlass) Color.Black else MaterialTheme.colorScheme.onSurface
        )
    }
}
