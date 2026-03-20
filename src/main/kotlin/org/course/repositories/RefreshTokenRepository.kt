package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.RefreshToken
import org.course.tables.RefreshTokenTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class RefreshTokenRepository : IRefreshTokenRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toRefreshToken() = RefreshToken(
        id           = this[RefreshTokenTable.id].value.toString(),
        userId       = this[RefreshTokenTable.userId].toString(),
        authToken    = this[RefreshTokenTable.authToken],
        refreshToken = this[RefreshTokenTable.refreshToken],
        createdAt    = this[RefreshTokenTable.createdAt],
    )

    override suspend fun create(token: RefreshToken): String = dbQuery {
        RefreshTokenTable.insert {
            it[id]           = UUID.fromString(token.id)
            it[userId]       = UUID.fromString(token.userId)
            it[authToken]    = token.authToken
            it[refreshToken] = token.refreshToken
            it[createdAt]    = Clock.System.now()
        }
        token.id
    }

    override suspend fun getByTokenPair(authToken: String, refreshToken: String): RefreshToken? = dbQuery {
        RefreshTokenTable.selectAll()
            .where {
                (RefreshTokenTable.authToken eq authToken) and
                (RefreshTokenTable.refreshToken eq refreshToken)
            }
            .singleOrNull()
            ?.toRefreshToken()
    }

    override suspend fun deleteByAuthToken(authToken: String): Boolean = dbQuery {
        val rows = RefreshTokenTable.deleteWhere { RefreshTokenTable.authToken eq authToken }
        rows > 0
    }

    override suspend fun deleteByUserId(userId: String): Boolean = dbQuery {
        val rows = RefreshTokenTable.deleteWhere { RefreshTokenTable.userId eq UUID.fromString(userId) }
        rows > 0
    }
}
