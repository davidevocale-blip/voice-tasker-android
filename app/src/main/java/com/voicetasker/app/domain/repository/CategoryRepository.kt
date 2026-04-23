package com.voicetasker.app.domain.repository

import com.voicetasker.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryByName(name: String): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategoryById(categoryId: Long)
}
