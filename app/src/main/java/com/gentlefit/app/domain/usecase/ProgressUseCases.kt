package com.gentlefit.app.domain.usecase

import com.gentlefit.app.domain.model.ProgressEntry
import com.gentlefit.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(entry: ProgressEntry) = progressRepository.logProgress(entry)
}

class GetRecentProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    operator fun invoke(days: Int = 7): Flow<List<ProgressEntry>> =
        progressRepository.getRecentProgress(days)
}
