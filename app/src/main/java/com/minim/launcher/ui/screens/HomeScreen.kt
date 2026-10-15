package com.minim.launcher.ui.screens

import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    appWidgetIds: List<Int> = emptyList(),
    createWidgetHostView: (Int) -> View? = { null },
    onAddWidget: () -> Unit = {},
    onRemoveWidget: (Int) -> Unit = {},
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

    // Calculate item index offset in LazyColumn where "All apps" items begin
    val allAppsHeaderItemIndex = remember(
        state.searchQuery,
        state.recentlyInstalled,
        state.showSuggestions,
        state.suggestions,
        state.favorites
    ) {
        if (state.searchQuery.isNotBlank()) {
            0
        } else {
            var count = 0
            if (state.recentlyInstalled.isNotEmpty()) {
                count += 1 + state.recentlyInstalled.size
            }
            if (state.showSuggestions && state.suggestions.isNotEmpty()) {
                count += 1 + state.suggestions.size
            }
            if (state.favorites.isNotEmpty()) {
                count += 1 + state.favorites.size
            }
            count
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Pinned top section (Clock, Search, QuickSettings, Widget)
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

        if (state.searchQuery.isBlank()) {
            WidgetSpace(
                collapsed = widgetCollapsed,
                onToggleCollapsed = onToggleWidget,
                appWidgetIds = appWidgetIds,
                createHostView = createWidgetHostView,
                onAddWidget = onAddWidget,
                onRemoveWidget = onRemoveWidget
            )
        }

        // Lower section: App list container and Alphabet Index side by side below the widget container
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Fixed container for the apps list: scrolling moves only app names & icons inside this container
            AdaptiveSurface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(
                        start = 20.dp,
                        end = if (state.searchQuery.isBlank()) 0.dp else 20.dp,
                        top = 4.dp,
                        bottom = 12.dp
                    )
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    if (state.searchQuery.isBlank() && state.recentlyInstalled.isNotEmpty()) {
                        item(key = "header_recently_installed") {
                            SectionLabel("Recently installed")
                        }
                        itemsIndexed(
                            items = state.recentlyInstalled,
                            key = { _, app -> "recent_${app.packageName}" }
                        ) { index, app ->
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
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }

                    if (state.searchQuery.isBlank() && state.showSuggestions && state.suggestions.isNotEmpty()) {
                        item(key = "header_suggestions") {
                            SectionLabel("Suggested")
                        }
                        itemsIndexed(
                            items = state.suggestions,
                            key = { _, app -> "suggested_${app.packageName}" }
                        ) { index, app ->
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
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }

                    if (state.searchQuery.isBlank() && state.favorites.isNotEmpty()) {
                        item(key = "header_favorites") {
                            SectionLabel("Favorites")
                        }
                        itemsIndexed(
                            items = state.favorites,
                            key = { _, app -> "favorite_${app.packageName}" }
                        ) { index, app ->
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
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }

                    if (visibleApps.isNotEmpty()) {
                        item(key = "header_all_apps") {
                            SectionLabel(if (state.searchQuery.isBlank()) "All apps" else "Results")
                        }
                        itemsIndexed(
                            items = visibleApps,
                            key = { _, app -> "all_${app.packageName}" }
                        ) { index, app ->
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
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (state.searchQuery.isBlank()) {
                AlphabetIndexBar(
                    availableLetters = availableLetters,
                    onLetterSelected = { letter ->
                        val targetIndex = findTargetAppIndex(letter, visibleApps)
                        if (targetIndex != null) {
                            val targetItemIndex = allAppsHeaderItemIndex + 1 + targetIndex
                            scope.launch {
                                listState.scrollToItem(targetItemIndex)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

private fun findTargetAppIndex(targetLetter: Char, visibleApps: List<AppInfo>): Int? {
    if (visibleApps.isEmpty()) return null

    if (targetLetter == '#') {
        val nonAlphaIndex = visibleApps.indexOfFirst { it.indexLetter == '#' }
        return if (nonAlphaIndex != -1) nonAlphaIndex else 0
    }

    val exactMatchIndex = visibleApps.indexOfFirst { it.indexLetter == targetLetter }
    if (exactMatchIndex != -1) return exactMatchIndex

    val nextMatchIndex = visibleApps.indexOfFirst { it.indexLetter > targetLetter }
    if (nextMatchIndex != -1) return nextMatchIndex

    return visibleApps.lastIndex
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
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 4.dp)
    )
}
