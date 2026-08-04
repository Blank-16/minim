package com.minim.launcher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppCacheEntity::class,
        UsageEventEntity::class,
        SpaceEntity::class,
        SpaceMemberEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MinimDatabase : RoomDatabase() {
    abstract fun appCacheDao(): AppCacheDao
    abstract fun usageDao(): UsageDao
    abstract fun spaceDao(): SpaceDao

    companion object {
        @Volatile private var instance: MinimDatabase? = null

        fun get(context: Context): MinimDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MinimDatabase::class.java,
                    "minim.db"
                )
                    // Launcher must survive schema mistakes gracefully rather than
                    // crash-looping and leaving the user with no home screen.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
