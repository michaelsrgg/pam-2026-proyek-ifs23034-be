package org.course.data.response

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

// ── User ─────────────────────────────────────────────────────────────────────

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val role: String,
    val bio: String?,
    val photoUrl: String?,
    val isActive: Boolean,
    @Contextual val createdAt: Instant,
)

@Serializable
data class PublicUserResponse(
    val id: String,
    val name: String,
    val username: String,
    val role: String,
    val bio: String?,
    val photoUrl: String?,
)

// ── Category ──────────────────────────────────────────────────────────────────

@Serializable
data class CategoryResponse(
    val id: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val totalCourses: Int = 0,
    @Contextual val createdAt: Instant,
)

// ── Course ────────────────────────────────────────────────────────────────────

@Serializable
data class CourseResponse(
    val id: String,
    val instructor: PublicUserResponse,
    val category: CategoryResponse?,
    val title: String,
    val slug: String,
    val description: String,
    val thumbnailUrl: String?,
    val level: String,
    val language: String,
    val price: Long,
    val discountPrice: Long?,
    val finalPrice: Long,
    val status: String,
    val totalDuration: Int,
    val totalLessons: Int,
    val totalStudents: Int,
    val avgRating: Double,
    val totalReviews: Int,
    val isEnrolled: Boolean = false,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
)

@Serializable
data class CourseListResponse(
    val id: String,
    val instructorName: String,
    val categoryName: String?,
    val title: String,
    val slug: String,
    val thumbnailUrl: String?,
    val level: String,
    val price: Long,
    val discountPrice: Long?,
    val finalPrice: Long,
    val totalDuration: Int,
    val totalLessons: Int,
    val avgRating: Double,
    val totalStudents: Int,
    @Contextual val updatedAt: Instant,
)

// ── Lesson ────────────────────────────────────────────────────────────────────

@Serializable
data class LessonResponse(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String?,
    val contentType: String,
    val contentUrl: String?,
    val content: String?,
    val duration: Int,
    val orderIndex: Int,
    val isFree: Boolean,
    val isPublished: Boolean,
    val isCompleted: Boolean = false,
    val watchedSeconds: Int = 0,
    @Contextual val createdAt: Instant,
)

@Serializable
data class LessonListResponse(
    val id: String,
    val title: String,
    val contentType: String,
    val duration: Int,
    val orderIndex: Int,
    val isFree: Boolean,
    val isPublished: Boolean,
    val isCompleted: Boolean = false,
)

// ── Enrollment ────────────────────────────────────────────────────────────────

@Serializable
data class EnrollmentResponse(
    val id: String,
    val course: CourseListResponse,
    val status: String,
    val progress: Int,
    val paidAmount: Long,
    @Contextual val enrolledAt: Instant,
    @Contextual val completedAt: Instant?,
)

// ── Review ─────────────────────────────────────────────────────────────────────

@Serializable
data class ReviewResponse(
    val id: String,
    val user: PublicUserResponse,
    val courseId: String,
    val rating: Int,
    val comment: String?,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
)

// ── Dashboard (Instructor) ─────────────────────────────────────────────────────

@Serializable
data class InstructorDashboardResponse(
    val totalCourses: Int,
    val totalStudents: Int,
    val totalRevenue: Long,
    val avgRating: Double,
    val recentCourses: List<CourseListResponse>,
)
