package com.minim.launcher.data.repository

import android.content.Context
import com.minim.launcher.data.db.MinimDatabase
import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.data.db.SpaceMemberEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class SpacesRepository(context: Context) {
    private val dao = MinimDatabase.get(context.applicationContext).spaceDao()

    fun observeSpaces(): Flow<List<SpaceEntity>> = dao.observeSpaces()

    fun observeMembers(spaceId: String): Flow<List<String>> = dao.observeMembers(spaceId)

    suspend fun createSpace(name: String, sortOrder: Int = 0): String {
        val id = UUID.randomUUID().toString()
        dao.upsertSpace(SpaceEntity(id = id, name = name, sortOrder = sortOrder))
        return id
    }

    suspend fun updateSpace(space: SpaceEntity) = dao.upsertSpace(space)

    suspend fun deleteSpace(spaceId: String) {
        dao.clearMembers(spaceId)
        dao.deleteSpace(spaceId)
    }

    suspend fun addAppToSpace(spaceId: String, packageName: String, sortOrder: Int = 0) =
        dao.addMember(SpaceMemberEntity(spaceId, packageName, sortOrder))

    suspend fun removeAppFromSpace(spaceId: String, packageName: String) =
        dao.removeMember(spaceId, packageName)

    suspend fun getSpace(spaceId: String): SpaceEntity? = dao.getSpaceOnce(spaceId)

    /**
     * Checked on app resume only (never a background job/alarm — keeps the
     * zero-polling philosophy intact). Pure function over an already-fetched
     * list so it can also run inside a reactive Flow combine without
     * suspending. Returns the first Space whose auto-activation window
     * contains the current time and day, or null if none matches.
     */
    fun resolveAutoActivated(spaces: List<SpaceEntity>, now: Calendar = Calendar.getInstance()): SpaceEntity? {
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val dayBit = 1 shl (now.get(Calendar.DAY_OF_WEEK) - 1) // Calendar.SUNDAY == 1

        return spaces.firstOrNull { space ->
            val start = space.autoActivateStartHour
            val end = space.autoActivateEndHour
            if (start == null || end == null) return@firstOrNull false

            val daysMask = space.autoActivateDaysMask
            val dayMatches = daysMask == null || (daysMask and dayBit) != 0
            if (!dayMatches) return@firstOrNull false

            if (start <= end) hour in start until end else (hour >= start || hour < end)
        }
    }

    suspend fun findAutoActivatedSpace(now: Calendar = Calendar.getInstance()): SpaceEntity? =
        resolveAutoActivated(dao.getAllSpacesOnce(), now)
}
