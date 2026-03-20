package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel untuk melacak progress lesson per siswa.
 */
object LessonProgressTable : UUIDTable("lesson_progress") {
    val userId        = uuid("user_id").references(UserTable.id)
    val lessonId      = uuid("lesson_id").references(LessonTable.id)
    val courseId      = uuid("course_id").references(CourseTable.id)
    val isCompleted   = bool("is_completed").default(false)
    val watchedSeconds = integer("watched_seconds").default(0)     // untuk video
    val completedAt   = timestamp("completed_at").nullable()
    val createdAt     = timestamp("created_at")
    val updatedAt     = timestamp("updated_at")

    init {
        uniqueIndex(userId, lessonId)
    }
}
