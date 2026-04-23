package com.voicetasker.app.data.repository

import com.voicetasker.app.data.local.dao.CategoryDao
import com.voicetasker.app.data.local.entity.CategoryEntity
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(private val dao: CategoryDao) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = dao.getAllCategories().map { list -> list.map { it.toDomain() } }
    override suspend fun getCategoryByName(name: String): Category? = dao.getCategoryByName(name)?.toDomain()
    override suspend fun insertCategory(category: Category): Long = dao.insertCategory(category.toEntity())
    override suspend fun updateCategory(category: Category) = dao.updateCategory(category.toEntity())
    override suspend fun deleteCategoryById(categoryId: Long) { dao.deleteCategoryById(categoryId) }
}

private fun CategoryEntity.toDomain() = Category(id, name, colorHex, iconName, isDefault, createdAt)
private fun Category.toEntity() = CategoryEntity(id, name, colorHex, iconName, isDefault, createdAt)
