package com.minim.launcher.util

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import com.minim.launcher.ui.theme.DesignLanguage

/**
 * Only Glass mode needs real window-level changes: it shows the actual live
 * wallpaper behind the launcher and blurs it natively via
 * Window.setBackgroundBlurRadius (API 31+), which is the same mechanism iOS's
 * "Liquid Glass" and Android's own blurred-behind surfaces use — not a fake
 * translucent screenshot. Nothing and Android 16 modes render an ordinary
 * opaque window, so this restores that on switch-away.
 */
object WindowChromeController {

    /**
     * @param useDark Which flat background to paint behind Nothing/Android16
     *   content. This is the window's *background drawable*, which Android
     *   paints before Compose has drawn a single frame — hardcoding it to
     *   black (as this used to do) meant a light-theme user saw a flash of
     *   black on every launcher resume/rotation, before Compose caught up
     *   and painted the real (light) background over it.
     */
    fun apply(activity: Activity, designLanguage: DesignLanguage, useDark: Boolean = true) {
        val window = activity.window
        when (designLanguage) {
            DesignLanguage.GLASS -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 80
                    }
                }
            }
            DesignLanguage.NOTHING, DesignLanguage.ANDROID_16 -> {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER or
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                )
                window.setBackgroundDrawable(
                    ColorDrawable(if (useDark) Color.BLACK else Color.WHITE)
                )
            }
        }
    }
}
