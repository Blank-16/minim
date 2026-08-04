package com.minim.launcher.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCacheDao {

    @Query("SELECT * FROM app_cache WHERE hidden = 0 ORDER BY label COLLATE NOCASE ASC")
    fun observeVisibleApps(): Flow<List<AppCacheEntity>>

    @Query("SELECT * FROM app_cache WHERE hidden = 1 ORDER BY label COLLATE NOCASE ASC")
    fun observeHiddenApps(): Flow<List<AppCacheEntity>>

    @Query("SELECT * FROM app_cache WHERE hidden = 0 AND installTimeMillis > :sinceMillis ORDER BY installTimeMillis DESC")
    fun observeRecentlyInstalled(sinceMillis: Long): Flow<List<AppCacheEntity>>

    @Query("SELECT * FROM app_cache WHERE favorite = 1 AND hidden = 0 ORDER BY label COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<AppCacheEntity>>

    @Query("SELECT * FROM app_cache")
    suspend fun getAllOnce(): List<AppCacheEntity>

    @Upsert
    suspend fun upsertAll(apps: List<AppCacheEntity>)

    @Upsert
    suspend fun upsert(app: AppCacheEntity)

    @Query("DELETE FROM app_cache WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE app_cache SET favorite = :favorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, favorite: Boolean)

    @Query("UPDATE app_cache SET hidden = :hidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, hidden: Boolean)

    @Query("UPDATE app_cache SET quickAction = :action WHERE packageName = :packageName")
    suspend fun setQuickAction(packageName: String, action: String?)
}

@Dao
interface UsageDao {

    @Insert
    suspend fun insert(event: UsageEventEntity)

    /**
     * Frequency+recency score per package, computed entirely on-device with SQL —
     * no ML runtime, no network. Recent launches and launches at a similar hour
     * of day are weighted higher. This backs the "smart suggestions" row.
     */
    @Query(
        """
        SELECT packageName, 
               COUNT(*) * 1.0 
                 + SUM(CASE WHEN ABS(hourOfDay - :currentHour) <= 1 THEN 2.0 ELSE 0.0 END)
                 + SUM(CASE WHEN dayOfWeek = :currentDayOfWeek THEN 1.0 ELSE 0.0 END)
                 AS score
        FROM usage_events
        WHERE timestampMillis > :sinceMillis
        GROUP BY packageName
        ORDER BY score DESC
        LIMIT :limit
        """
    )
    suspend fun topSuggestions(
        currentHour: Int,
        currentDayOfWeek: Int,
        sinceMillis: Long,
        limit: Int = 5
    ): List<PackageScore>

    @Query("DELETE FROM usage_events WHERE timestampMillis < :cutoffMillis")
    suspend fun pruneOlderThan(cutoffMillis: Long)
}

data class PackageScore(
    val packageName: String,
    val score: Double
)
