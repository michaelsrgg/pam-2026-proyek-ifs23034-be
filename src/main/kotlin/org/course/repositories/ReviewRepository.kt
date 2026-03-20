package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.Review
import org.course.tables.ReviewTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ReviewRepository : IReviewRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toReview() = Review(
        id        = this[ReviewTable.id].value.toString(),
        userId    = this[ReviewTable.userId].toString(),
        courseId  = this[ReviewTable.courseId].toString(),
        rating    = this[ReviewTable.rating],
        comment   = this[ReviewTable.comment],
        createdAt = this[ReviewTable.createdAt],
        updatedAt = this[ReviewTable.updatedAt],
    )

    override suspend fun getByCourseId(courseId: String, page: Int, perPage: Int): Pair<List<Review>, Int> = dbQuery {
        val uuid = UUID.fromString(courseId)
        val query = ReviewTable.selectAll().where { ReviewTable.courseId eq uuid }
        val total = query.count().toInt()
        val offset = ((page - 1) * perPage).toLong()
        val items = query
            .orderBy(ReviewTable.createdAt, SortOrder.DESC)
            .limit(perPage).offset(offset)
            .map { it.toReview() }
        Pair(items, total)
    }

    override suspend fun getByUserAndCourse(userId: String, courseId: String): Review? = dbQuery {
        ReviewTable.selectAll()
            .where {
                (ReviewTable.userId eq UUID.fromString(userId)) and
                (ReviewTable.courseId eq UUID.fromString(courseId))
            }
            .singleOrNull()
            ?.toReview()
    }

    override suspend fun create(review: Review): String = dbQuery {
        val now = Clock.System.now()
        ReviewTable.insert {
            it[id]        = UUID.fromString(review.id)
            it[userId]    = UUID.fromString(review.userId)
            it[courseId]  = UUID.fromString(review.courseId)
            it[rating]    = review.rating
            it[comment]   = review.comment
            it[createdAt] = now
            it[updatedAt] = now
        }
        review.id
    }

    override suspend fun update(review: Review): Boolean = dbQuery {
        val rows = ReviewTable.update({ ReviewTable.id eq UUID.fromString(review.id) }) {
            it[rating]    = review.rating
            it[comment]   = review.comment
            it[updatedAt] = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun delete(reviewId: String): Boolean = dbQuery {
        val rows = ReviewTable.deleteWhere { id eq UUID.fromString(reviewId) }
        rows > 0
    }

    override suspend fun hasReviewed(userId: String, courseId: String): Boolean = dbQuery {
        ReviewTable.selectAll()
            .where {
                (ReviewTable.userId eq UUID.fromString(userId)) and
                (ReviewTable.courseId eq UUID.fromString(courseId))
            }
            .count() > 0
    }
}
