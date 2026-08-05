package com.minim.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage

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
    val shape = MaterialTheme.shapes.large

    when (language) {
        DesignLanguage.NOTHING -> {
            // Thin single-pixel border, flat fill, sharp corners — reads as
            // an etched panel rather than a soft card.
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            ) { content() }
        }

        DesignLanguage.ANDROID_16 -> {
            // Tonal Material surface, generous rounding, no border — the
            // color itself carries the elevation cue.
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
            ) { content() }
        }

        DesignLanguage.GLASS -> {
            // Frosted panel: translucent fill (already semi-transparent in
            // GlassDark/GlassLight) plus a soft specular highlight along the
            // top edge, which is what sells "glass" over a plain translucent
            // rectangle. The actual blur-of-what's-behind comes from the
            // window's native backdrop blur (see WindowChromeController), not
            // from this composable — this just needs to look like glass
            // sitting on top of that.
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.0f)
                            ),
                            endY = 60f
                        )
                    )
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))
                        ),
                        shape = shape
                    )
            ) { content() }
        }
    }
}
