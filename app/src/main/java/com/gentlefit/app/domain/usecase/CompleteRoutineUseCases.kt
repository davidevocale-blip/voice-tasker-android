package com.gentlefit.app.domain.usecase

import com.gentlefit.app.domain.repository.RoutineRepository
import javax.inject.Inject

class CompleteExerciseUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    suspend operator fun invoke(routineId: Long) = routineRepository.completeExercise(routineId)
}

class CompleteFoodTipUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    suspend operator fun invoke(routineId: Long) = routineRepository.completeFoodTip(routineId)
}

class CompleteDailyGoalUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    suspend operator fun invoke(routineId: Long) = routineRepository.completeGoal(routineId)
}
