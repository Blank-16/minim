package com.minim.launcher.data.repository

import android.content.Context
import com.minim.launcher.data.db.MinimDatabase
import com.minim.launcher.data.db.UsageEventEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Powers the "smart suggestions" row. Deliberately simple: a frequency+recency
 * SQL query over a 21-day rolling window, no cloud calls, no on-device ML runtime.
 * This keeps suggestion quality "good enough" while adding effectively zero
 * battery or latency cost.
 */
class SuggestionRepository(context: Context) {
    private val db = MinimDatabase.get(context.applicationContext)
    private val usageDao = db.usageDao()

    private val windowMillis = TimeUnit.DAYS.toMillis(21)

    suspend fun recordLaunch(packageName: String) {
        val cal = Calendar.getInstance()
        usageDao.insert(
            UsageEventEntity(
                packageName = packageName,
                timestampMillis = cal.timeInMillis,
                hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            )
        )
        // Prune opportunistically on write instead of a scheduled job — avoids
        // any background wake-up purely for housekeeping.
        usageDao.pruneOlderThan(System.currentTimeMillis() - windowMillis)
    }

    suspend fun topSuggestions(limit: Int = 5): List<String> {
        val cal = Calendar.getInstance()
        return usageDao.topSuggestions(
            currentHour = cal.get(Calendar.HOUR_OF_DAY),
            currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
            sinceMillis = System.currentTimeMillis() - windowMillis,
            limit = limit
        ).map { it.packageName }
    }
}
