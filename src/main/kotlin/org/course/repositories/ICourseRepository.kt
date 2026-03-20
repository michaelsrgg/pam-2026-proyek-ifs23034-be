package org.course.repositories

import org.course.entities.Course

data class CourseFilter(
    val search: String = "",
    val categoryId: String? = null,
    val level: String? = null,
    val status: String = "published",
    val instructorId: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val sortBy: String = "newest",   // newest | popular | rating | price_asc | price_desc
    val page: Int = 1,
    val perPage: Int = 10,
)

interface ICourseRepository {
    suspend fun getAll(filter: CourseFilter): Pair<List<Course>, Int>
    suspend fun getById(id: String): Course?
    suspend fun getBySlug(slug: String): Course?
    suspend fun slugExists(slug: String): Boolean
    suspend fun create(course: Course): String
    suspend fun update(course: Course): Boolean
    suspend fun updateThumbnail(courseId: String, thumbnail: String?): Boolean
    suspend fun incrementStudents(courseId: String): Boolean
    suspend fun decrementStudents(courseId: String): Boolean
    suspend fun updateStats(courseId: String): Boolean
    suspend fun updateLessonCount(courseId: String): Boolean
    suspend fun delete(courseId: String): Boolean
}
