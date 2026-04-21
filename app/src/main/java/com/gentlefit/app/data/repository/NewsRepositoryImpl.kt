package com.gentlefit.app.data.repository

import com.gentlefit.app.data.local.dao.NewsDao
import com.gentlefit.app.data.local.entity.NewsEntity
import com.gentlefit.app.domain.model.NewsArticle
import com.gentlefit.app.domain.model.NewsCategory
import com.gentlefit.app.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val newsDao: NewsDao
) : NewsRepository {

    override fun getAllNews(): Flow<List<NewsArticle>> {
        return newsDao.getAllNews().map { list -> list.map { it.toDomain() } }
    }

    override fun getNewsByCategory(category: NewsCategory): Flow<List<NewsArticle>> {
        return newsDao.getNewsByCategory(category.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getNewsById(id: Long): Flow<NewsArticle?> {
        return newsDao.getNewsById(id).map { it?.toDomain() }
    }

    override suspend fun markAsRead(newsId: Long) {
        newsDao.markAsRead(newsId)
    }

    override suspend fun insertNews(article: NewsArticle) {
        newsDao.insertNews(article.toEntity())
    }

    private fun NewsEntity.toDomain(): NewsArticle = NewsArticle(
        id = id,
        title = title,
        summary = summary,
        content = content,
        category = try { NewsCategory.valueOf(category) } catch (e: Exception) { NewsCategory.BENESSERE },
        imageResName = imageResName,
        publishedDate = publishedDate,
        isRead = isRead
    )

    private fun NewsArticle.toEntity(): NewsEntity = NewsEntity(
        id = id,
        title = title,
        summary = summary,
        content = content,
        category = category.name,
        imageResName = imageResName,
        publishedDate = publishedDate,
        isRead = isRead
    )
}
