package com.minim.launcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---- Nothing OS: black, white, and exactly one red. No secondary accents. ----
private val NothingRed = Color(0xFFD71921)

private val NothingDark = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    primary = NothingRed,
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF8A8A8A),
    outline = Color(0xFF2A2A2A)
)

private val NothingLight = lightColorScheme(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF7F7F7),
    onBackground = Color(0xFF0A0A0A),
    onSurface = Color(0xFF0A0A0A),
    primary = NothingRed,
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6E6E6E),
    outline = Color(0xFFE0E0E0)
)

// ---- Android 16 / Material 3 Expressive: bold, colorful, can use dynamic color. ----
private val Android16Dark = darkColorScheme(
    background = Color(0xFF141218),
    surface = Color(0xFF1D1B20),
    onBackground = Color(0xFFE6E0E9),
    onSurface = Color(0xFFE6E0E9),
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC)
)

private val Android16Light = lightColorScheme(
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFF3EDF7),
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20),
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71)
)

// ---- iOS 26 Liquid Glass: near-transparent so the real wallpaper (blurred
// behind the window, see MainActivity's window chrome) shows through. ----
private val GlassDark = darkColorScheme(
    background = Color.Transparent,
    surface = Color(0x33FFFFFF), // translucent white panel over blurred wallpaper
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    primary = Color(0xFF0A84FF), // iOS system blue
    secondary = Color(0xFFEBEBF5)
)

private val GlassLight = lightColorScheme(
    background = Color.Transparent,
    surface = Color(0x40FFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    primary = Color(0xFF007AFF),
    secondary = Color(0xFF3C3C43)
)

// Curated accent set — used by Nothing (as a secondary tap-target color
// beyond signature red, if the user wants one) and Android 16 when dynamic
// color is off. Ignored entirely by Glass, which always uses iOS system blue.
val AccentOptions: Map<String, Color> = linkedMapOf(
    "Red" to NothingRed,
    "Blue" to Color(0xFF8AB4F8),
    "Mint" to Color(0xFF81C995),
    "Amber" to Color(0xFFFDD663),
    "Lavender" to Color(0xFFC58AF9),
    "Rose" to Color(0xFFFF8BCB)
)

enum class MinimThemeMode {
    SYSTEM, LIGHT, DARK;

    fun toRaw(): String = when (this) {
        SYSTEM -> "system"
        LIGHT -> "light"
        DARK -> "dark"
    }

    companion object {
        fun fromRaw(raw: String): MinimThemeMode = when (raw) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

@Composable
fun MinimTheme(
    designLanguage: DesignLanguage = DesignLanguage.NOTHING,
    themeMode: MinimThemeMode = MinimThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    accentColor: Color = AccentOptions.getValue("Red"),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        MinimThemeMode.SYSTEM -> systemDark
        MinimThemeMode.LIGHT -> false
        MinimThemeMode.DARK -> true
    }
    val context = LocalContext.current

    val colorScheme: ColorScheme = when (designLanguage) {
        DesignLanguage.NOTHING -> if (useDark) NothingDark else NothingLight
        DesignLanguage.ANDROID_16 -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            useDark -> Android16Dark.copy(primary = accentColor)
            else -> Android16Light.copy(primary = accentColor)
        }
        DesignLanguage.GLASS -> if (useDark) GlassDark else GlassLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
            if (designLanguage != DesignLanguage.GLASS) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
            }
        }
    }

    CompositionLocalProvider(LocalDesignLanguage provides designLanguage) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyFor(designLanguage),
            shapes = shapesFor(designLanguage),
            content = content
        )
    }
}
