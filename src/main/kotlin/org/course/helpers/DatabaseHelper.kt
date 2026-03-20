package org.course.helpers

import io.ktor.server.application.*
import org.course.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val host     = environment.config.property("ktor.database.host").getString()
    val port     = environment.config.property("ktor.database.port").getString()
    val name     = environment.config.property("ktor.database.name").getString()
    val user     = environment.config.property("ktor.database.user").getString()
    val password = environment.config.property("ktor.database.password").getString()

    Database.connect(
        url      = "jdbc:postgresql://$host:$port/$name",
        driver   = "org.postgresql.Driver",
        user     = user,
        password = password,
    )

    // Auto-create tables if not exist (urutan penting karena foreign key)
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            UserTable,
            RefreshTokenTable,
            CategoryTable,
            CourseTable,
            LessonTable,
            EnrollmentTable,
            LessonProgressTable,
            ReviewTable,
        )
    }

    log.info("Database connected and tables verified.")
}
