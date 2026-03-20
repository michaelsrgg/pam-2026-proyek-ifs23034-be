package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel untuk menyimpan refresh token JWT.
 */
object RefreshTokenTable : UUIDTable("refresh_tokens") {
    val userId       = uuid("user_id").references(UserTable.id)
    val authToken    = text("auth_token")
    val refreshToken = text("refresh_token")
    val createdAt    = timestamp("created_at")
}
