package org.course

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.course.data.AppException
import org.course.data.ErrorResponse
import org.course.helpers.JWTConstants
import org.course.services.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val authService       : AuthService       by inject()
    val userService       : UserService       by inject()
    val categoryService   : CategoryService   by inject()
    val courseService     : CourseService     by inject()
    val lessonService     : LessonService     by inject()
    val enrollmentService : EnrollmentService by inject()
    val reviewService     : ReviewService     by inject()

    // ── Global Error Handler ──────────────────────────────────────────────────
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(
                status  = HttpStatusCode.fromValue(cause.code),
                message = ErrorResponse(
                    status  = if (cause.code in 400..499) "fail" else "error",
                    message = cause.message,
                )
            )
        }
        exception<kotlinx.serialization.SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("fail", "Format data tidak valid: ${cause.message}")
            )
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("error", "Terjadi kesalahan pada server. Silakan coba lagi.")
            )
        }
    }

    routing {

        // ── Health Check ──────────────────────────────────────────────────────
        get("/") {
            call.respond(mapOf(
                "status"  to "ok",
                "message" to "Course App API v1.0 berjalan dengan baik.",
                "docs"    to "/api-docs",
            ))
        }

        get("/health") {
            call.respond(mapOf("status" to "healthy", "timestamp" to System.currentTimeMillis()))
        }

        // ══════════════════════════════════════════════════════════════════════
        // AUTH  — /auth
        // ══════════════════════════════════════════════════════════════════════
        route("/auth") {
            post("/register")      { authService.postRegister(call) }
            post("/login")         { authService.postLogin(call) }
            post("/refresh-token") { authService.postRefreshToken(call) }
            post("/logout")        { authService.postLogout(call) }
        }

        // ══════════════════════════════════════════════════════════════════════
        // PUBLIC IMAGE SERVING  — /images
        // ══════════════════════════════════════════════════════════════════════
        route("/images") {
            get("/users/{id}")   { userService.getPhoto(call) }
            get("/courses/{id}") { courseService.getThumbnail(call) }
        }

        // ══════════════════════════════════════════════════════════════════════
        // PUBLIC CATEGORIES  — /categories
        // ══════════════════════════════════════════════════════════════════════
        route("/categories") {
            get         { categoryService.getAll(call) }
            get("/{id}") { categoryService.getById(call) }
        }

        // ══════════════════════════════════════════════════════════════════════
        // PUBLIC COURSES  — /courses
        // ══════════════════════════════════════════════════════════════════════
        route("/courses") {
            get              { courseService.getAll(call) }           // list + filter + pagination
            get("/{slug}")   { courseService.getBySlug(call) }        // detail by slug
        }

        // ══════════════════════════════════════════════════════════════════════
        // PUBLIC REVIEWS PER COURSE  — /courses/:courseId/reviews
        // ══════════════════════════════════════════════════════════════════════
        route("/courses/{courseId}/reviews") {
            get { reviewService.getByCourse(call) }
        }

        // ══════════════════════════════════════════════════════════════════════
        // PROTECTED ROUTES  — require valid JWT
        // ══════════════════════════════════════════════════════════════════════
        authenticate(JWTConstants.NAME) {

            // ── User Profile ─────────────────────────────────────────────────
            route("/users/me") {
                get        { userService.getMe(call) }
                put        { userService.putMe(call) }
                put("/password") { userService.putMyPassword(call) }
                put("/photo")    { userService.putMyPhoto(call) }
            }

            // ── Category Management (Instructor only) ────────────────────────
            route("/categories") {
                post       { categoryService.post(call) }
                put("/{id}")    { categoryService.put(call) }
                delete("/{id}") { categoryService.delete(call) }
            }

            // ── Instructor Course Management ─────────────────────────────────
            route("/instructor/courses") {
                get              { courseService.getMyCourses(call) }
                post             { courseService.post(call) }
                get("/{id}")     { courseService.getMyCoursById(call) }
                put("/{id}")     { courseService.put(call) }
                delete("/{id}")  { courseService.deleteCourse(call) }

                // Thumbnail upload
                put("/{id}/thumbnail") { courseService.putThumbnail(call) }

                // Publish / archive / draft
                patch("/{id}/status/{status}") { courseService.patchStatus(call) }

                // Dashboard statistik instructor
                get("/dashboard/stats") { courseService.getInstructorDashboard(call) }

                // Daftar siswa di kursus
                get("/{courseId}/students") { enrollmentService.getCourseStudents(call) }
            }

            // ── Lesson Management (Instructor) & Access (Student) ─────────────
            route("/courses/{courseId}/lessons") {
                get              { lessonService.getByCourse(call) }
                post             { lessonService.post(call) }
                get("/{id}")     { lessonService.getById(call) }
                put("/{id}")     { lessonService.put(call) }
                delete("/{id}")  { lessonService.delete(call) }
                put("/reorder")  { lessonService.reorder(call) }

                // Student: update progress lesson
                post("/{id}/progress") { lessonService.updateProgress(call) }
            }

            // ── Enrollment ────────────────────────────────────────────────────
            route("/enrollments") {
                get                          { enrollmentService.getMyEnrollments(call) }
                post("/{courseId}")          { enrollmentService.enroll(call) }
                get("/{courseId}")           { enrollmentService.getEnrollmentDetail(call) }
                delete("/{courseId}/cancel") { enrollmentService.cancel(call) }
            }

            // ── Reviews (Authenticated) ───────────────────────────────────────
            route("/courses/{courseId}/reviews") {
                post    { reviewService.post(call) }
                put     { reviewService.put(call) }
                delete  { reviewService.delete(call) }
                get("/me") { reviewService.getMyReview(call) }
            }
        }
    }
}
