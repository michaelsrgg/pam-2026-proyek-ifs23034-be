package org.course.repositories

import org.course.entities.User

interface IUserRepository {
    suspend fun getById(id: String): User?
    suspend fun getByUsername(username: String): User?
    suspend fun getByEmail(email: String): User?
    suspend fun getByUsernameOrEmail(usernameOrEmail: String): User?
    suspend fun create(user: User): String
    suspend fun update(user: User): Boolean
    suspend fun updatePhoto(userId: String, photo: String?): Boolean
    suspend fun delete(userId: String): Boolean
}
