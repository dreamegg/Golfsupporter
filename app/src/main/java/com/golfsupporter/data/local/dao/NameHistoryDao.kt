package com.golfsupporter.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.golfsupporter.data.local.entity.RememberedNameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NameHistoryDao {

    @Upsert
    suspend fun upsertAll(names: List<RememberedNameEntity>)

    @Query("SELECT name FROM remembered_names ORDER BY lastUsedAt DESC LIMIT 12")
    fun observeRecent(): Flow<List<String>>
}
