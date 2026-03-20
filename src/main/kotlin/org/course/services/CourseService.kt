package org.course.services

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking
import org.course.data.AppException
import org.course.data.DataResponse
import org.course.data.PaginatedResponse
import org.course.data.PaginationMeta
import org.course.data.request.CourseRequest
import org.course.data.response.InstructorDashboardResponse
import org.course.helpers.*
import org.course.repositories.*

class CourseService(
    private val userRepo: IUserRepository,
    private val courseRepo: ICourseRepository,
    private val categoryRepo: ICategoryRepository,
    private val enrollRepo: IEnrollmentRepository,
) {
    suspend fun getAll(call: ApplicationCall) {
        val params   = call.pageParams()
        val search   = call.request.queryParameters["search"] ?: ""
        val catId    = call.request.queryParameters["categoryId"]
        val level    = call.request.queryParameters["level"]
        val sortBy   = call.request.queryParameters["sortBy"] ?: "newest"
        val minPrice = call.request.queryParameters["minPrice"]?.toLongOrNull()
        val maxPrice = call.request.queryParameters["maxPrice"]?.toLongOrNull()

        val filter = CourseFilter(
            search     = search,
            categoryId = catId,
            level      = level,
            status     = "published",
            sortBy     = sortBy,
            minPrice   = minPrice,
            maxPrice   = maxPrice,
            page       = params.page,
            perPage    = params.perPage,
        )

        val (courses, total) = courseRepo.getAll(filter)
        val responses = courses.map { course ->
            val instructor = userRepo.getById(course.instructorId)
            val category   = course.categoryId?.let { categoryRepo.getById(it) }
            course.toListResponse(call, instructor?.name ?: "Unknown", category?.name)
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
        call.respond(PaginatedResponse("success", "Berhasil mengambil daftar kursus.",
            mapOf("courses" to responses), meta))
    }

    suspend fun getBySlug(call: ApplicationCall) {
        val slug   = call.requireParam("slug")
        val course = courseRepo.getBySlug(slug)
            ?: throw AppException(404, "Kursus tidak ditemukan.")

        if (course.status != "published") throw AppException(404, "Kursus tidak tersedia.")

        var isEnrolled = false
        runCatching {
            val user = call.getAuthUser(userRepo)
            isEnrolled = enrollRepo.isEnrolled(user.id, course.id)
        }

        val response = course.toDetailResponse(call, userRepo, categoryRepo, isEnrolled)
        call.respond(DataResponse("success", "Berhasil mengambil detail kursus.", response))
    }

    suspend fun getMyCourses(call: ApplicationCall) {
        val user   = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val params = call.pageParams()
        val search = call.request.queryParameters["search"] ?: ""
        val status = call.request.queryParameters["status"] ?: ""

        val filter = CourseFilter(
            search       = search,
            instructorId = user.id,
            status       = status,
            page         = params.page,
            perPage      = params.perPage,
        )

        val (courses, total) = courseRepo.getAll(filter)
        val responses = courses.map { course ->
            val category = course.categoryId?.let { categoryRepo.getById(it) }
            course.toListResponse(call, user.name, category?.name)
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
        call.respond(PaginatedResponse("success", "Berhasil mengambil kursus saya.",
            mapOf("courses" to responses), meta))
    }

    suspend fun getMyCoursById(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("id")
        val course   = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")

        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        val response = course.toDetailResponse(call, userRepo, categoryRepo)
        call.respond(DataResponse("success", "Berhasil mengambil detail kursus.", response))
    }

    suspend fun post(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)
        user.requireRole("instructor")

        val req = call.receive<CourseRequest>()
        ValidatorHelper(req.toMap())
            .required("title", "Judul kursus tidak boleh kosong")
            .minLength("title", 5, "Judul kursus minimal 5 karakter")
            .required("description", "Deskripsi kursus tidak boleh kosong")
            .minLength("description", 20, "Deskripsi minimal 20 karakter")
            .inList("level", listOf("beginner", "intermediate", "advanced"), "Level tidak valid")
            .positiveNumber("price", "Harga tidak boleh negatif")
            .validate()

        if (!req.categoryId.isNullOrBlank()) {
            categoryRepo.getById(req.categoryId)
                ?: throw AppException(404, "Kategori tidak ditemukan.")
        }

        val slug   = makeUniqueSlug(req.title) { runBlocking { courseRepo.slugExists(it) } }
        val course = req.toEntity(user.id, slug)
        val id     = courseRepo.create(course)

        call.respond(HttpStatusCode.Created,
            DataResponse("success", "Kursus berhasil dibuat sebagai draft.",
                mapOf("courseId" to id, "slug" to slug)))
    }

    suspend fun put(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("id")

        val existing = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (existing.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        val req = call.receive<CourseRequest>()
        ValidatorHelper(req.toMap())
            .required("title", "Judul kursus tidak boleh kosong")
            .minLength("title", 5, "Judul kursus minimal 5 karakter")
            .required("description", "Deskripsi tidak boleh kosong")
            .minLength("description", 20, "Deskripsi minimal 20 karakter")
            .inList("level", listOf("beginner", "intermediate", "advanced"), "Level tidak valid")
            .positiveNumber("price", "Harga tidak boleh negatif")
            .validate()

        if (!req.categoryId.isNullOrBlank()) {
            categoryRepo.getById(req.categoryId)
                ?: throw AppException(404, "Kategori tidak ditemukan.")
        }

        val slug = if (req.title.trim() != existing.title) {
            makeUniqueSlug(req.title) { newSlug ->
                newSlug != existing.slug && runBlocking { courseRepo.slugExists(newSlug) }
            }
        } else existing.slug

        val updated = req.toEntity(user.id, slug).copy(
            id        = existing.id,
            thumbnail = existing.thumbnail,
        )
        courseRepo.update(updated)
        call.respond(DataResponse<Unit>("success", "Kursus berhasil diperbarui."))
    }

    suspend fun putThumbnail(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("id")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        val savedPath = processImageUpload(call, "courses", course.thumbnail)
            ?: throw AppException(400, "Tidak ada file thumbnail yang diunggah.")

        courseRepo.updateThumbnail(courseId, savedPath)
        val thumbnailUrl = buildFileUrl(call, savedPath, "courses")
        call.respond(DataResponse("success", "Thumbnail berhasil diunggah.",
            mapOf("thumbnailUrl" to thumbnailUrl)))
    }

    suspend fun patchStatus(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("id")
        val status   = call.requireParam("status")

        if (status !in listOf("published", "draft", "archived")) {
            throw AppException(400, "Status tidak valid.")
        }

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        if (status == "published" && course.totalLessons == 0) {
            throw AppException(400, "Kursus harus memiliki minimal 1 lesson sebelum dipublish.")
        }

        courseRepo.update(course.copy(status = status))
        val msg = when (status) {
            "published" -> "Kursus berhasil dipublikasikan."
            "archived"  -> "Kursus berhasil diarsipkan."
            else        -> "Status kursus diubah ke draft."
        }
        call.respond(DataResponse<Unit>("success", msg))
    }

    suspend fun deleteCourse(call: ApplicationCall) {
        val user     = call.getAuthUser(userRepo)
        user.requireRole("instructor")
        val courseId = call.requireParam("id")

        val course = courseRepo.getById(courseId)
            ?: throw AppException(404, "Kursus tidak ditemukan.")
        if (course.instructorId != user.id) throw AppException(403, "Akses ditolak.")

        if (course.totalStudents > 0) {
            throw AppException(409, "Kursus tidak dapat dihapus karena sudah memiliki siswa.")
        }

        deleteFileQuietly(course.thumbnail)
        courseRepo.delete(courseId)
        call.respond(DataResponse<Unit>("success", "Kursus berhasil dihapus."))
    }

    suspend fun getInstructorDashboard(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)
        user.requireRole("instructor")

        val (allCourses, _) = courseRepo.getAll(
            CourseFilter(instructorId = user.id, status = "", perPage = 1000)
        )

        val totalStudents = allCourses.sumOf { it.totalStudents }
        val totalRevenue  = allCourses.sumOf { it.totalStudents * it.finalPrice() }
        val ratedCourses  = allCourses.filter { it.totalReviews > 0 }
        val avgRating     = if (ratedCourses.isEmpty()) 0.0
        else ratedCourses.sumOf { it.avgRating } / ratedCourses.size

        val (recentCourses, _) = courseRepo.getAll(
            CourseFilter(instructorId = user.id, status = "", page = 1, perPage = 5, sortBy = "newest")
        )
        val recentResponses = recentCourses.map { course ->
            val category = course.categoryId?.let { categoryRepo.getById(it) }
            course.toListResponse(call, user.name, category?.name)
        }

        val dashboard = InstructorDashboardResponse(
            totalCourses  = allCourses.size,
            totalStudents = totalStudents,
            totalRevenue  = totalRevenue,
            avgRating     = String.format("%.2f", avgRating).toDouble(),
            recentCourses = recentResponses,
        )
        call.respond(DataResponse("success", "Berhasil mengambil data dashboard.", dashboard))
    }

    suspend fun getThumbnail(call: ApplicationCall) {
        val fileId     = call.requireParam("id")
        val extensions = listOf("jpg", "jpeg", "png", "webp")
        val file = extensions.map { java.io.File("uploads/courses/$fileId.$it") }
            .firstOrNull { it.exists() }
            ?: java.io.File("uploads/courses/$fileId").takeIf { it.exists() }
            ?: return call.respond(HttpStatusCode.NotFound,
                DataResponse<Unit>("fail", "Thumbnail tidak ditemukan."))
        call.respondFile(file)
    }
}