package com.golfsupporter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.golfsupporter.data.course.local.CachedCourseEntity
import com.golfsupporter.data.course.local.CourseDao
import com.golfsupporter.data.local.dao.GameDao
import com.golfsupporter.data.local.dao.PenaltyTypeDao
import com.golfsupporter.data.local.entity.GameSessionEntity
import com.golfsupporter.data.local.entity.GameSettingsEntity
import com.golfsupporter.data.local.entity.HoleConfigEntity
import com.golfsupporter.data.local.entity.HoleScoreEntity
import com.golfsupporter.data.local.entity.PenaltyRecordEntity
import com.golfsupporter.data.local.entity.PenaltyTypeEntity
import com.golfsupporter.data.local.entity.PlayerEntity
import com.golfsupporter.data.local.entity.ScoreEditEntity

@Database(
    entities = [
        GameSessionEntity::class,
        GameSettingsEntity::class,
        PlayerEntity::class,
        HoleConfigEntity::class,
        HoleScoreEntity::class,
        PenaltyRecordEntity::class,
        PenaltyTypeEntity::class,
        ScoreEditEntity::class,
        CachedCourseEntity::class,
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GolfDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun penaltyTypeDao(): PenaltyTypeDao
    abstract fun courseDao(): CourseDao

    companion object {
        const val NAME = "golf_score_tracker.db"
    }
}
