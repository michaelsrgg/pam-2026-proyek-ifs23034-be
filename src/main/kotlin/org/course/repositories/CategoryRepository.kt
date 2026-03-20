package org.course.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.course.entities.Category
import org.course.tables.CategoryTable
import org.course.tables.CourseTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class CategoryRepository : ICategoryRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toCategory() = Category(
        id          = this[CategoryTable.id].value.toString(),
        name        = this[CategoryTable.name],
        description = this[CategoryTable.description],
        icon        = this[CategoryTable.icon],
        createdAt   = this[CategoryTable.createdAt],
        updatedAt   = this[CategoryTable.updatedAt],
    )

    override suspend fun getAll(): List<Category> = dbQuery {
        CategoryTable.selectAll()
            .orderBy(CategoryTable.name, SortOrder.ASC)
            .map { it.toCategory() }
    }

    override suspend fun getById(id: String): Category? = dbQuery {
        CategoryTable.selectAll()
            .where { CategoryTable.id eq UUID.fromString(id) }
            .singleOrNull()
            ?.toCategory()
    }

    override suspend fun getByName(name: String): Category? = dbQuery {
        CategoryTable.selectAll()
            .where { CategoryTable.name eq name.trim() }
            .singleOrNull()
            ?.toCategory()
    }

    override suspend fun create(category: Category): String = dbQuery {
        val now = Clock.System.now()
        CategoryTable.insert {
            it[id]          = UUID.fromString(category.id)
            it[name]        = category.name
            it[description] = category.description
            it[icon]        = category.icon
            it[createdAt]   = now
            it[updatedAt]   = now
        }
        category.id
    }

    override suspend fun update(category: Category): Boolean = dbQuery {
        val rows = CategoryTable.update({ CategoryTable.id eq UUID.fromString(category.id) }) {
            it[name]        = category.name
            it[description] = category.description
            it[icon]        = category.icon
            it[updatedAt]   = Clock.System.now()
        }
        rows > 0
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        val rows = CategoryTable.deleteWhere { CategoryTable.id eq UUID.fromString(id) }
        rows > 0
    }

    override suspend fun countCourses(categoryId: String): Int = dbQuery {
        CourseTable.selectAll()
            .where { CourseTable.categoryId eq UUID.fromString(categoryId) }
            .count()
            .toInt()
    }
}
