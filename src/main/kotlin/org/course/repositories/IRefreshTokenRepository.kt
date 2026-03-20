package org.course.repositories

import org.course.entities.RefreshToken

interface IRefreshTokenRepository {
    suspend fun create(token: RefreshToken): String
    suspend fun getByTokenPair(authToken: String, refreshToken: String): RefreshToken?
    suspend fun deleteByAuthToken(authToken: String): Boolean
    suspend fun deleteByUserId(userId: String): Boolean
}
