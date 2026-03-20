package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.Lesson
import org.course.tables.LessonTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class LessonRepository : ILessonRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toLesson() = Lesson(
        id          = this[LessonTable.id].value.toString(),
        courseId    = this[LessonTable.courseId].toString(),
        title       = this[LessonTable.title],
        description = this[LessonTable.description],
        contentType = this[LessonTable.contentType],
        contentUrl  = this[LessonTable.contentUrl],
        content     = this[LessonTable.content],
        duration    = this[LessonTable.duration],
        orderIndex  = this[LessonTable.orderIndex],
        isFree      = this[LessonTable.isFree],
        isPublished = this[LessonTable.isPublished],
        createdAt   = this[LessonTable.createdAt],
        updatedAt   = this[LessonTable.updatedAt],
    )

    override suspend fun getByCourseId(courseId: String, publishedOnly: Boolean): List<Lesson> = dbQuery {
        var query = LessonTable.selectAll()
            .where { LessonTable.courseId eq UUID.fromString(courseId) }

        if (publishedOnly) {
            query = query.andWhere { LessonTable.isPublished eq true }
        }

        query.orderBy(LessonTable.orderIndex, SortOrder.ASC).map { it.toLesson() }
    }

    override suspend fun getById(id: String): Lesson? = dbQuery {
        LessonTable.selectAll()
            .where { LessonTable.id eq UUID.fromString(id) }
            .singleOrNull()
            ?.toLesson()
    }

    override suspend fun getMaxOrderIndex(courseId: String): Int = dbQuery {
        LessonTable.select(LessonTable.orderIndex)
            .where { LessonTable.courseId eq UUID.fromString(courseId) }
            .orderBy(LessonTable.orderIndex, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.get(LessonTable.orderIndex) ?: 0
    }

    override suspend fun create(lesson: Lesson): String = dbQuery {
        val now = Clock.System.now()
        LessonTable.insert {
            it[id]          = UUID.fromString(lesson.id)
            it[courseId]    = UUID.fromString(lesson.courseId)
            it[title]       = lesson.title
            it[description] = lesson.description
            it[contentType] = lesson.contentType
            it[contentUrl]  = lesson.contentUrl
            it[content]     = lesson.content
            it[duration]    = lesson.duration
            it[orderIndex]  = lesson.orderIndex
            it[isFree]      = lesson.isFree
            it[isPublished] = lesson.isPublished
            it[createdAt]   = now
            it[updatedAt]   = now
        }
        lesson.id
    }

    override suspend fun update(lesson: Lesson): Boolean = dbQuery {
        val rows = LessonTable.update({ LessonTable.id eq UUID.fromString(lesson.id) }) {
            it[title]       = lesson.title
            it[description] = lesson.description
            it[contentType] = lesson.contentType
            it[contentUrl]  = lesson.contentUrl
            it[content]     = lesson.content
            it[duration]    = lesson.duration
            it[orderIndex]  = lesson.orderIndex
            it[isFree]      = lesson.isFree
            it[isPublished] = lesson.isPublished
            it[updatedAt]   = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun delete(lessonId: String): Boolean = dbQuery {
        val rows = LessonTable.deleteWhere { id eq UUID.fromString(lessonId) }
        rows > 0
    }

    override suspend fun reorder(courseId: String, lessonIds: List<String>): Boolean = dbQuery {
        val uuid = UUID.fromString(courseId)
        lessonIds.forEachIndexed { index, lessonId ->
            LessonTable.update({
                (LessonTable.id eq UUID.fromString(lessonId)) and (LessonTable.courseId eq uuid)
            }) {
                it[orderIndex] = index + 1
                it[updatedAt]  = Clock.System.now()
            }
        }
        true
    }
}
