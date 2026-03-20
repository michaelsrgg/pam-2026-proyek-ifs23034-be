package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel lesson (materi dalam kursus).
 * contentType: "video" | "text" | "quiz"
 */
object LessonTable : UUIDTable("lessons") {
    val courseId     = uuid("course_id").references(CourseTable.id)
    val title        = varchar("title", 200)
    val description  = text("description").nullable()
    val contentType  = varchar("content_type", 20).default("video")   // video | text | quiz
    val contentUrl   = text("content_url").nullable()                  // URL video atau path file
    val content      = text("content").nullable()                      // Konten teks/html jika tipe text
    val duration     = integer("duration").default(0)                  // dalam menit
    val orderIndex   = integer("order_index").default(0)               // urutan dalam kursus
    val isFree       = bool("is_free").default(false)                  // preview gratis?
    val isPublished  = bool("is_published").default(false)
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")
}
