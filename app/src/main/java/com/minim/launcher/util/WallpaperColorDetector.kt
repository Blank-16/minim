package com.minim.launcher.util

import android.app.WallpaperManager
import android.content.Context
import android.os.Build

/**
 * Uses Android's WallpaperManager API (API 27+) to detect whether the user's
 * current home screen wallpaper (static or live) is light/bright.
 * When true, dark text / darker card scrims are used so that text remains
 * 100% legible on pure white or bright wallpapers.
 */
object WallpaperColorDetector {
    fun isWallpaperLight(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    val primary = colors.primaryColor
                    return primary.luminance() > 0.5f
                }
            } catch (_: Exception) {
                // Fall back gracefully if permission or IPC fails
            }
        }
        return false
    }
}
