package com.gentlefit.app.domain.repository

import com.gentlefit.app.domain.model.NewsArticle
import com.gentlefit.app.domain.model.NewsCategory
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getAllNews(): Flow<List<NewsArticle>>
    fun getNewsByCategory(category: NewsCategory): Flow<List<NewsArticle>>
    fun getNewsById(id: Long): Flow<NewsArticle?>
    suspend fun markAsRead(newsId: Long)
    suspend fun insertNews(article: NewsArticle)
}
