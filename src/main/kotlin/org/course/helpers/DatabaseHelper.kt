package org.course.helpers

import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import org.course.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val dotenv = dotenv {
        directory       = "."
        ignoreIfMissing = true
        systemProperties = true
    }

    val host     = dotenv["DB_HOST"]     ?: System.getenv("DB_HOST")     ?: "localhost"
    val port     = dotenv["DB_PORT"]     ?: System.getenv("DB_PORT")     ?: "5432"
    val name     = dotenv["DB_NAME"]     ?: System.getenv("DB_NAME")     ?: "course_db"
    val user     = dotenv["DB_USER"]     ?: System.getenv("DB_USER")     ?: "postgres"
    val password = dotenv["DB_PASSWORD"] ?: System.getenv("DB_PASSWORD") ?: "postgres"

    log.info("Connecting to DB: jdbc:postgresql://$host:$port/$name as $user")

    Database.connect(
        url      = "jdbc:postgresql://$host:$port/$name",
        driver   = "org.postgresql.Driver",
        user     = user,
        password = password,
    )

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