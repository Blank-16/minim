package com.minim.launcher.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class SpaceWithMembers(
    val space: SpaceEntity,
    val memberPackages: List<String>
)

@Dao
interface SpaceDao {

    @Query("SELECT * FROM spaces ORDER BY sortOrder ASC")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT packageName FROM space_members WHERE spaceId = :spaceId ORDER BY sortOrder ASC")
    fun observeMembers(spaceId: String): Flow<List<String>>

    @Upsert
    suspend fun upsertSpace(space: SpaceEntity)

    @Query("SELECT * FROM spaces WHERE id = :spaceId")
    suspend fun getSpaceOnce(spaceId: String): SpaceEntity?

    @Query("SELECT * FROM spaces")
    suspend fun getAllSpacesOnce(): List<SpaceEntity>

    @Query("DELETE FROM spaces WHERE id = :spaceId")
    suspend fun deleteSpace(spaceId: String)

    @Query("DELETE FROM space_members WHERE spaceId = :spaceId")
    suspend fun clearMembers(spaceId: String)

    @Upsert
    suspend fun addMember(member: SpaceMemberEntity)

    @Query("DELETE FROM space_members WHERE spaceId = :spaceId AND packageName = :packageName")
    suspend fun removeMember(spaceId: String, packageName: String)
}
