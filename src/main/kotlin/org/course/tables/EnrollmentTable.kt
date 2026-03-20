package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel enrollment (pendaftaran siswa ke kursus).
 * status: "active" | "completed" | "cancelled"
 */
object EnrollmentTable : UUIDTable("enrollments") {
    val userId         = uuid("user_id").references(UserTable.id)
    val courseId       = uuid("course_id").references(CourseTable.id)
    val status         = varchar("status", 20).default("active")   // active | completed | cancelled
    val progress       = integer("progress").default(0)            // persentase 0-100
    val paidAmount     = long("paid_amount").default(0)
    val enrolledAt     = timestamp("enrolled_at")
    val completedAt    = timestamp("completed_at").nullable()
    val updatedAt      = timestamp("updated_at")

    init {
        uniqueIndex(userId, courseId)
    }
}
