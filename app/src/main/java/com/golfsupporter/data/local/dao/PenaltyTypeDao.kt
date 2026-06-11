package com.golfsupporter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.golfsupporter.data.local.entity.PenaltyTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PenaltyTypeDao {

    @Upsert
    suspend fun upsert(type: PenaltyTypeEntity)

    @Upsert
    suspend fun upsertAll(types: List<PenaltyTypeEntity>)

    @Delete
    suspend fun delete(type: PenaltyTypeEntity)

    @Query("DELETE FROM penalty_types WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: String)

    @Query("SELECT * FROM penalty_types ORDER BY sortOrder")
    fun observeAll(): Flow<List<PenaltyTypeEntity>>

    @Query("SELECT * FROM penalty_types ORDER BY sortOrder")
    suspend fun getAll(): List<PenaltyTypeEntity>

    @Query("SELECT COUNT(*) FROM penalty_types")
    suspend fun count(): Int
}
