package com.minim.launcher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minim.launcher.ui.components.AdaptiveSurface
import com.minim.launcher.ui.components.AppIcon
import com.minim.launcher.ui.components.SearchField
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
import com.minim.launcher.ui.theme.LocalIsDark
import com.minim.launcher.util.WidgetHostManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetPickerItem(
    val providerInfo: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val packageName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    widgetHostManager: WidgetHostManager,
    iconLoader: suspend (String) -> Any?,
    iconShape: Shape,
    onDismiss: () -> Unit,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalDesignLanguage.current
    val isGlass = language == DesignLanguage.GLASS
    val isDark = LocalIsDark.current
    val glassColor = if (isDark) Color.White else Color.Black

    var searchQuery by remember { mutableStateOf("") }
    var allItems by remember { mutableStateOf<List<WidgetPickerItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val items = withContext(Dispatchers.IO) {
            widgetHostManager.getInstalledProviders().mapNotNull { info ->
                val pkg = info.provider.packageName
                val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                val appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
                val widgetLabel = info.loadLabel(pm)?.takeIf { it.isNotBlank() } ?: appLabel
                WidgetPickerItem(
                    providerInfo = info,
                    appLabel = appLabel,
                    widgetLabel = widgetLabel,
                    packageName = pkg
                )
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))
        }
        allItems = items
    }

    val filteredItems = remember(searchQuery, allItems) {
        if (searchQuery.isBlank()) allItems
        else {
            val q = searchQuery.trim().lowercase()
            allItems.filter {
                it.appLabel.lowercase().contains(q) || it.widgetLabel.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        val content = @Composable {
            Column(modifier = Modifier.fillMaxHeight(0.85f).padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Widget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGlass) glassColor else MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { onOpenSystemPicker(); onDismiss() }) {
                        Text("System picker")
                    }
                }

                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {}
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Loading widgets..." else "No widgets found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(
                            items = filteredItems,
                            key = { "${it.packageName}/${it.providerInfo.provider.className}" }
                        ) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onWidgetSelected(item.providerInfo)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    packageName = item.packageName,
                                    loader = iconLoader,
                                    shape = iconShape,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.widgetLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isGlass) glassColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.widgetLabel != item.appLabel) {
                                        Text(
                                            text = item.appLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )
                        }
                    }
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
