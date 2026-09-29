package com.minim.launcher.ui.components

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage

/**
 * ONE collapsible widget slot instead of a free-form widget-covered home
 * screen — keeps memory and redraw cost bounded and predictable. Real
 * hosting (AppWidgetHost/AppWidgetManager) lives in WidgetHostManager and
 * MainActivity's widget-pick flow; this component only owns the collapse/
 * expand chrome, the empty-state "add widget" prompt, and rendering the
 * AppWidgetHostView once one is configured.
 */
@Composable
fun WidgetSpace(
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    appWidgetId: Int,
    createHostView: (Int) -> View?,
    onAddWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalDesignLanguage.current
    val label = if (language == DesignLanguage.NOTHING) "WIDGET" else "Widget"
    val hasWidget = appWidgetId != -1

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasWidget && !collapsed) {
                    IconButton(onClick = onRemoveWidget, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove widget",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = onToggleCollapsed, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (collapsed) "Expand widget" else "Collapse widget",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !collapsed,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            AdaptiveSurface(modifier = Modifier.heightIn(min = 120.dp, max = 260.dp)) {
                if (hasWidget) {
                    // Keyed on appWidgetId: AndroidView's `factory` only runs once
                    // per call site, so without this key, switching to a different
                    // widget (remove + add) would keep showing the old host view
                    // since createHostView(appWidgetId) never re-runs.
                    key(appWidgetId) {
                        AndroidView(
                            factory = { context -> createHostView(appWidgetId) ?: View(context) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No widget added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        TextButton(onClick = onAddWidget) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add widget")
                        }
                    }
                }
            }
        }
    }
}
