package org.course.data.request

import kotlinx.serialization.Serializable
import org.course.entities.Course

@Serializable
data class CourseRequest(
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val level: String = "beginner",
    val language: String = "Indonesia",
    val price: Long = 0,
    val discountPrice: Long? = null,
    val status: String = "draft",
) {
    fun toMap() = mapOf(
        "title" to title,
        "description" to description,
        "level" to level,
    )

    fun toEntity(instructorId: String, slug: String) = Course(
        instructorId = instructorId,
        categoryId = categoryId,
        title = title.trim(),
        slug = slug,
        description = description.trim(),
        level = when (level) {
            "intermediate", "advanced" -> level
            else -> "beginner"
        },
        language = language.ifBlank { "Indonesia" },
        price = if (price < 0) 0 else price,
        discountPrice = discountPrice?.let { if (it < 0) null else it },
        status = when (status) {
            "published", "archived" -> status
            else -> "draft"
        },
    )
}

@Serializable
data class PublishCourseRequest(
    val status: String = "published",
) {
    fun toMap() = mapOf("status" to status)
}
