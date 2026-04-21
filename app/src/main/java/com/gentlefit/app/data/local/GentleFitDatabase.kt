package com.gentlefit.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gentlefit.app.data.local.dao.CoachMessageDao
import com.gentlefit.app.data.local.dao.GoalDao
import com.gentlefit.app.data.local.dao.NewsDao
import com.gentlefit.app.data.local.dao.ProgressDao
import com.gentlefit.app.data.local.dao.RoutineDao
import com.gentlefit.app.data.local.entity.CoachMessageEntity
import com.gentlefit.app.data.local.entity.GoalEntity
import com.gentlefit.app.data.local.entity.NewsEntity
import com.gentlefit.app.data.local.entity.ProgressEntity
import com.gentlefit.app.data.local.entity.RoutineEntity

@Database(
    entities = [
        RoutineEntity::class,
        ProgressEntity::class,
        GoalEntity::class,
        NewsEntity::class,
        CoachMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GentleFitDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun progressDao(): ProgressDao
    abstract fun goalDao(): GoalDao
    abstract fun newsDao(): NewsDao
    abstract fun coachMessageDao(): CoachMessageDao
}
