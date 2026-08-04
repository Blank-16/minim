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

    fun apply(activity: Activity, designLanguage: DesignLanguage) {
        val window = activity.window
        when (designLanguage) {
            DesignLanguage.GLASS -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 110
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
                // Pre-API 31: no native blur behind is available. The
                // translucent panel colors in GlassDark/GlassLight still show
                // the live (unblurred) wallpaper through, which is a
                // reasonable, honest degradation rather than a fake blur.
            }
            DesignLanguage.NOTHING, DesignLanguage.ANDROID_16 -> {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER or
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                )
                window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            }
        }
    }
}
