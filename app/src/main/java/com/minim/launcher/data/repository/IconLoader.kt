package com.minim.launcher.data.repository

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Icons are the single most expensive thing a launcher can render. Niagara-style
 * launchers stay light by treating icons as optional decoration, not the primary
 * affordance. This loader:
 *   - Is never called unless the user has enabled "show icons" in settings
 *   - Caches a bounded number of drawables in memory (LruCache sized to 1/8 of
 *     available app memory) so scrolling never re-decodes
 *   - Loads off the main thread
 */
class IconLoader(context: Context) {
    private val appContext = context.applicationContext
    private val pm = appContext.packageManager

    private val cache: LruCache<String, Drawable> = run {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = maxMemoryKb / 8
        object : LruCache<String, Drawable>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Drawable): Int {
                // Rough estimate: assume ~4 bytes/px at typical launcher icon size.
                val w = value.intrinsicWidth.coerceAtLeast(1)
                val h = value.intrinsicHeight.coerceAtLeast(1)
                return (w * h * 4) / 1024
            }
        }
    }

    suspend fun load(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        cache.get(packageName)?.let { return@withContext it }
        val drawable = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
        drawable?.let { cache.put(packageName, it) }
        drawable
    }

    fun clear() = cache.evictAll()
}
