package org.course.repositories

import org.course.entities.Category

interface ICategoryRepository {
    suspend fun getAll(): List<Category>
    suspend fun getById(id: String): Category?
    suspend fun getByName(name: String): Category?
    suspend fun create(category: Category): String
    suspend fun update(category: Category): Boolean
    suspend fun delete(id: String): Boolean
    suspend fun countCourses(categoryId: String): Int
}
