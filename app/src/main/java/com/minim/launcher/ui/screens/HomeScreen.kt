package com.minim.launcher.ui.screens

import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo
import com.minim.launcher.ui.components.*
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
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
    createWidgetHostView: (Int) -> View? = { null },
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
    val density = LocalDensity.current

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
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Pinned top section (Clock, Search, QuickSettings)
            if (showClock && state.searchQuery.isBlank()) {
                ClockHeader(
                    activeProfileName = activeProfileName,
                    infoLine = calendarPeekText,
                    onClick = onClockClick,
                    onLongClick = onClockLongClick
                )
            }

            SearchField(
                query = state.searchQuery,
                onQueryChange = onQueryChange,
                onSearch = { }
            )

            if (showQuickSettings && state.searchQuery.isBlank() && quickToggles.isNotEmpty()) {
                QuickSettingsRow(toggles = quickToggles)
            }

            // Scrolling apps list (LazyColumn with widget, section headers and cards) taking remaining height
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                if (state.searchQuery.isBlank()) {
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
                }

                if (state.searchQuery.isBlank() && state.recentlyInstalled.isNotEmpty()) {
                    item {
                        SectionLabel("Recently installed")
                    }
                    item {
                        AdaptiveSurface(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 4.dp)
                        ) {
                            Column {
                                state.recentlyInstalled.forEachIndexed { index, app ->
                                    AppRow(
                                        app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                                        hasNotification = app.packageName in notificationPackages,
                                        hapticsEnabled = state.hapticsEnabled,
                                        iconShapeName = state.iconShape,
                                        onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                                        onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                                    )
                                    if (index < state.recentlyInstalled.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.searchQuery.isBlank() && state.showSuggestions && state.suggestions.isNotEmpty()) {
                    item {
                        SectionLabel("Suggested")
                    }
                    item {
                        AdaptiveSurface(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 4.dp)
                        ) {
                            Column {
                                state.suggestions.forEachIndexed { index, app ->
                                    AppRow(
                                        app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                                        hasNotification = app.packageName in notificationPackages,
                                        hapticsEnabled = state.hapticsEnabled,
                                        iconShapeName = state.iconShape,
                                        onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                                        onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                                    )
                                    if (index < state.suggestions.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.searchQuery.isBlank() && state.favorites.isNotEmpty()) {
                    item {
                        SectionLabel("Favorites")
                    }
                    item {
                        AdaptiveSurface(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 4.dp)
                        ) {
                            Column {
                                state.favorites.forEachIndexed { index, app ->
                                    AppRow(
                                        app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                                        hasNotification = app.packageName in notificationPackages,
                                        hapticsEnabled = state.hapticsEnabled,
                                        iconShapeName = state.iconShape,
                                        onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                                        onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                                    )
                                    if (index < state.favorites.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (visibleApps.isNotEmpty()) {
                    item {
                        SectionLabel(if (state.searchQuery.isBlank()) "All apps" else "Results")
                    }
                    item {
                        AdaptiveSurface(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 4.dp)
                        ) {
                            Column {
                                visibleApps.forEachIndexed { index, app ->
                                    AppRow(
                                        app = app, showIcon = state.showIcons, iconLoader = iconLoader,
                                        hasNotification = app.packageName in notificationPackages,
                                        hapticsEnabled = state.hapticsEnabled,
                                        iconShapeName = state.iconShape,
                                        onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) },
                                        onQuickAction = { onQuickAction(app) }, quickActionType = quickActionFor(app)
                                    )
                                    if (index < visibleApps.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.searchQuery.isBlank()) {
            val appRowHeightPx = with(density) { 60.dp.toPx() }
            val allAppsItemIndex = listOf(
                true,
                state.recentlyInstalled.isNotEmpty(),
                state.recentlyInstalled.isNotEmpty(),
                state.showSuggestions && state.suggestions.isNotEmpty(),
                state.showSuggestions && state.suggestions.isNotEmpty(),
                state.favorites.isNotEmpty(),
                state.favorites.isNotEmpty()
            ).count { it }

            AlphabetIndexBar(
                availableLetters = availableLetters,
                onLetterSelected = { letter ->
                    letterIndexMap[letter]?.let { targetIndex ->
                        val scrollOffset = (targetIndex * appRowHeightPx).toInt()
                        scope.launch {
                            listState.scrollToItem(allAppsItemIndex, scrollOffset)
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
    val language = LocalDesignLanguage.current

    val displayText = if (language == DesignLanguage.NOTHING) {
        text.uppercase()
    } else {
        text
    }

    val textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = if (language == DesignLanguage.GLASS) 0.75f else 0.35f)

    val textStyle = MaterialTheme.typography.labelSmall

    Text(
        text = displayText,
        style = textStyle,
        color = textColor,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 4.dp)
    )
}
