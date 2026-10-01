package com.minim.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
import com.minim.launcher.ui.theme.LocalIsDark
import com.minim.launcher.util.WallpaperColorDetector

/**
 * One panel primitive, three renderings — used for the widget space, the
 * quick-settings row, and bottom sheets. This is the single place that
 * decides what a "card" looks like in each design language, so those
 * components don't each need their own if/when block.
 */
@Composable
fun AdaptiveSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val language = LocalDesignLanguage.current
    val baseShape = MaterialTheme.shapes.large
    val shape: RoundedCornerShape = remember(baseShape) {
        val radius = if (language == DesignLanguage.GLASS) 26.dp else 28.dp
        RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = radius,
            bottomEnd = 0.dp
        )
    }
    val context = LocalContext.current

    // Background & border modifier with right-side fade so the right edge
    // seamlessly blends into the background with no harsh boundary.
    val surfaceBackgroundModifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            val fadeWidth = size.width * 0.25f
            val startX = size.width - fadeWidth
            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.White,
                    1.0f to Color.Transparent,
                    startX = startX,
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }

    when (language) {
        DesignLanguage.NOTHING -> {
            Box(modifier = modifier.fillMaxWidth()) {
                Box(
                    modifier = surfaceBackgroundModifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                )
                Box(modifier = Modifier.fillMaxWidth().clip(shape)) {
                    content()
                }
            }
        }

        DesignLanguage.ANDROID_16 -> {
            Box(modifier = modifier.fillMaxWidth()) {
                Box(
                    modifier = surfaceBackgroundModifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface)
                )
                Box(modifier = Modifier.fillMaxWidth().clip(shape)) {
                    content()
                }
            }
        }

        DesignLanguage.GLASS -> {
            val isDark = LocalIsDark.current
            val baseCardColor = if (isDark) {
                Color(0x991C1C1E)
            } else {
                Color.White.copy(alpha = 0.85f)
            }

            val highlightTop = if (isDark) {
                Color.White.copy(alpha = 0.18f)
            } else {
                Color.White.copy(alpha = 0.30f)
            }

            val borderColor = if (isDark) {
                Color.White.copy(alpha = 0.35f)
            } else {
                Color.Black.copy(alpha = 0.15f)
            }

            Box(modifier = modifier.fillMaxWidth()) {
                Box(
                    modifier = surfaceBackgroundModifier
                        .matchParentSize()
                        .background(baseCardColor)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    highlightTop,
                                    Color.Transparent
                                ),
                                endY = 80f
                            )
                        )
                        .border(
                            width = 0.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(borderColor, borderColor.copy(alpha = 0.10f))
                            ),
                            shape = shape
                        )
                )
                Box(modifier = Modifier.fillMaxWidth().clip(shape)) {
                    content()
                }
            }
        }
    }
}
