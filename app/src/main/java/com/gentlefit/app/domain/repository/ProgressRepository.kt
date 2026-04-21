package com.gentlefit.app.domain.repository

import com.gentlefit.app.domain.model.ProgressEntry
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun getAllProgress(): Flow<List<ProgressEntry>>
    fun getProgressByDate(date: String): Flow<ProgressEntry?>
    fun getRecentProgress(days: Int): Flow<List<ProgressEntry>>
    suspend fun logProgress(entry: ProgressEntry)
    suspend fun updateProgress(entry: ProgressEntry)
    fun getAverageEnergy(days: Int): Flow<Float>
    fun getAverageSleep(days: Int): Flow<Float>
}
