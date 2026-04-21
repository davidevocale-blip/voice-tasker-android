package com.gentlefit.app.data.repository

import com.gentlefit.app.data.local.dao.ProgressDao
import com.gentlefit.app.data.local.entity.ProgressEntity
import com.gentlefit.app.domain.model.Mood
import com.gentlefit.app.domain.model.ProgressEntry
import com.gentlefit.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao
) : ProgressRepository {

    override fun getAllProgress(): Flow<List<ProgressEntry>> {
        return progressDao.getAllProgress().map { list -> list.map { it.toDomain() } }
    }

    override fun getProgressByDate(date: String): Flow<ProgressEntry?> {
        return progressDao.getProgressByDate(date).map { it?.toDomain() }
    }

    override fun getRecentProgress(days: Int): Flow<List<ProgressEntry>> {
        return progressDao.getRecentProgress(days).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun logProgress(entry: ProgressEntry) {
        progressDao.insertProgress(entry.toEntity())
    }

    override suspend fun updateProgress(entry: ProgressEntry) {
        progressDao.updateProgress(entry.toEntity())
    }

    override fun getAverageEnergy(days: Int): Flow<Float> {
        return progressDao.getAverageEnergy(days)
    }

    override fun getAverageSleep(days: Int): Flow<Float> {
        return progressDao.getAverageSleep(days)
    }

    private fun ProgressEntity.toDomain(): ProgressEntry = ProgressEntry(
        id = id,
        date = date,
        weight = weight,
        energyLevel = energyLevel,
        sleepQuality = sleepQuality,
        mood = try { Mood.valueOf(mood) } catch (e: Exception) { Mood.NEUTRAL },
        note = note
    )

    private fun ProgressEntry.toEntity(): ProgressEntity = ProgressEntity(
        id = id,
        date = date,
        weight = weight,
        energyLevel = energyLevel,
        sleepQuality = sleepQuality,
        mood = mood.name,
        note = note
    )
}
