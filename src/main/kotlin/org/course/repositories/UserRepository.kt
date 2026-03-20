package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.User
import org.course.tables.UserTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserRepository : IUserRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toUser() = User(
        id        = this[UserTable.id].value.toString(),
        name      = this[UserTable.name],
        username  = this[UserTable.username],
        email     = this[UserTable.email],
        password  = this[UserTable.password],
        role      = this[UserTable.role],
        bio       = this[UserTable.bio],
        photo     = this[UserTable.photo],
        isActive  = this[UserTable.isActive],
        createdAt = this[UserTable.createdAt],
        updatedAt = this[UserTable.updatedAt],
    )

    override suspend fun getById(id: String): User? = dbQuery {
        UserTable.selectAll()
            .where { UserTable.id eq UUID.fromString(id) }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun getByUsername(username: String): User? = dbQuery {
        UserTable.selectAll()
            .where { UserTable.username eq username.lowercase() }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun getByEmail(email: String): User? = dbQuery {
        UserTable.selectAll()
            .where { UserTable.email eq email.lowercase() }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun getByUsernameOrEmail(usernameOrEmail: String): User? = dbQuery {
        val value = usernameOrEmail.lowercase()
        UserTable.selectAll()
            .where { (UserTable.username eq value) or (UserTable.email eq value) }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun create(user: User): String = dbQuery {
        val now = Clock.System.now()
        UserTable.insert {
            it[id]        = UUID.fromString(user.id)
            it[name]      = user.name
            it[username]  = user.username.lowercase()
            it[email]     = user.email.lowercase()
            it[password]  = user.password
            it[role]      = user.role
            it[bio]       = user.bio
            it[photo]     = user.photo
            it[isActive]  = user.isActive
            it[createdAt] = now
            it[updatedAt] = now
        }
        user.id
    }

    override suspend fun update(user: User): Boolean = dbQuery {
        val rows = UserTable.update({ UserTable.id eq UUID.fromString(user.id) }) {
            it[name]      = user.name
            it[bio]       = user.bio
            it[password]  = user.password
            it[updatedAt] = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun updatePhoto(userId: String, photo: String?): Boolean = dbQuery {
        val rows = UserTable.update({ UserTable.id eq UUID.fromString(userId) }) {
            it[UserTable.photo]     = photo
            it[UserTable.updatedAt] = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun delete(userId: String): Boolean = dbQuery {
        val rows = UserTable.deleteWhere { id eq UUID.fromString(userId) }
        rows > 0
    }
}
