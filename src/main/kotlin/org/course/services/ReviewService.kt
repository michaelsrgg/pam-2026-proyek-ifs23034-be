package org.course.services

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.course.data.AppException
import org.course.data.DataResponse
import org.course.data.PaginatedResponse
import org.course.data.PaginationMeta
import org.course.data.request.ReviewRequest
import org.course.helpers.*
import org.course.repositories.*

class ReviewService(
    private val userRepo: IUserRepository,
    private val courseRepo: ICourseRepository,
    private val enrollRepo: IEnrollmentRepository,
    private val reviewRepo: IReviewRepository,
) {
    // ── Public: Ambil semua review sebuah kursus ──────────────────────────────

    suspend fun getByCourse(call: ApplicationCall) {
        val courseId = call.requireParam("courseId")
        courseRepo.getById(courseId) ?: throw AppException(404, "Kursus tidak ditemukan.")

        val params = call.pageParams()
        val (reviews, total) = reviewRepo.getByCourseId(courseId, params.page, params.perPage)

        val responses = reviews.mapNotNull { review ->
            val reviewer = userRepo.getById(review.userId) ?: return@mapNotNull null
            review.toResponse(call, reviewer)
        }

        val totalPages = if (total == 0) 0 else (total + params.perPage - 1) / params.perPage
        val meta = PaginationMeta(
            currentPage  = params.page,
            perPage      = params.perPage,
            total        = total,
            totalPages   = totalPages,
            hasNextPage  = params.page < totalPages,
            hasPrevPage  = params.page > 1,
        )

        call.respond(PaginatedResponse("success", "Berhasil mengambil daftar review.",
            mapOf("reviews" to responses), meta))
    }

    // ── Student: Tulis review kursus ──────────────────────────────────────────

    suspend fun post(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")

        if (course.instructorId == user.id) {
            throw AppException(400, "Anda tidak dapat me-review kursus Anda sendiri.")
        }

        val enrollment = enrollRepo.getByUserAndCourse(user.id, courseId)
        if (enrollment == null || enrollment.status == "cancelled") {
            throw AppException(403, "Anda harus terdaftar di kursus ini untuk menulis review.")
        }

        if (reviewRepo.hasReviewed(user.id, courseId)) {
            throw AppException(409, "Anda sudah memberikan review untuk kursus ini. Gunakan endpoint PUT untuk memperbarui.")
        }

        val req = call.receive<ReviewRequest>()
        ValidatorHelper(req.toMap())
            .required("rating", "Rating tidak boleh kosong")
            .validate()

        if (req.rating !in 1..5) {
            throw AppException(400, "Rating harus berupa angka antara 1 sampai 5.")
        }

        val review = req.toEntity(user.id, courseId)
        val id     = reviewRepo.create(review)
        courseRepo.updateStats(courseId)

        call.respond(HttpStatusCode.Created,
            DataResponse("success", "Review berhasil dikirimkan. Terima kasih atas feedback Anda!",
                mapOf("reviewId" to id)))
    }

    // ── Student: Update review ────────────────────────────────────────────────

    suspend fun put(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")

        courseRepo.getById(courseId) ?: throw AppException(404, "Kursus tidak ditemukan.")

        val existing = reviewRepo.getByUserAndCourse(user.id, courseId)
            ?: throw AppException(404, "Anda belum memberikan review untuk kursus ini.")

        val req = call.receive<ReviewRequest>()
        if (req.rating !in 1..5) {
            throw AppException(400, "Rating harus berupa angka antara 1 sampai 5.")
        }

        val updated = existing.copy(
            rating  = req.rating,
            comment = req.comment?.trim(),
        )
        reviewRepo.update(updated)
        courseRepo.updateStats(courseId)

        call.respond(DataResponse<Unit>("success", "Review berhasil diperbarui."))
    }

    // ── Student: Hapus review sendiri ─────────────────────────────────────────

    suspend fun delete(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")

        courseRepo.getById(courseId) ?: throw AppException(404, "Kursus tidak ditemukan.")

        val existing = reviewRepo.getByUserAndCourse(user.id, courseId)
            ?: throw AppException(404, "Anda belum memberikan review untuk kursus ini.")

        reviewRepo.delete(existing.id)
        courseRepo.updateStats(courseId)

        call.respond(DataResponse<Unit>("success", "Review berhasil dihapus."))
    }

    // ── Student: Ambil review sendiri untuk sebuah kursus ────────────────────

    suspend fun getMyReview(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")

        val review = reviewRepo.getByUserAndCourse(user.id, courseId)
            ?: throw AppException(404, "Anda belum memberikan review untuk kursus ini.")

        val response = review.toResponse(call, user)
        call.respond(DataResponse("success", "Berhasil mengambil review Anda.", response))
    }
}
