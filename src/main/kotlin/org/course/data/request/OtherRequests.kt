package org.course.data.request

import kotlinx.serialization.Serializable
import org.course.entities.Category
import org.course.entities.Lesson
import org.course.entities.Review

// ── Lesson ──────────────────────────────────────────────────────────────────

@Serializable
data class LessonRequest(
    val title: String = "",
    val description: String? = null,
    val contentType: String = "video",
    val contentUrl: String? = null,
    val content: String? = null,
    val duration: Int = 0,
    val orderIndex: Int = 0,
    val isFree: Boolean = false,
    val isPublished: Boolean = false,
) {
    fun toMap() = mapOf(
        "title" to title,
        "contentType" to contentType,
    )

    fun toEntity(courseId: String) = Lesson(
        courseId = courseId,
        title = title.trim(),
        description = description?.trim(),
        contentType = when (contentType) { "text", "quiz" -> contentType else -> "video" },
        contentUrl = contentUrl?.trim(),
        content = content?.trim(),
        duration = if (duration < 0) 0 else duration,
        orderIndex = if (orderIndex < 0) 0 else orderIndex,
        isFree = isFree,
        isPublished = isPublished,
    )
}

@Serializable
data class LessonProgressRequest(
    val watchedSeconds: Int = 0,
    val isCompleted: Boolean = false,
) {
    fun toMap() = mapOf("watchedSeconds" to watchedSeconds.toString())
}

// ── Category ─────────────────────────────────────────────────────────────────

@Serializable
data class CategoryRequest(
    val name: String = "",
    val description: String? = null,
    val icon: String? = null,
) {
    fun toMap() = mapOf("name" to name)

    fun toEntity() = Category(
        name = name.trim(),
        description = description?.trim(),
        icon = icon?.trim(),
    )
}

// ── Review ────────────────────────────────────────────────────────────────────

@Serializable
data class ReviewRequest(
    val rating: Int = 0,
    val comment: String? = null,
) {
    fun toMap() = mapOf("rating" to rating.toString())

    fun toEntity(userId: String, courseId: String) = Review(
        userId = userId,
        courseId = courseId,
        rating = rating.coerceIn(1, 5),
        comment = comment?.trim(),
    )
}
