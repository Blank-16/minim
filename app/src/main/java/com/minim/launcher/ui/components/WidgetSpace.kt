package com.minim.launcher.ui.components

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Collapsible widget space supporting multiple app widgets with manual
 * swipe paging and automatic slideshow auto-scrolling every 2 seconds in an infinite loop.
 * Any user touch interaction temporarily pauses auto-scrolling.
 */
@Composable
fun WidgetSpace(
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    appWidgetIds: List<Int>,
    createHostView: (Int) -> View?,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalDesignLanguage.current
    val label = if (language == DesignLanguage.NOTHING) "WIDGET" else "Widget"
    val hasWidgets = appWidgetIds.isNotEmpty()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { appWidgetIds.size }
    )

    // Interaction timestamp to pause auto-scroll during touch/drag
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Scroll to new widget when added
    var previousWidgetCount by remember { mutableIntStateOf(appWidgetIds.size) }
    LaunchedEffect(appWidgetIds.size) {
        if (appWidgetIds.size > previousWidgetCount && appWidgetIds.isNotEmpty()) {
            pagerState.animateScrollToPage(appWidgetIds.lastIndex)
        }
        previousWidgetCount = appWidgetIds.size
    }

    // Auto horizontal scroll slideshow timer (every 2s in infinite loop)
    LaunchedEffect(hasWidgets, collapsed, appWidgetIds.size) {
        if (hasWidgets && !collapsed && appWidgetIds.size > 1) {
            while (true) {
                delay(2000L)
                val timeSinceLastTouch = System.currentTimeMillis() - lastInteractionTime
                if (!pagerState.isScrollInProgress && appWidgetIds.size > 1 && timeSinceLastTouch > 3000L) {
                    val nextPage = (pagerState.currentPage + 1) % appWidgetIds.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    // Safe current appWidgetId based on pager position
    val currentPage = pagerState.currentPage.coerceIn(0, (appWidgetIds.size - 1).coerceAtLeast(0))
    val currentAppWidgetId = if (hasWidgets && currentPage < appWidgetIds.size) appWidgetIds[currentPage] else -1

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
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
                if (hasWidgets && !collapsed) {
                    // Page indicator dots for multiple widgets
                    if (appWidgetIds.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            repeat(appWidgetIds.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 6.dp else 4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }

                    // Add widget button
                    IconButton(onClick = onAddWidget, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add widget",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    // Remove current widget button
                    IconButton(
                        onClick = { if (currentAppWidgetId != -1) onRemoveWidget(currentAppWidgetId) },
                        modifier = Modifier.size(36.dp)
                    ) {
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
            if (hasWidgets) {
                var totalDragOffset by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                        .pointerInput(appWidgetIds.size) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            }
                        }
                        .pointerInput(appWidgetIds.size) {
                            if (appWidgetIds.size > 1) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        totalDragOffset = 0f
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    onDragEnd = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        if (totalDragOffset < -40f) {
                                            val nextPage = (pagerState.currentPage + 1) % appWidgetIds.size
                                            scope.launch { pagerState.animateScrollToPage(nextPage) }
                                        } else if (totalDragOffset > 40f) {
                                            val prevPage = if (pagerState.currentPage - 1 < 0) appWidgetIds.size - 1 else pagerState.currentPage - 1
                                            scope.launch { pagerState.animateScrollToPage(prevPage) }
                                        }
                                    },
                                    onDragCancel = {
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDragOffset += dragAmount
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                )
                            }
                        }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val widgetId = appWidgetIds.getOrNull(page) ?: return@HorizontalPager
                        key(widgetId) {
                            AndroidView(
                                factory = { context -> createHostView(widgetId) ?: View(context) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                AdaptiveSurface(modifier = Modifier.heightIn(min = 120.dp, max = 260.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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
