package com.minim.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo
import com.minim.launcher.ui.components.*
import com.minim.launcher.viewmodel.HomeUiState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    state: HomeUiState,
    widgetCollapsed: Boolean,
    showClock: Boolean,
    showQuickSettings: Boolean,
    activeProfileName: String? = null,
    onClockLongClick: () -> Unit = {},
    calendarPeekText: String? = null,
    appWidgetId: Int = -1,
    createWidgetHostView: (Int) -> android.view.View? = { null },
    onAddWidget: () -> Unit = {},
    onRemoveWidget: () -> Unit = {},
    quickToggles: List<QuickToggle>,
    onToggleWidget: () -> Unit,
    onClockClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onQuickAction: (AppInfo) -> Unit,
    quickActionFor: (AppInfo) -> QuickActionType?,
    notificationPackages: Set<String>,
    iconLoader: suspend (String) -> Any?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val visibleApps = state.filteredApps
    val availableLetters = remember(visibleApps) {
        visibleApps.map { it.indexLetter }.toSet()
    }
    val letterIndexMap = remember(visibleApps) {
        val map = LinkedHashMap<Char, Int>()
        visibleApps.forEachIndexed { i, app -> map.putIfAbsent(app.indexLetter, i) }
        map
    }

    Row(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            if (showClock && state.searchQuery.isBlank()) {
                item {
                    ClockHeader(
                        activeProfileName = activeProfileName,
                        infoLine = calendarPeekText,
                        onClick = onClockClick,
                        onLongClick = onClockLongClick
                    )
                }
            }

            item {
                SearchField(
                    query = state.searchQuery,
                    onQueryChange = onQueryChange,
                    onSearch = { }
                )
            }

            if (showQuickSettings && state.searchQuery.isBlank() && quickToggles.isNotEmpty()) {
                item { QuickSettingsRow(toggles = quickToggles) }
            }

            item {
                WidgetSpace(
                    collapsed = widgetCollapsed,
                    onToggleCollapsed = onToggleWidget,
                    appWidgetId = appWidgetId,
                    createHostView = createWidgetHostView,
                    onAddWidget = onAddWidget,
                    onRemoveWidget = onRemoveWidget
                )
            }

            if (state.searchQuery.isBlank() && state.recentlyInstalled.isNotEmpty()) {
                item { SectionLabel("Recently installed") }
                items(state.recentlyInstalled, key = { "recent_${it.packageName}" }) { app ->
                    AppRow(
                        app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                        hasNotification = app.packageName in notificationPackages,
                        hapticsEnabled = state.hapticsEnabled,
                        iconShapeName = state.iconShape,
                        onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                        onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                    )
                }
            }

            if (state.searchQuery.isBlank() && state.showSuggestions && state.suggestions.isNotEmpty()) {
                item { SectionLabel("Suggested") }
                items(state.suggestions, key = { "sugg_${it.packageName}" }) { app ->
                    AppRow(
                        app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                        hasNotification = app.packageName in notificationPackages,
                        hapticsEnabled = state.hapticsEnabled,
                        iconShapeName = state.iconShape,
                        onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                        onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                    )
                }
            }

            if (state.searchQuery.isBlank() && state.favorites.isNotEmpty()) {
                item { SectionLabel("Favorites") }
                items(state.favorites, key = { "fav_${it.packageName}" }) { app ->
                    AppRow(
                        app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                        hasNotification = app.packageName in notificationPackages,
                        hapticsEnabled = state.hapticsEnabled,
                        iconShapeName = state.iconShape,
                        onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                        onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                    )
                }
            }

            item { SectionLabel(if (state.searchQuery.isBlank()) "All apps" else "Results") }
            items(visibleApps, key = { it.packageName }) { app ->
                AppRow(
                    app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                    hasNotification = app.packageName in notificationPackages,
                    hapticsEnabled = state.hapticsEnabled,
                        iconShapeName = state.iconShape,
                    onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                    onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                )
            }
        }

        if (state.searchQuery.isBlank()) {
            AlphabetIndexBar(
                availableLetters = availableLetters,
                onLetterSelected = { letter ->
                    letterIndexMap[letter]?.let { targetIndex ->
                        val headerOffset = listOf(
                            showClock,
                            true,
                            showQuickSettings && quickToggles.isNotEmpty(),
                            true,
                            state.recentlyInstalled.isNotEmpty(),
                            state.suggestions.isNotEmpty(),
                            state.favorites.isNotEmpty(),
                            true
                        ).count { it }
                        scope.launch {
                            listState.scrollToItem((targetIndex + headerOffset).coerceAtLeast(0))
                        }
                    }
                },
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val language = com.minim.launcher.ui.theme.LocalDesignLanguage.current
    val displayText = if (language == com.minim.launcher.ui.theme.DesignLanguage.NOTHING) {
        text.uppercase()
    } else {
        text
    }
    Text(
        text = displayText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 8.dp)
    )
}
