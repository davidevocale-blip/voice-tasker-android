package com.gentlefit.app.domain.usecase

import com.gentlefit.app.domain.model.NewsArticle
import com.gentlefit.app.domain.model.NewsCategory
import com.gentlefit.app.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNewsUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(): Flow<List<NewsArticle>> = newsRepository.getAllNews()
}

class GetNewsByCategoryUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(category: NewsCategory): Flow<List<NewsArticle>> =
        newsRepository.getNewsByCategory(category)
}

class MarkNewsReadUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(newsId: Long) = newsRepository.markAsRead(newsId)
}
