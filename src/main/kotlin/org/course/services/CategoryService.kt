package org.course.services

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.course.data.AppException
import org.course.data.DataResponse
import org.course.data.request.CategoryRequest
import org.course.data.response.CategoryResponse
import org.course.helpers.*
import org.course.repositories.ICategoryRepository
import org.course.repositories.IUserRepository

class CategoryService(
    private val userRepo: IUserRepository,
    private val categoryRepo: ICategoryRepository,
) {
    private suspend fun toResponse(call: ApplicationCall, cat: org.course.entities.Category): CategoryResponse {
        val total = categoryRepo.countCourses(cat.id)
        return CategoryResponse(
            id          = cat.id,
            name        = cat.name,
            description = cat.description,
            icon        = cat.icon,
            totalCourses = total,
            createdAt   = cat.createdAt,
        )
    }

    suspend fun getAll(call: ApplicationCall) {
        val categories = categoryRepo.getAll()
        val responses  = categories.map { toResponse(call, it) }
        call.respond(DataResponse("success", "Berhasil mengambil daftar kategori.", mapOf("categories" to responses)))
    }

    suspend fun getById(call: ApplicationCall) {
        val id       = call.requireParam("id")
        val category = categoryRepo.getById(id)
            ?: throw AppException(404, "Kategori tidak ditemukan.")
        call.respond(DataResponse("success", "Berhasil mengambil kategori.", toResponse(call, category)))
    }

    suspend fun post(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)
        user.requireRole("instructor", "admin")

        val req = call.receive<CategoryRequest>()
        ValidatorHelper(req.toMap())
            .required("name", "Nama kategori tidak boleh kosong")
            .maxLength("name", 100, "Nama kategori maksimal 100 karakter")
            .validate()

        if (categoryRepo.getByName(req.name) != null) {
            throw AppException(409, "Kategori dengan nama ini sudah ada.")
        }

        val id = categoryRepo.create(req.toEntity())
        call.respond(HttpStatusCode.Created,
            DataResponse("success", "Kategori berhasil dibuat.", mapOf("categoryId" to id)))
    }

    suspend fun put(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)
        user.requireRole("instructor", "admin")

        val id  = call.requireParam("id")
        val req = call.receive<CategoryRequest>()

        ValidatorHelper(req.toMap())
            .required("name", "Nama kategori tidak boleh kosong")
            .maxLength("name", 100, "Nama kategori maksimal 100 karakter")
            .validate()

        val existing = categoryRepo.getById(id)
            ?: throw AppException(404, "Kategori tidak ditemukan.")

        val nameConflict = categoryRepo.getByName(req.name)
        if (nameConflict != null && nameConflict.id != id) {
            throw AppException(409, "Kategori dengan nama ini sudah ada.")
        }

        val updated = existing.copy(
            name        = req.name.trim(),
            description = req.description?.trim(),
            icon        = req.icon?.trim(),
        )
        categoryRepo.update(updated)
        call.respond(DataResponse<Unit>("success", "Kategori berhasil diperbarui."))
    }

    suspend fun delete(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)
        user.requireRole("instructor", "admin")

        val id = call.requireParam("id")
        categoryRepo.getById(id) ?: throw AppException(404, "Kategori tidak ditemukan.")

        if (categoryRepo.countCourses(id) > 0) {
            throw AppException(409, "Kategori tidak dapat dihapus karena masih memiliki kursus.")
        }

        categoryRepo.delete(id)
        call.respond(DataResponse<Unit>("success", "Kategori berhasil dihapus."))
    }
}
