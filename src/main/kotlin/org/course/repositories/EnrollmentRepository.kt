package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.Enrollment
import org.course.tables.EnrollmentTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class EnrollmentRepository : IEnrollmentRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toEnrollment() = Enrollment(
        id          = this[EnrollmentTable.id].value.toString(),
        userId      = this[EnrollmentTable.userId].toString(),
        courseId    = this[EnrollmentTable.courseId].toString(),
        status      = this[EnrollmentTable.status],
        progress    = this[EnrollmentTable.progress],
        paidAmount  = this[EnrollmentTable.paidAmount],
        enrolledAt  = this[EnrollmentTable.enrolledAt],
        completedAt = this[EnrollmentTable.completedAt],
        updatedAt   = this[EnrollmentTable.updatedAt],
    )

    override suspend fun getByUserId(userId: String, status: String?, page: Int, perPage: Int): Pair<List<Enrollment>, Int> = dbQuery {
        val uuid = UUID.fromString(userId)
        var query = EnrollmentTable.selectAll()
            .where { EnrollmentTable.userId eq uuid }

        if (!status.isNullOrBlank()) {
            query = query.andWhere { EnrollmentTable.status eq status }
        }

        val total = query.count().toInt()
        val offset = ((page - 1) * perPage).toLong()
        val items = query
            .orderBy(EnrollmentTable.enrolledAt, SortOrder.DESC)
            .limit(perPage).offset(offset)
            .map { it.toEnrollment() }
        Pair(items, total)
    }

    override suspend fun getByCourseId(courseId: String, page: Int, perPage: Int): Pair<List<Enrollment>, Int> = dbQuery {
        val uuid = UUID.fromString(courseId)
        val query = EnrollmentTable.selectAll()
            .where { EnrollmentTable.courseId eq uuid }

        val total = query.count().toInt()
        val offset = ((page - 1) * perPage).toLong()
        val items = query
            .orderBy(EnrollmentTable.enrolledAt, SortOrder.DESC)
            .limit(perPage).offset(offset)
            .map { it.toEnrollment() }
        Pair(items, total)
    }

    override suspend fun getByUserAndCourse(userId: String, courseId: String): Enrollment? = dbQuery {
        EnrollmentTable.selectAll()
            .where {
                (EnrollmentTable.userId eq UUID.fromString(userId)) and
                (EnrollmentTable.courseId eq UUID.fromString(courseId))
            }
            .singleOrNull()
            ?.toEnrollment()
    }

    override suspend fun create(enrollment: Enrollment): String = dbQuery {
        val now = Clock.System.now()
        EnrollmentTable.insert {
            it[id]         = UUID.fromString(enrollment.id)
            it[userId]     = UUID.fromString(enrollment.userId)
            it[courseId]   = UUID.fromString(enrollment.courseId)
            it[status]     = enrollment.status
            it[progress]   = enrollment.progress
            it[paidAmount] = enrollment.paidAmount
            it[enrolledAt] = now
            it[updatedAt]  = now
        }
        enrollment.id
    }

    override suspend fun updateProgress(userId: String, courseId: String, progress: Int): Boolean = dbQuery {
        val rows = EnrollmentTable.update({
            (EnrollmentTable.userId eq UUID.fromString(userId)) and
            (EnrollmentTable.courseId eq UUID.fromString(courseId))
        }) {
            it[EnrollmentTable.progress] = progress.coerceIn(0, 100)
            it[updatedAt]                = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun complete(userId: String, courseId: String): Boolean = dbQuery {
        val now = Clock.System.now()
        val rows = EnrollmentTable.update({
            (EnrollmentTable.userId eq UUID.fromString(userId)) and
            (EnrollmentTable.courseId eq UUID.fromString(courseId))
        }) {
            it[status]      = "completed"
            it[progress]    = 100
            it[completedAt] = now
            it[updatedAt]   = now
        }
        rows > 0
    }

    override suspend fun cancel(userId: String, courseId: String): Boolean = dbQuery {
        val rows = EnrollmentTable.update({
            (EnrollmentTable.userId eq UUID.fromString(userId)) and
            (EnrollmentTable.courseId eq UUID.fromString(courseId))
        }) {
            it[status]    = "cancelled"
            it[updatedAt] = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun isEnrolled(userId: String, courseId: String): Boolean = dbQuery {
        EnrollmentTable.selectAll()
            .where {
                (EnrollmentTable.userId eq UUID.fromString(userId)) and
                (EnrollmentTable.courseId eq UUID.fromString(courseId)) and
                (EnrollmentTable.status eq "active")
            }
            .count() > 0
    }
}
