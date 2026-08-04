package com.minim.launcher.data

data class AppInfo(
    val packageName: String,
    val activityClassName: String,
    val label: String,
    val isSystemApp: Boolean,
    val installTimeMillis: Long,
    val favorite: Boolean = false,
    val hidden: Boolean = false,
    val quickAction: String? = null
) {
    /** First letter used for alphabet-index bucketing, "#" for non-alphabetic labels. */
    val indexLetter: Char
        get() = label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
}

/** A single usage event, recorded on app launch, used only for on-device suggestion scoring. */
data class UsageEvent(
    val packageName: String,
    val timestampMillis: Long,
    val hourOfDay: Int,
    val dayOfWeek: Int
)
