package com.minim.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * On-device cache of installed apps. This is the ONLY source the UI reads from —
 * PackageManager is queried solely to build/update this table, triggered by
 * PackageChangeReceiver, never on every launcher open. This is the single
 * biggest lever for both startup latency and battery draw.
 */
@Entity(tableName = "app_cache")
data class AppCacheEntity(
    @PrimaryKey val packageName: String,
    val activityClassName: String,
    val label: String,
    val isSystemApp: Boolean,
    val installTimeMillis: Long,
    val favorite: Boolean = false,
    val hidden: Boolean = false,
    val quickAction: String? = null // e.g. "call:+1555..." — swipe-right action payload
)

/**
 * Append-only usage log capped and pruned by [com.minim.launcher.data.db.UsageDao.pruneOlderThan].
 * Used exclusively for the on-device, no-network frequency+recency suggestion model.
 */
@Entity(tableName = "usage_events")
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestampMillis: Long,
    val hourOfDay: Int,
    val dayOfWeek: Int
)
