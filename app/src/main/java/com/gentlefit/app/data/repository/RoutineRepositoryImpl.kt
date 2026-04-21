package com.gentlefit.app.data.repository

import com.gentlefit.app.data.local.dao.RoutineDao
import com.gentlefit.app.data.local.entity.RoutineEntity
import com.gentlefit.app.domain.model.DailyGoal
import com.gentlefit.app.domain.model.DailyRoutine
import com.gentlefit.app.domain.model.Exercise
import com.gentlefit.app.domain.model.ExerciseType
import com.gentlefit.app.domain.model.FoodTip
import com.gentlefit.app.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao
) : RoutineRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getTodayRoutine(): Flow<DailyRoutine?> {
        val today = LocalDate.now().format(dateFormatter)
        return routineDao.getRoutineByDate(today).map { it?.toDomain() }
    }

    override fun getRoutineByDate(date: String): Flow<DailyRoutine?> {
        return routineDao.getRoutineByDate(date).map { it?.toDomain() }
    }

    override suspend fun completeExercise(routineId: Long) {
        routineDao.completeExercise(routineId)
    }

    override suspend fun completeFoodTip(routineId: Long) {
        routineDao.completeFoodTip(routineId)
    }

    override suspend fun completeGoal(routineId: Long) {
        routineDao.completeGoal(routineId)
    }

    override suspend fun insertRoutine(routine: DailyRoutine) {
        routineDao.insertRoutine(routine.toEntity())
    }

    override fun getCompletedDaysCount(): Flow<Int> {
        return routineDao.getCompletedDaysCount()
    }

    override fun getCurrentStreak(): Flow<Int> {
        return routineDao.getActiveRoutines().map { routines ->
            var streak = 0
            val today = LocalDate.now()
            for (i in routines.indices) {
                val routineDate = LocalDate.parse(routines[i].date, dateFormatter)
                val expectedDate = today.minusDays(i.toLong())
                if (routineDate == expectedDate &&
                    (routines[i].isExerciseCompleted || routines[i].isFoodTipFollowed || routines[i].isGoalCompleted)
                ) {
                    streak++
                } else {
                    break
                }
            }
            streak
        }
    }

    private fun RoutineEntity.toDomain(): DailyRoutine = DailyRoutine(
        id = id,
        date = date,
        exercise = Exercise(
            title = exerciseTitle,
            description = exerciseDescription,
            durationMinutes = exerciseDurationMin,
            type = try { ExerciseType.valueOf(exerciseType) } catch (e: Exception) { ExerciseType.STRETCHING }
        ),
        foodTip = FoodTip(text = foodTip),
        dailyGoal = DailyGoal(text = dailyGoal),
        isExerciseCompleted = isExerciseCompleted,
        isFoodTipFollowed = isFoodTipFollowed,
        isGoalCompleted = isGoalCompleted
    )

    private fun DailyRoutine.toEntity(): RoutineEntity = RoutineEntity(
        id = id,
        date = date,
        exerciseTitle = exercise.title,
        exerciseDescription = exercise.description,
        exerciseDurationMin = exercise.durationMinutes,
        exerciseType = exercise.type.name,
        foodTip = foodTip.text,
        dailyGoal = dailyGoal.text,
        isExerciseCompleted = isExerciseCompleted,
        isFoodTipFollowed = isFoodTipFollowed,
        isGoalCompleted = isGoalCompleted
    )
}
