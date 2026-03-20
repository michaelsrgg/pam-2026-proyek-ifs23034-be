package org.course.services

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.course.data.AppException
import org.course.data.DataResponse
import org.course.data.PaginatedResponse
import org.course.data.PaginationMeta
import org.course.data.response.EnrollmentResponse
import org.course.helpers.*
import org.course.repositories.*

class EnrollmentService(
    private val userRepo: IUserRepository,
    private val courseRepo: ICourseRepository,
    private val categoryRepo: ICategoryRepository,
    private val enrollRepo: IEnrollmentRepository,
    private val lessonRepo: ILessonRepository,
    private val progressRepo: ILessonProgressRepository,
) {
    suspend fun enroll(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")

        if (course.status != "published") {
            throw AppException(400, "Kursus ini belum tersedia untuk pendaftaran.")
        }
        if (course.instructorId == user.id) {
            throw AppException(400, "Anda tidak dapat mendaftar ke kursus yang Anda buat sendiri.")
        }

        val existing = enrollRepo.getByUserAndCourse(user.id, courseId)
        if (existing != null) {
            when (existing.status) {
                "active"    -> throw AppException(409, "Anda sudah terdaftar di kursus ini.")
                "completed" -> throw AppException(409, "Anda sudah menyelesaikan kursus ini.")
            }
        }

        val enrollment = org.course.entities.Enrollment(
            userId     = user.id,
            courseId   = courseId,
            paidAmount = course.finalPrice(),
        )
        val id = enrollRepo.create(enrollment)
        courseRepo.incrementStudents(courseId)

        call.respond(HttpStatusCode.Created,
            DataResponse("success", "Berhasil mendaftar ke kursus \"${course.title}\".",
                mapOf("enrollmentId" to id)))
    }

    suspend fun getMyEnrollments(call: ApplicationCall) {
        val user   = call.getAuthUser(userRepo)
        val params = call.pageParams()
        val status = call.request.queryParameters["status"]

        val (enrollments, total) = enrollRepo.getByUserId(
            userId  = user.id,
            status  = status,
            page    = params.page,
            perPage = params.perPage,
        )

        val responses = enrollments.mapNotNull { enrollment ->
            val course     = courseRepo.getById(enrollment.courseId) ?: return@mapNotNull null
            val instructor = userRepo.getById(course.instructorId)
            val category   = course.categoryId?.let { categoryRepo.getById(it) }

            EnrollmentResponse(
                id          = enrollment.id,
                course      = course.toListResponse(call, instructor?.name ?: "Unknown", category?.name),
                status      = enrollment.status,
                progress    = enrollment.progress,
                paidAmount  = enrollment.paidAmount,
                enrolledAt  = enrollment.enrolledAt,
                completedAt = enrollment.completedAt,
            )
        }

        val totalPages = if (total == 0) 0 else (total + params.perPage - 1) / params.perPage
        val meta = PaginationMeta(
            currentPage = params.page,
            perPage     = params.perPage,
            total       = total,
            totalPages  = totalPages,
            hasNextPage = params.page < totalPages,
            hasPrevPage = params.page > 1,
        )
        call.respond(PaginatedResponse("success", "Berhasil mengambil daftar kursus saya.",
            mapOf("enrollments" to responses), meta))
    }

    suspend fun getEnrollmentDetail(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")

        val enrollment = enrollRepo.getByUserAndCourse(user.id, courseId)
            ?: throw AppException(404, "Anda belum terdaftar di kursus ini.")

        if (enrollment.status == "cancelled") {
            throw AppException(404, "Enrollment Anda untuk kursus ini telah dibatalkan.")
        }

        val lessons      = lessonRepo.getByCourseId(courseId, publishedOnly = true)
        val progressList = progressRepo.getByUserAndCourse(user.id, courseId)
        val progressMap  = progressList.associate { it.lessonId to it }
        val completedCount = progressList.count { it.isCompleted }
        val totalLessons   = lessons.size
        val progressPct    = if (totalLessons == 0) 0
        else ((completedCount.toDouble() / totalLessons) * 100).toInt()

        val lessonResponses = lessons.map { lesson ->
            val prog = progressMap[lesson.id]
            lesson.toListResponse(prog?.isCompleted ?: false)
        }

        call.respond(DataResponse("success", "Berhasil mengambil detail enrollment.",
            mapOf(
                "enrollmentId"   to enrollment.id,
                "status"         to enrollment.status,
                "progress"       to progressPct,
                "completedCount" to completedCount,
                "totalLessons"   to totalLessons,
                "enrolledAt"     to enrollment.enrolledAt.toString(),
                "completedAt"    to enrollment.completedAt?.toString(),
                "lessons"        to lessonResponses,
            )
        ))
    }

    suspend fun cancel(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")

        val enrollment = enrollRepo.getByUserAndCourse(user.id, courseId)
            ?: throw AppException(404, "Anda belum terdaftar di kursus ini.")

        if (enrollment.status == "completed") {
            throw AppException(400, "Kursus yang sudah selesai tidak dapat dibatalkan.")
        }
        if (enrollment.status == "cancelled") {
            throw AppException(400, "Enrollment Anda sudah dibatalkan sebelumnya.")
        }

        enrollRepo.cancel(user.id, courseId)
        courseRepo.decrementStudents(courseId)
        call.respond(DataResponse<Unit>("success", "Pendaftaran kursus berhasil dibatalkan."))
    }

    suspend fun getCourseStudents(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("courseId")
        val params   = call.pageParams()

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        val (enrollments, total) = enrollRepo.getByCourseId(courseId, params.page, params.perPage)

        val responses = enrollments.mapNotNull { enrollment ->
            val student = userRepo.getById(enrollment.userId) ?: return@mapNotNull null
            mapOf(
                "userId"     to student.id,
                "name"       to student.name,
                "username"   to student.username,
                "progress"   to enrollment.progress,
                "status"     to enrollment.status,
                "enrolledAt" to enrollment.enrolledAt.toString(),
            )
        }

        val totalPages = if (total == 0) 0 else (total + params.perPage - 1) / params.perPage
        val meta = PaginationMeta(
            currentPage = params.page,
            perPage     = params.perPage,
            total       = total,
            totalPages  = totalPages,
            hasNextPage = params.page < totalPages,
            hasPrevPage = params.page > 1,
        )
        call.respond(PaginatedResponse("success", "Berhasil mengambil daftar siswa.",
            mapOf("students" to responses), meta))
    }
}