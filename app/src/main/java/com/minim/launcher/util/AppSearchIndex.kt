package com.minim.launcher.util

import com.minim.launcher.data.AppInfo

/**
 * Built once whenever the app list changes (install/uninstall — rare), not
 * on every keystroke, so typing never pays a rebuild cost. Query itself is a
 * single contains-scan over the (small, a few hundred apps at most) list —
 * cheap enough that no bucketing/indexing is needed, and simple enough that
 * "type anywhere in the name" behaves the way people expect.
 *
 * Earlier versions bucketed apps by their first letter and only scanned the
 * bucket matching the query's first letter. That's correct for a prefix
 * search but wrong for a contains search: searching "note" would miss
 * "BlackNote" because 'n' and 'B' don't match, even though the label clearly
 * contains "note". Scanning the full list avoids that class of missing
 * results entirely.
 */
class AppSearchIndex private constructor(
    private val apps: List<AppInfo>
) {
    fun query(raw: String): List<AppInfo> {
        val q = raw.trim().lowercase()
        if (q.isEmpty()) return apps

        return apps
            .filter { it.label.contains(q, ignoreCase = true) }
            .sortedWith(
                compareByDescending<AppInfo> { it.label.startsWith(q, ignoreCase = true) }
                    .thenBy { it.label.length }
            )
    }

    companion object {
        fun build(apps: List<AppInfo>): AppSearchIndex = AppSearchIndex(apps)
    }
}
