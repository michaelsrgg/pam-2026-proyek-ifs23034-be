package org.course.repositories

import org.course.entities.Lesson

interface ILessonRepository {
    suspend fun getByCourseId(courseId: String, publishedOnly: Boolean = true): List<Lesson>
    suspend fun getById(id: String): Lesson?
    suspend fun getMaxOrderIndex(courseId: String): Int
    suspend fun create(lesson: Lesson): String
    suspend fun update(lesson: Lesson): Boolean
    suspend fun delete(lessonId: String): Boolean
    suspend fun reorder(courseId: String, lessonIds: List<String>): Boolean
}
