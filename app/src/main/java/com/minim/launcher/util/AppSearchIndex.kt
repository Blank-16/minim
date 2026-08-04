package com.minim.launcher.util

import com.minim.launcher.data.AppInfo

/**
 * Precomputed once whenever the app list changes (install/uninstall — rare),
 * not on every keystroke. Search itself is then just a map lookup plus a
 * cheap contains-scan over a small candidate set, so typing feels instant
 * even with 500+ apps installed. This is what keeps search latency
 * imperceptible without needing debounce-induced lag.
 */
class AppSearchIndex private constructor(
    private val apps: List<AppInfo>,
    private val byPrefix: Map<String, List<AppInfo>>
) {
    fun query(raw: String): List<AppInfo> {
        val q = raw.trim().lowercase()
        if (q.isEmpty()) return apps

        // Exact-prefix bucket first (covers the overwhelming majority of real
        // searches — people type the start of an app name).
        val prefixHit = byPrefix[q.take(PREFIX_LEN)]
        val candidates = prefixHit ?: apps

        return candidates
            .filter { it.label.contains(q, ignoreCase = true) }
            .sortedWith(
                compareByDescending<AppInfo> { it.label.startsWith(q, ignoreCase = true) }
                    .thenBy { it.label.length }
            )
    }

    companion object {
        private const val PREFIX_LEN = 1

        fun build(apps: List<AppInfo>): AppSearchIndex {
            val buckets = HashMap<String, MutableList<AppInfo>>()
            for (app in apps) {
                val key = app.label.take(PREFIX_LEN).lowercase()
                buckets.getOrPut(key) { mutableListOf() }.add(app)
            }
            return AppSearchIndex(apps, buckets)
        }
    }
}
