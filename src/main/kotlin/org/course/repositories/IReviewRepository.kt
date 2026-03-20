package org.course.repositories

import org.course.entities.Review

interface IReviewRepository {
    suspend fun getByCourseId(courseId: String, page: Int = 1, perPage: Int = 10): Pair<List<Review>, Int>
    suspend fun getByUserAndCourse(userId: String, courseId: String): Review?
    suspend fun create(review: Review): String
    suspend fun update(review: Review): Boolean
    suspend fun delete(reviewId: String): Boolean
    suspend fun hasReviewed(userId: String, courseId: String): Boolean
}
