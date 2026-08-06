package com.smartdispenser.repository

import com.smartdispenser.database.CategoryDao
import com.smartdispenser.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun observeAllCategories(): Flow<List<Category>> = categoryDao.observeAllCategories()

    fun observeCategoryById(categoryId: Long): Flow<Category?> = categoryDao.observeCategoryById(categoryId)

    suspend fun insertCategory(name: String): Long {
        return categoryDao.insertCategory(Category(name = name))
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}