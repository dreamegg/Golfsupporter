package com.golfsupporter.di

import android.content.Context
import androidx.room.Room
import com.golfsupporter.data.course.local.CourseDao
import com.golfsupporter.data.local.GolfDatabase
import com.golfsupporter.data.local.dao.GameDao
import com.golfsupporter.data.local.dao.PenaltyTypeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GolfDatabase =
        Room.databaseBuilder(context, GolfDatabase::class.java, GolfDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideGameDao(db: GolfDatabase): GameDao = db.gameDao()

    @Provides
    fun providePenaltyTypeDao(db: GolfDatabase): PenaltyTypeDao = db.penaltyTypeDao()

    @Provides
    fun provideCourseDao(db: GolfDatabase): CourseDao = db.courseDao()
}
