package com.minim.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * "Spaces" are Minim's answer to folders — a named, ordered subset of apps
 * (e.g. "Work", "Morning", "Travel") shown as an optional extra section
 * above the full alphabetical list. Unlike an icon-grid folder, a Space is
 * still just text rows, so opening one costs nothing extra to render.
 *
 * Spaces double as Contextual Profiles: each can optionally carry its own
 * design language / accent / theme mode, so activating a Space (manually,
 * via a gesture, or automatically inside a time window) can restyle the
 * whole launcher, not just filter the app list. All override fields are
 * nullable — null means "inherit the global Settings value" — so a plain
 * organizational folder with no styling opinion costs nothing extra.
 *
 * Auto-activation is time-window only, checked on app resume (see
 * ProfileResolver) — never a background job or alarm, to keep the
 * zero-polling philosophy intact. A device that's never opened during a
 * window simply never auto-activates that Space, which is an acceptable
 * tradeoff for not running anything in the background.
 */
@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val designLanguage: String? = null, // "nothing" | "android16" | "glass" | null = inherit
    val accentName: String? = null,
    val themeMode: String? = null, // "system" | "light" | "dark" | null = inherit
    val autoActivateStartHour: Int? = null, // 0-23, inclusive
    val autoActivateEndHour: Int? = null, // 0-23, exclusive; wraps past midnight if < start
    val autoActivateDaysMask: Int? = null // bitmask, bit 0 = Sunday .. bit 6 = Saturday; null = every day
)

@Entity(tableName = "space_members", primaryKeys = ["spaceId", "packageName"])
data class SpaceMemberEntity(
    val spaceId: String,
    val packageName: String,
    val sortOrder: Int
)
