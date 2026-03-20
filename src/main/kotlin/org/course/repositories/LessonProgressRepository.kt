package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.LessonProgress
import org.course.tables.LessonProgressTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class LessonProgressRepository : ILessonProgressRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toProgress() = LessonProgress(
        id             = this[LessonProgressTable.id].value.toString(),
        userId         = this[LessonProgressTable.userId].toString(),
        lessonId       = this[LessonProgressTable.lessonId].toString(),
        courseId       = this[LessonProgressTable.courseId].toString(),
        isCompleted    = this[LessonProgressTable.isCompleted],
        watchedSeconds = this[LessonProgressTable.watchedSeconds],
        completedAt    = this[LessonProgressTable.completedAt],
        createdAt      = this[LessonProgressTable.createdAt],
        updatedAt      = this[LessonProgressTable.updatedAt],
    )

    override suspend fun getByUserAndCourse(userId: String, courseId: String): List<LessonProgress> = dbQuery {
        LessonProgressTable.selectAll()
            .where {
                (LessonProgressTable.userId eq UUID.fromString(userId)) and
                (LessonProgressTable.courseId eq UUID.fromString(courseId))
            }
            .map { it.toProgress() }
    }

    override suspend fun getByUserAndLesson(userId: String, lessonId: String): LessonProgress? = dbQuery {
        LessonProgressTable.selectAll()
            .where {
                (LessonProgressTable.userId eq UUID.fromString(userId)) and
                (LessonProgressTable.lessonId eq UUID.fromString(lessonId))
            }
            .singleOrNull()
            ?.toProgress()
    }

    override suspend fun upsert(progress: LessonProgress): String = dbQuery {
        val now = Clock.System.now()
        val existing = LessonProgressTable.selectAll()
            .where {
                (LessonProgressTable.userId eq UUID.fromString(progress.userId)) and
                (LessonProgressTable.lessonId eq UUID.fromString(progress.lessonId))
            }
            .singleOrNull()

        if (existing != null) {
            // Update existing progress
            LessonProgressTable.update({
                (LessonProgressTable.userId eq UUID.fromString(progress.userId)) and
                (LessonProgressTable.lessonId eq UUID.fromString(progress.lessonId))
            }) {
                it[isCompleted]    = progress.isCompleted
                it[watchedSeconds] = progress.watchedSeconds
                it[completedAt]    = progress.completedAt
                it[updatedAt]      = now
            }
            existing[LessonProgressTable.id].value.toString()
        } else {
            // Insert new progress
            LessonProgressTable.insert {
                it[id]             = UUID.fromString(progress.id)
                it[userId]         = UUID.fromString(progress.userId)
                it[lessonId]       = UUID.fromString(progress.lessonId)
                it[courseId]       = UUID.fromString(progress.courseId)
                it[isCompleted]    = progress.isCompleted
                it[watchedSeconds] = progress.watchedSeconds
                it[completedAt]    = progress.completedAt
                it[createdAt]      = now
                it[updatedAt]      = now
            }
            progress.id
        }
    }

    override suspend fun countCompleted(userId: String, courseId: String): Int = dbQuery {
        LessonProgressTable.selectAll()
            .where {
                (LessonProgressTable.userId eq UUID.fromString(userId)) and
                (LessonProgressTable.courseId eq UUID.fromString(courseId)) and
                (LessonProgressTable.isCompleted eq true)
            }
            .count()
            .toInt()
    }
}
