package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.Course
import org.course.tables.CourseTable
import org.course.tables.LessonTable
import org.course.tables.ReviewTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class CourseRepository : ICourseRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toCourse() = Course(
        id            = this[CourseTable.id].value.toString(),
        instructorId  = this[CourseTable.instructorId].toString(),
        categoryId    = this[CourseTable.categoryId]?.toString(),
        title         = this[CourseTable.title],
        slug          = this[CourseTable.slug],
        description   = this[CourseTable.description],
        thumbnail     = this[CourseTable.thumbnail],
        level         = this[CourseTable.level],
        language      = this[CourseTable.language],
        price         = this[CourseTable.price],
        discountPrice = this[CourseTable.discountPrice],
        status        = this[CourseTable.status],
        totalDuration = this[CourseTable.totalDuration],
        totalLessons  = this[CourseTable.totalLessons],
        totalStudents = this[CourseTable.totalStudents],
        avgRating     = this[CourseTable.avgRating],
        totalReviews  = this[CourseTable.totalReviews],
        createdAt     = this[CourseTable.createdAt],
        updatedAt     = this[CourseTable.updatedAt],
    )

    override suspend fun getAll(filter: CourseFilter): Pair<List<Course>, Int> = dbQuery {
        var query = CourseTable.selectAll()

        if (filter.status.isNotBlank()) {
            query = query.where { CourseTable.status eq filter.status }
        }
        filter.instructorId?.let { instrId ->
            query = query.andWhere { CourseTable.instructorId eq UUID.fromString(instrId) }
        }
        filter.categoryId?.let { catId ->
            query = query.andWhere { CourseTable.categoryId eq UUID.fromString(catId) }
        }
        filter.level?.let { lvl ->
            if (lvl.isNotBlank()) query = query.andWhere { CourseTable.level eq lvl }
        }
        if (filter.search.isNotBlank()) {
            query = query.andWhere {
                CourseTable.title.lowerCase() like "%${filter.search.lowercase()}%"
            }
        }
        filter.minPrice?.let { min ->
            query = query.andWhere { CourseTable.price greaterEq min }
        }
        filter.maxPrice?.let { max ->
            query = query.andWhere { CourseTable.price lessEq max }
        }

        val total = query.count().toInt()

        query = when (filter.sortBy) {
            "popular"    -> query.orderBy(CourseTable.totalStudents, SortOrder.DESC)
            "rating"     -> query.orderBy(CourseTable.avgRating, SortOrder.DESC)
            "price_asc"  -> query.orderBy(CourseTable.price, SortOrder.ASC)
            "price_desc" -> query.orderBy(CourseTable.price, SortOrder.DESC)
            else         -> query.orderBy(CourseTable.createdAt, SortOrder.DESC)
        }

        val offset = ((filter.page - 1) * filter.perPage).toLong()
        val courses = query.limit(filter.perPage).offset(offset).map { it.toCourse() }
        Pair(courses, total)
    }

    override suspend fun getById(id: String): Course? = dbQuery {
        CourseTable.selectAll()
            .where { CourseTable.id eq UUID.fromString(id) }
            .singleOrNull()?.toCourse()
    }

    override suspend fun getBySlug(slug: String): Course? = dbQuery {
        CourseTable.selectAll()
            .where { CourseTable.slug eq slug }
            .singleOrNull()?.toCourse()
    }

    override suspend fun slugExists(slug: String): Boolean = dbQuery {
        CourseTable.selectAll()
            .where { CourseTable.slug eq slug }
            .count() > 0
    }

    override suspend fun create(course: Course): String = dbQuery {
        val now = Clock.System.now()
        CourseTable.insert {
            it[id]            = UUID.fromString(course.id)
            it[instructorId]  = UUID.fromString(course.instructorId)
            it[categoryId]    = course.categoryId?.let { cid -> UUID.fromString(cid) }
            it[title]         = course.title
            it[slug]          = course.slug
            it[description]   = course.description
            it[thumbnail]     = course.thumbnail
            it[level]         = course.level
            it[language]      = course.language
            it[price]         = course.price
            it[discountPrice] = course.discountPrice
            it[status]        = course.status
            it[createdAt]     = now
            it[updatedAt]     = now
        }
        course.id
    }

    override suspend fun update(course: Course): Boolean = dbQuery {
        val rows = CourseTable.update({ CourseTable.id eq UUID.fromString(course.id) }) {
            it[categoryId]    = course.categoryId?.let { cid -> UUID.fromString(cid) }
            it[title]         = course.title
            it[slug]          = course.slug
            it[description]   = course.description
            it[level]         = course.level
            it[language]      = course.language
            it[price]         = course.price
            it[discountPrice] = course.discountPrice
            it[status]        = course.status
            it[updatedAt]     = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun updateThumbnail(courseId: String, thumbnail: String?): Boolean = dbQuery {
        val rows = CourseTable.update({ CourseTable.id eq UUID.fromString(courseId) }) {
            it[CourseTable.thumbnail] = thumbnail
            it[updatedAt]             = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun incrementStudents(courseId: String): Boolean = dbQuery {
        val uuid = UUID.fromString(courseId)
        val current = CourseTable.selectAll()
            .where { CourseTable.id eq uuid }
            .singleOrNull()?.get(CourseTable.totalStudents) ?: 0
        val rows = CourseTable.update({ CourseTable.id eq uuid }) {
            it[totalStudents] = current + 1
            it[updatedAt]     = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun decrementStudents(courseId: String): Boolean = dbQuery {
        val uuid = UUID.fromString(courseId)
        val current = CourseTable.selectAll()
            .where { CourseTable.id eq uuid }
            .singleOrNull()?.get(CourseTable.totalStudents) ?: 0
        val rows = CourseTable.update({ CourseTable.id eq uuid }) {
            it[totalStudents] = if (current > 0) current - 1 else 0
            it[updatedAt]     = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun updateStats(courseId: String): Boolean = dbQuery {
        val uuid = UUID.fromString(courseId)
        val reviews = ReviewTable.selectAll()
            .where { ReviewTable.courseId eq uuid }
            .toList()
        val avg = if (reviews.isEmpty()) 0.0
        else reviews.sumOf { it[ReviewTable.rating] }.toDouble() / reviews.size

        val rows = CourseTable.update({ CourseTable.id eq uuid }) {
            it[avgRating]    = avg
            it[totalReviews] = reviews.size
            it[updatedAt]    = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun updateLessonCount(courseId: String): Boolean = dbQuery {
        val uuid = UUID.fromString(courseId)
        val lessons = LessonTable.selectAll()
            .where { (LessonTable.courseId eq uuid) and (LessonTable.isPublished eq true) }
            .toList()
        val totalDur = lessons.sumOf { it[LessonTable.duration] }

        val rows = CourseTable.update({ CourseTable.id eq uuid }) {
            it[totalLessons]  = lessons.size
            it[totalDuration] = totalDur
            it[updatedAt]     = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun delete(courseId: String): Boolean = dbQuery {
        val rows = CourseTable.deleteWhere { id eq UUID.fromString(courseId) }
        rows > 0
    }
}