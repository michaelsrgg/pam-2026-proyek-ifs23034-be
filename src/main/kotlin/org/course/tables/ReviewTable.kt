package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel review/rating kursus oleh siswa.
 * rating: 1-5
 */
object ReviewTable : UUIDTable("reviews") {
    val userId     = uuid("user_id").references(UserTable.id)
    val courseId   = uuid("course_id").references(CourseTable.id)
    val rating     = integer("rating")           // 1 - 5
    val comment    = text("comment").nullable()
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")

    init {
        uniqueIndex(userId, courseId)
    }
}
