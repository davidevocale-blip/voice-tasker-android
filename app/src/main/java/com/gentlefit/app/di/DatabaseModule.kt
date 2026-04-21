package com.gentlefit.app.di

import android.content.Context
import androidx.room.Room
import com.gentlefit.app.data.local.GentleFitDatabase
import com.gentlefit.app.data.local.dao.CoachMessageDao
import com.gentlefit.app.data.local.dao.GoalDao
import com.gentlefit.app.data.local.dao.NewsDao
import com.gentlefit.app.data.local.dao.ProgressDao
import com.gentlefit.app.data.local.dao.RoutineDao
import com.gentlefit.app.data.repository.CoachRepositoryImpl
import com.gentlefit.app.data.repository.GoalRepositoryImpl
import com.gentlefit.app.data.repository.NewsRepositoryImpl
import com.gentlefit.app.data.repository.ProgressRepositoryImpl
import com.gentlefit.app.data.repository.RoutineRepositoryImpl
import com.gentlefit.app.domain.repository.CoachRepository
import com.gentlefit.app.domain.repository.GoalRepository
import com.gentlefit.app.domain.repository.NewsRepository
import com.gentlefit.app.domain.repository.ProgressRepository
import com.gentlefit.app.domain.repository.RoutineRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GentleFitDatabase = Room.databaseBuilder(
        context,
        GentleFitDatabase::class.java,
        "gentlefit_database"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideRoutineDao(db: GentleFitDatabase): RoutineDao = db.routineDao()

    @Provides
    fun provideProgressDao(db: GentleFitDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideGoalDao(db: GentleFitDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideNewsDao(db: GentleFitDatabase): NewsDao = db.newsDao()

    @Provides
    fun provideCoachMessageDao(db: GentleFitDatabase): CoachMessageDao = db.coachMessageDao()

    @Provides
    @Singleton
    fun provideRoutineRepository(dao: RoutineDao): RoutineRepository = RoutineRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideProgressRepository(dao: ProgressDao): ProgressRepository = ProgressRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideGoalRepository(dao: GoalDao): GoalRepository = GoalRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideNewsRepository(dao: NewsDao): NewsRepository = NewsRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCoachRepository(dao: CoachMessageDao): CoachRepository = CoachRepositoryImpl(dao)
}
