package org.course.services

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.course.data.AppException
import org.course.data.DataResponse
import org.course.data.request.LessonProgressRequest
import org.course.data.request.LessonRequest
import org.course.helpers.*
import org.course.repositories.*

class LessonService(
    private val userRepo: IUserRepository,
    private val courseRepo: ICourseRepository,
    private val lessonRepo: ILessonRepository,
    private val enrollRepo: IEnrollmentRepository,
    private val progressRepo: ILessonProgressRepository,
) {
    // ── Student / Public: Ambil semua lesson dalam kursus ────────────────────

    suspend fun getByCourse(call: ApplicationCall) {
        val courseId = call.requireParam("courseId")
        val course   = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")

        // Cek apakah user login dan terdaftar di kursus
        var isEnrolled = false
        var userId     = ""
        runCatching {
            val user  = call.getAuthUser(userRepo)
            userId    = user.id
            isEnrolled = enrollRepo.isEnrolled(user.id, courseId) ||
                         course.instructorId == user.id
        }

        val publishedOnly = !isEnrolled
        val lessons = lessonRepo.getByCourseId(courseId, publishedOnly = publishedOnly)

        // Ambil progress jika user terdaftar
        val progressMap = if (isEnrolled && userId.isNotBlank()) {
            progressRepo.getByUserAndCourse(userId, courseId)
                .associate { it.lessonId to it }
        } else emptyMap()

        val responses = lessons.map { lesson ->
            val prog = progressMap[lesson.id]
            // Sembunyikan konten jika lesson berbayar dan user belum enroll
            if (!isEnrolled && !lesson.isFree) {
                lesson.copy(contentUrl = null, content = null)
                    .toListResponse(false)
            } else {
                lesson.toListResponse(prog?.isCompleted ?: false)
            }
        }

        call.respond(DataResponse("success", "Berhasil mengambil daftar lesson.", mapOf("lessons" to responses)))
    }

    // ── Student: Detail satu lesson ───────────────────────────────────────────

    suspend fun getById(call: ApplicationCall) {
        val courseId = call.requireParam("courseId")
        val lessonId = call.requireParam("id")

        val lesson = lessonRepo.getById(lessonId)
            ?: throw AppException(404, "Lesson tidak ditemukan.")
        if (lesson.courseId != courseId) throw AppException(404, "Lesson tidak ditemukan dalam kursus ini.")

        // Cek akses: lesson gratis/preview atau sudah enroll
        var isEnrolled  = false
        var userId      = ""
        var isInstructor = false

        runCatching {
            val user = call.getAuthUser(userRepo)
            userId   = user.id
            val course = courseRepo.getById(courseId)
            isInstructor = course?.instructorId == user.id
            isEnrolled   = isInstructor || enrollRepo.isEnrolled(user.id, courseId)
        }

        if (!isEnrolled && !lesson.isFree) {
            throw AppException(403, "Anda harus terdaftar di kursus ini untuk mengakses lesson ini.")
        }

        val prog = if (userId.isNotBlank()) {
            progressRepo.getByUserAndLesson(userId, lessonId)
        } else null

        val response = lesson.toResponse(
            isCompleted    = prog?.isCompleted ?: false,
            watchedSeconds = prog?.watchedSeconds ?: 0,
        )
        call.respond(DataResponse("success", "Berhasil mengambil detail lesson.", response))
    }

    // ── Instructor: Buat lesson baru ──────────────────────────────────────────

    suspend fun post(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("courseId")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        val req = call.receive<LessonRequest>()
        ValidatorHelper(req.toMap())
            .required("title", "Judul lesson tidak boleh kosong")
            .minLength("title", 3, "Judul lesson minimal 3 karakter")
            .maxLength("title", 200, "Judul lesson maksimal 200 karakter")
            .inList("contentType", listOf("video", "text", "quiz"), "Tipe konten tidak valid")
            .validate()

        // Auto-assign order index jika tidak diberikan
        val maxOrder = lessonRepo.getMaxOrderIndex(courseId)
        val lesson   = req.toEntity(courseId).copy(
            orderIndex = if (req.orderIndex > 0) req.orderIndex else maxOrder + 1
        )

        val id = lessonRepo.create(lesson)
        courseRepo.updateLessonCount(courseId)

        call.respond(HttpStatusCode.Created,
            DataResponse("success", "Lesson berhasil dibuat.", mapOf("lessonId" to id)))
    }

    // ── Instructor: Update lesson ─────────────────────────────────────────────

    suspend fun put(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("courseId")
        val lessonId = call.requireParam("id")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        val existing = lessonRepo.getById(lessonId)
            ?: throw AppException(404, "Lesson tidak ditemukan.")
        if (existing.courseId != courseId) throw AppException(404, "Lesson tidak ditemukan dalam kursus ini.")

        val req = call.receive<LessonRequest>()
        ValidatorHelper(req.toMap())
            .required("title", "Judul lesson tidak boleh kosong")
            .minLength("title", 3, "Judul lesson minimal 3 karakter")
            .inList("contentType", listOf("video", "text", "quiz"), "Tipe konten tidak valid")
            .validate()

        val updated = req.toEntity(courseId).copy(id = lessonId)
        lessonRepo.update(updated)
        courseRepo.updateLessonCount(courseId)

        call.respond(DataResponse<Unit>("success", "Lesson berhasil diperbarui."))
    }

    // ── Instructor: Hapus lesson ──────────────────────────────────────────────

    suspend fun delete(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("courseId")
        val lessonId = call.requireParam("id")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        val lesson = lessonRepo.getById(lessonId)
            ?: throw AppException(404, "Lesson tidak ditemukan.")
        if (lesson.courseId != courseId) throw AppException(404, "Lesson tidak ditemukan dalam kursus ini.")

        lessonRepo.delete(lessonId)
        courseRepo.updateLessonCount(courseId)

        call.respond(DataResponse<Unit>("success", "Lesson berhasil dihapus."))
    }

    // ── Instructor: Ubah urutan lesson ────────────────────────────────────────

    suspend fun reorder(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("courseId")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        @kotlinx.serialization.Serializable
        data class ReorderBody(val lessonIds: List<String> = emptyList())

        val body = call.receive<ReorderBody>()
        if (body.lessonIds.isEmpty()) throw AppException(400, "Daftar lesson tidak boleh kosong.")

        lessonRepo.reorder(courseId, body.lessonIds)

        call.respond(DataResponse<Unit>("success", "Urutan lesson berhasil diperbarui."))
    }

    // ── Student: Update progress lesson ──────────────────────────────────────

    suspend fun updateProgress(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        val courseId = call.requireParam("courseId")
        val lessonId = call.requireParam("id")

        val lesson = lessonRepo.getById(lessonId)
            ?: throw AppException(404, "Lesson tidak ditemukan.")
        if (lesson.courseId != courseId) throw AppException(404, "Lesson tidak valid.")

        if (!enrollRepo.isEnrolled(user.id, courseId)) {
            throw AppException(403, "Anda belum terdaftar di kursus ini.")
        }

        val req = call.receive<LessonProgressRequest>()
        ValidatorHelper(req.toMap())
            .positiveNumber("watchedSeconds", "Durasi menonton tidak valid")
            .validate()

        val completedAt = if (req.isCompleted) kotlinx.datetime.Clock.System.now() else null
        val progress    = org.course.entities.LessonProgress(
            userId         = user.id,
            lessonId       = lessonId,
            courseId       = courseId,
            isCompleted    = req.isCompleted,
            watchedSeconds = req.watchedSeconds,
            completedAt    = completedAt,
        )
        progressRepo.upsert(progress)

        // Hitung ulang progress kursus keseluruhan
        val totalLessons    = lessonRepo.getByCourseId(courseId, publishedOnly = true).size
        val completedCount  = progressRepo.countCompleted(user.id, courseId)
        val progressPercent = if (totalLessons == 0) 0
                              else ((completedCount.toDouble() / totalLessons) * 100).toInt()

        enrollRepo.updateProgress(user.id, courseId, progressPercent)

        // Auto-complete enrollment jika sudah 100%
        if (progressPercent >= 100) {
            enrollRepo.complete(user.id, courseId)
        }

        call.respond(DataResponse("success", "Progress lesson berhasil diperbarui.",
            mapOf("progress" to progressPercent, "completedLessons" to completedCount, "totalLessons" to totalLessons)))
    }
}
