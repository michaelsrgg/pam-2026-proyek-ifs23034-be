package org.course.repositories

import org.course.entities.Enrollment

interface IEnrollmentRepository {
    suspend fun getByUserId(userId: String, status: String? = null, page: Int = 1, perPage: Int = 10): Pair<List<Enrollment>, Int>
    suspend fun getByCourseId(courseId: String, page: Int = 1, perPage: Int = 10): Pair<List<Enrollment>, Int>
    suspend fun getByUserAndCourse(userId: String, courseId: String): Enrollment?
    suspend fun create(enrollment: Enrollment): String
    suspend fun updateProgress(userId: String, courseId: String, progress: Int): Boolean
    suspend fun complete(userId: String, courseId: String): Boolean
    suspend fun cancel(userId: String, courseId: String): Boolean
    suspend fun isEnrolled(userId: String, courseId: String): Boolean
}
