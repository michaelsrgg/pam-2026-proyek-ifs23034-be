package org.course.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabel kursus utama.
 * level: "beginner" | "intermediate" | "advanced"
 * status: "draft" | "published" | "archived"
 */
object CourseTable : UUIDTable("courses") {
    val instructorId  = uuid("instructor_id").references(UserTable.id)
    val categoryId    = uuid("category_id").references(CategoryTable.id).nullable()
    val title         = varchar("title", 200)
    val slug          = varchar("slug", 250).uniqueIndex()
    val description   = text("description")
    val thumbnail     = varchar("thumbnail", 500).nullable()
    val level         = varchar("level", 20).default("beginner")      // beginner | intermediate | advanced
    val language      = varchar("language", 50).default("Indonesia")
    val price         = long("price").default(0)                       // dalam Rupiah, 0 = gratis
    val discountPrice = long("discount_price").nullable()
    val status        = varchar("status", 20).default("draft")         // draft | published | archived
    val totalDuration = integer("total_duration").default(0)           // dalam menit
    val totalLessons  = integer("total_lessons").default(0)
    val totalStudents = integer("total_students").default(0)
    val avgRating     = double("avg_rating").default(0.0)
    val totalReviews  = integer("total_reviews").default(0)
    val createdAt     = timestamp("created_at")
    val updatedAt     = timestamp("updated_at")
}
