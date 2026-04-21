package com.gentlefit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gentlefit.app.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM progress ORDER BY date DESC")
    fun getAllProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE date = :date LIMIT 1")
    fun getProgressByDate(date: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress ORDER BY date DESC LIMIT :days")
    fun getRecentProgress(days: Int): Flow<List<ProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity): Long

    @Update
    suspend fun updateProgress(progress: ProgressEntity)

    @Query("SELECT AVG(energyLevel) FROM progress ORDER BY date DESC LIMIT :days")
    fun getAverageEnergy(days: Int): Flow<Float>

    @Query("SELECT AVG(sleepQuality) FROM progress ORDER BY date DESC LIMIT :days")
    fun getAverageSleep(days: Int): Flow<Float>
}
