package com.minim.launcher.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Deliberately the only "glanceable info" source in the launcher — a next-
 * calendar-event line is queryable entirely on-device via CalendarContract,
 * with zero network calls, which is what keeps it consistent with the rest
 * of Minim's privacy story. Weather was considered and left out on purpose:
 * every weather source requires either an API key and a network permission
 * (undermining the zero-INTERNET-permission story) or bundling stale offline
 * data, neither of which fit. If you want weather anyway, the settings
 * toggle and info-line rendering here are already generic — swap the data
 * source in CalendarPeek.nextEvent for one, but expect to add
 * android.permission.INTERNET to the manifest to do it honestly.
 */
object CalendarPeek {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Null if no permission, no calendar provider, or nothing in the next 24h. */
    fun nextEventLine(context: Context): String? {
        if (!hasPermission(context)) return null

        val now = System.currentTimeMillis()
        val windowEnd = now + 24 * 60 * 60 * 1000L
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath(windowEnd.toString())
            .build()
        val projection = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN)

        return runCatching {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "Untitled event"
                val beginMillis = cursor.getLong(1)
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "$title · ${timeFormat.format(beginMillis)}"
            }
        }.getOrNull()
    }
}
