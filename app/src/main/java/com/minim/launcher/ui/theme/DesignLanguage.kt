package com.minim.launcher.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Three complete visual identities, not just three color swaps. Each one
 * changes color, shape, typography, and surface treatment together so the
 * launcher actually feels like a different OS's home screen rather than a
 * single skin with a tinted accent.
 */
enum class DesignLanguage {
    /** Monochrome + signature red, sharp corners, dot-matrix-style type. Default. */
    NOTHING,

    /** Material 3 Expressive: bold color, large continuous corners, springy motion. */
    ANDROID_16,

    /** iOS 26 "Liquid Glass": translucent frosted panels over the real wallpaper. */
    GLASS;

    /** Encodes back to the same string stored in Settings/Space rows. */
    fun toRaw(): String = when (this) {
        NOTHING -> "nothing"
        ANDROID_16 -> "android16"
        GLASS -> "glass"
    }

    companion object {
        /**
         * Centralizes the string<->enum mapping that was previously
         * duplicated inline in MainActivity, SettingsActivity, SpacesActivity,
         * GesturesActivity, and HiddenAppsActivity — one place to change if a
         * fourth design language is ever added.
         */
        fun fromRaw(raw: String): DesignLanguage = when (raw) {
            "android16" -> ANDROID_16
            "glass" -> GLASS
            else -> NOTHING
        }
    }
}

val LocalDesignLanguage = compositionLocalOf { DesignLanguage.NOTHING }
