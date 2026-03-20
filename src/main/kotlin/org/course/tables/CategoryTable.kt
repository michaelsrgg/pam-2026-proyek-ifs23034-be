package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel untuk kategori kursus, contoh: "Web Development", "Mobile", "Data Science"
 */
object CategoryTable : UUIDTable("categories") {
    val name        = varchar("name", 100).uniqueIndex()
    val description = text("description").nullable()
    val icon        = varchar("icon", 100).nullable()   // nama icon atau emoji
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}
