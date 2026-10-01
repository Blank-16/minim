package com.minim.launcher.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos

private val DEFAULT_ALPHABET = listOf('#') + ('A'..'Z')

/**
 * Niagara Launcher-style vertical alphabet index sidebar with compact height,
 * vertical centering, organic fisheye wave magnification, and frosted glass preview badge.
 */
@Composable
fun NiagaraAlphabetSidebar(
    items: List<Char>,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    availableItems: Set<Char>? = null
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var isTouching by remember { mutableStateOf(false) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(1f) }
    var componentHeight by remember { mutableFloatStateOf(1f) }
    var activeIndex by remember { mutableStateOf<Int?>(null) }

    val maxRadiusPx = with(density) { 240.dp.toPx() }
    val translationXDistance = with(density) { (-120).dp.toPx() }

    // Smooth touch interaction state for bounce-back animation
    val touchProgress by animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "touchProgress"
    )

    // Calculate vertical offset of the centered column inside the parent Box
    val columnTopOffset = if (containerHeight > 0f && componentHeight > 0f) {
        (containerHeight - componentHeight) / 2f
    } else {
        0f
    }

    val itemHeight = if (componentHeight > 0f && items.isNotEmpty()) {
        componentHeight / items.size.toFloat()
    } else {
        1f
    }

    // Position the preview badge directly at the cursor touch Y coordinate in real time
    val targetBadgeYDp = with(density) { (columnTopOffset + currentTouchY - 32.dp.toPx()).toDp() }

    val animatedBadgeY by animateDpAsState(
        targetValue = targetBadgeYDp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "badgeY"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .onSizeChanged { containerHeight = it.height.toFloat() },
        contentAlignment = Alignment.CenterEnd
    ) {
        // Compact vertically centered column (75% height) for high efficiency and easy thumb reach
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.75f)
                .width(48.dp)
                .padding(end = 6.dp)
                .onSizeChanged { componentHeight = it.height.toFloat() }
                .pointerInput(items) {
                    fun updateFromY(y: Float) {
                        if (items.isEmpty()) return
                        val currentCompHeight = if (componentHeight > 1f) componentHeight else size.height.toFloat()
                        if (currentCompHeight <= 0f) return
                        val clampedY = y.coerceIn(0f, currentCompHeight)
                        currentTouchY = clampedY
                        val curItemHeight = currentCompHeight / items.size.toFloat()
                        val index = (clampedY / curItemHeight)
                            .toInt()
                            .coerceIn(0, items.size - 1)
                        
                        if (index != activeIndex) {
                            activeIndex = index
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIndexSelected(index)
                        }
                    }

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                if (change.pressed) {
                                    isTouching = true
                                    updateFromY(change.position.y)
                                    change.consume()
                                } else {
                                    isTouching = false
                                    activeIndex = null
                                }
                            }
                        }
                    }
                },
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val totalCount = items.size
            if (totalCount == 0) return@Column

            items.forEachIndexed { index, letter ->
                val isAvailable = availableItems?.contains(letter) ?: true
                val letterCenterY = (index + 0.5f) * itemHeight
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            val touchY = currentTouchY
                            val progress = touchProgress

                            if (progress <= 0.001f || componentHeight <= 0f) {
                                scaleX = 1f
                                scaleY = 1f
                                translationX = 0f
                            } else {
                                val distance = abs(touchY - letterCenterY)

                                val factor = if (distance < maxRadiusPx) {
                                    val rawFactor = (1f + cos((distance / maxRadiusPx) * Math.PI.toFloat())) / 2f
                                    rawFactor * progress
                                } else {
                                    0f
                                }

                                scaleX = 1f + (0.15f * factor)
                                scaleY = 1f + (0.15f * factor)
                                translationX = translationXDistance * factor
                            }
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = letter.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAvailable) {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                        }
                    )
                }
            }
        }

        // Layer 1: Floating Frosted Glass Preview Badge perfectly following cursor Y
        val activeChar = activeIndex?.let { if (it in items.indices) items[it] else null }
        if (touchProgress > 0.01f && activeChar != null) {
            FrostedGlassBadge(
                letter = activeChar,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-42).dp, y = animatedBadgeY)
                    .graphicsLayer {
                        scaleX = touchProgress
                        scaleY = touchProgress
                        alpha = touchProgress
                    }
            )
        }
    }
}

/**
 * Isolated Frosted Glass Badge with Android 12+ RenderEffect blur and crisp text overlay.
 */
@Composable
fun FrostedGlassBadge(
    letter: Char,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val blurRadius = with(density) { 20.dp.toPx() }

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        // Blurred frosted glass background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurEffect = RenderEffect.createBlurEffect(
                            blurRadius,
                            blurRadius,
                            Shader.TileMode.CLAMP
                        )
                        renderEffect = blurEffect.asComposeRenderEffect()
                    }
                    shape = CircleShape
                    clip = true
                }
                .background(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Color.White.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )

        // Crisp text overlay on top so the letter inside the circle is sharp and fully legible
        Text(
            text = letter.toString(),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Backward-compatible wrapper for AlphabetIndexBar.
 */
@Composable
fun AlphabetIndexBar(
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    NiagaraAlphabetSidebar(
        items = DEFAULT_ALPHABET,
        onIndexSelected = { index ->
            if (index in DEFAULT_ALPHABET.indices) {
                onLetterSelected(DEFAULT_ALPHABET[index])
            }
        },
        modifier = modifier,
        availableItems = availableLetters
    )
}
