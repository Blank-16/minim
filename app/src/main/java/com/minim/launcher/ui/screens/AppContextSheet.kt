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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo

/**
 * Long-press context menu. Every action here maps straight to a repository
 * call that already existed before this pass (setFavorite/setHidden/
 * setQuickAction) — this sheet is what makes those reachable instead of
 * dead code.
 */
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            ContextSheetRow(
                icon = if (app.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                label = if (app.favorite) "Remove from favorites" else "Add to favorites",
                onClick = { onToggleFavorite(); onDismiss() }
            )
            ContextSheetRow(
                icon = Icons.Filled.TouchApp,
                label = "Set swipe quick action",
                onClick = { onSetQuickAction(); onDismiss() }
            )
            ContextSheetRow(
                icon = Icons.Filled.Widgets,
                label = "Add to a space",
                onClick = { onAddToSpace(); onDismiss() }
            )
            ContextSheetRow(
                icon = Icons.Filled.VisibilityOff,
                label = "Hide from app list",
                onClick = { onHide(); onDismiss() }
            )
            ContextSheetRow(
                icon = Icons.Filled.Delete,
                label = "Uninstall",
                destructive = true,
                onClick = { onUninstall(); onDismiss() }
            )
        }
    }
}

@Composable
private fun ContextSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
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
            tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
