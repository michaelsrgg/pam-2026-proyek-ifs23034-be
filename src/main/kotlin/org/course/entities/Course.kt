package org.course.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Course(
    var id: String = UUID.randomUUID().toString(),
    var instructorId: String,
    var categoryId: String? = null,
    var title: String,
    var slug: String,
    var description: String,
    var thumbnail: String? = null,
    var level: String = "beginner",
    var language: String = "Indonesia",
    var price: Long = 0,
    var discountPrice: Long? = null,
    var status: String = "draft",
    var totalDuration: Int = 0,
    var totalLessons: Int = 0,
    var totalStudents: Int = 0,
    var avgRating: Double = 0.0,
    var totalReviews: Int = 0,
    @Contextual val createdAt: Instant = Clock.System.now(),
    @Contextual var updatedAt: Instant = Clock.System.now(),
)
