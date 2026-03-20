package org.course.repositories

import org.course.entities.LessonProgress

interface ILessonProgressRepository {
    suspend fun getByUserAndCourse(userId: String, courseId: String): List<LessonProgress>
    suspend fun getByUserAndLesson(userId: String, lessonId: String): LessonProgress?
    suspend fun upsert(progress: LessonProgress): String
    suspend fun countCompleted(userId: String, courseId: String): Int
}
