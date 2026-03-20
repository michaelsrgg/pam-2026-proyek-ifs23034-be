package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel untuk menyimpan data pengguna.
 * Role: "student" atau "instructor"
 */
object UserTable : UUIDTable("users") {
    val name       = varchar("name", 150)
    val username   = varchar("username", 80).uniqueIndex()
    val email      = varchar("email", 200).uniqueIndex()
    val password   = varchar("password", 255)
    val role       = varchar("role", 20).default("student")      // student | instructor
    val bio        = text("bio").nullable()
    val photo      = varchar("photo", 500).nullable()
    val isActive   = bool("is_active").default(true)
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
}
