package com.minim.launcher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
import com.minim.launcher.util.WallpaperColorDetector

private val SWIPE_REVEAL_DP = 72.dp

/**
 * A single row in the alphabetical app list. Swiping left reveals a quick
 * action (call, message, or "open to a specific screen") without leaving the
 * list. Tap = launch. Long-press = context menu. Both give a short haptic
 * tick when enabled — small, but it's the difference between a launcher that
 * feels tactile and one that feels like a plain list view.
 */
@Composable
fun AppRow(
    app: AppInfo,
    showIcon: Boolean,
    iconLoader: suspend (String) -> Any?,
    hasNotification: Boolean,
    hapticsEnabled: Boolean,
    iconShapeName: String = "circle",
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickAction: () -> Unit,
    quickActionType: QuickActionType?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val revealPx = with(density) { SWIPE_REVEAL_DP.toPx() }
    var offsetX by remember(app.packageName) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "swipeOffset"
    )

    fun tick(type: HapticFeedbackType = HapticFeedbackType.LongPress) {
        if (hapticsEnabled) haptics.performHapticFeedback(type)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (quickActionType != null && animatedOffset < 0f) {
            val alpha = (-animatedOffset / revealPx).coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { this.alpha = alpha }
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (quickActionType) {
                        QuickActionType.CALL -> Icons.Filled.Call
                        QuickActionType.MESSAGE -> Icons.Filled.Message
                        QuickActionType.DEEP_LINK -> Icons.Filled.OpenInNew
                    },
                    contentDescription = "Quick action",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedOffset }
                .pointerInput(app.packageName, quickActionType) {
                    if (quickActionType == null) return@pointerInput
                    var revealed = false
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -revealPx / 2) {
                                if (!revealed) tick(HapticFeedbackType.TextHandleMove)
                                onQuickAction()
                            }
                            offsetX = 0f
                            revealed = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-revealPx, 0f)
                            if (offsetX <= -revealPx / 2 && !revealed) {
                                revealed = true
                                tick(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
                .pointerInput(app.packageName) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = {
                            tick()
                            onLongClick()
                        }
                    )
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                AppIcon(
                    packageName = app.packageName,
                    loader = iconLoader,
                    shape = com.minim.launcher.ui.theme.iconShapeFor(iconShapeName)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            val language = LocalDesignLanguage.current
            val textColor = MaterialTheme.colorScheme.onBackground

            val textStyle = MaterialTheme.typography.bodyLarge

            Text(
                text = app.label,
                style = textStyle,
                color = textColor,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            if (hasNotification) {
                val badgeShape = if (language == com.minim.launcher.ui.theme.DesignLanguage.NOTHING) {
                    RectangleShape
                } else {
                    CircleShape
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, badgeShape)
                )
            }
        }
    }
}

enum class QuickActionType { CALL, MESSAGE, DEEP_LINK }
