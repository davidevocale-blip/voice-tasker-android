package com.gentlefit.app.data.repository

import com.gentlefit.app.data.local.dao.GoalDao
import com.gentlefit.app.data.local.entity.GoalEntity
import com.gentlefit.app.domain.model.GoalCategory
import com.gentlefit.app.domain.model.MicroGoal
import com.gentlefit.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getActiveGoals(): Flow<List<MicroGoal>> {
        return goalDao.getActiveGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getCompletedGoals(): Flow<List<MicroGoal>> {
        return goalDao.getCompletedGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getSuggestedGoals(): Flow<List<MicroGoal>> {
        val today = LocalDate.now().format(dateFormatter)
        return flowOf(
            listOf(
                MicroGoal(title = "Bevi 1 bicchiere d'acqua in più", description = "Aggiungi un bicchiere d'acqua alla tua giornata", category = GoalCategory.ACQUA, createdDate = today),
                MicroGoal(title = "Muoviti 5 minuti", description = "Fai una piccola camminata o stretching", category = GoalCategory.MOVIMENTO, createdDate = today),
                MicroGoal(title = "Respira profondamente 3 volte", description = "Fermati e fai 3 respiri profondi", category = GoalCategory.RELAX, createdDate = today),
                MicroGoal(title = "Mangia una porzione di frutta", description = "Aggiungi un frutto come spuntino", category = GoalCategory.ALIMENTAZIONE, createdDate = today),
                MicroGoal(title = "Fai 2000 passi in più", description = "Cammina un po' di più oggi", category = GoalCategory.MOVIMENTO, createdDate = today),
                MicroGoal(title = "Riduci lo zucchero nel caffè", description = "Prova con meno zucchero oggi", category = GoalCategory.ALIMENTAZIONE, createdDate = today),
                MicroGoal(title = "Stacca dallo schermo 10 min", description = "Prenditi una pausa dagli schermi", category = GoalCategory.RELAX, createdDate = today),
                MicroGoal(title = "Bevi una tisana", description = "Preparati una tisana rilassante", category = GoalCategory.ACQUA, createdDate = today),
                MicroGoal(title = "Fai 5 minuti di stretching", description = "Allungati dolcemente per 5 minuti", category = GoalCategory.MOVIMENTO, createdDate = today),
                MicroGoal(title = "Mangia più verdure a pranzo", description = "Aggiungi una porzione extra di verdure", category = GoalCategory.ALIMENTAZIONE, createdDate = today),
                MicroGoal(title = "Ascolta musica rilassante", description = "Metti su musica calma per 10 minuti", category = GoalCategory.RELAX, createdDate = today),
                MicroGoal(title = "Vai a letto 15 min prima", description = "Stasera prova ad andare a dormire un po' prima", category = GoalCategory.RELAX, createdDate = today),
            )
        )
    }

    override suspend fun addGoal(goal: MicroGoal) {
        goalDao.insertGoal(goal.toEntity())
    }

    override suspend fun completeGoal(goalId: Long, completedDate: String) {
        goalDao.completeGoal(goalId, completedDate)
    }

    override suspend fun updateStreak(goalId: Long, streakDays: Int) {
        goalDao.updateStreak(goalId, streakDays)
    }

    override suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteGoal(goalId)
    }

    private fun GoalEntity.toDomain(): MicroGoal = MicroGoal(
        id = id,
        title = title,
        description = description,
        category = try { GoalCategory.valueOf(category) } catch (e: Exception) { GoalCategory.MOVIMENTO },
        isCompleted = isCompleted,
        createdDate = createdDate,
        completedDate = completedDate,
        streakDays = streakDays
    )

    private fun MicroGoal.toEntity(): GoalEntity = GoalEntity(
        id = id,
        title = title,
        description = description,
        category = category.name,
        isCompleted = isCompleted,
        createdDate = createdDate,
        completedDate = completedDate,
        streakDays = streakDays
    )
}
