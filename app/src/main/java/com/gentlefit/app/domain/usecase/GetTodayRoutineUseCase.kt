package com.gentlefit.app.domain.usecase

import com.gentlefit.app.domain.model.DailyRoutine
import com.gentlefit.app.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodayRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    operator fun invoke(): Flow<DailyRoutine?> = routineRepository.getTodayRoutine()
}
