package org.course.services

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.course.data.AppException
import org.course.data.DataResponse
import org.course.data.request.ChangePasswordRequest
import org.course.data.request.UpdateProfileRequest
import org.course.data.response.UserResponse
import org.course.helpers.*
import org.course.repositories.IUserRepository
import java.io.File

class UserService(private val userRepo: IUserRepository) {

    private fun buildPhotoUrl(call: ApplicationCall, photo: String?): String? {
        if (photo.isNullOrBlank()) return null
        val host = "${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}"
        return "$host/images/users/${photo.substringAfterLast('/')}"
    }

    suspend fun getMe(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)

        val response = UserResponse(
            id        = user.id,
            name      = user.name,
            username  = user.username,
            email     = user.email,
            role      = user.role,
            bio       = user.bio,
            photoUrl  = buildPhotoUrl(call, user.photo),
            isActive  = user.isActive,
            createdAt = user.createdAt,
        )
        call.respond(DataResponse("success", "Data profil berhasil diambil.", response))
    }

    suspend fun putMe(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)
        val req  = call.receive<UpdateProfileRequest>()

        ValidatorHelper(req.toMap())
            .required("name", "Nama tidak boleh kosong")
            .minLength("name", 2, "Nama minimal 2 karakter")
            .maxLength("name", 150, "Nama maksimal 150 karakter")
            .validate()

        val updated = user.copy(name = req.name.trim(), bio = req.bio?.trim())
        val success = userRepo.update(updated)

        if (!success) throw AppException(500, "Gagal memperbarui profil. Coba lagi.")

        call.respond(DataResponse<Unit>("success", "Profil berhasil diperbarui."))
    }

    suspend fun putMyPassword(call: ApplicationCall) {
        val user = call.getAuthUser(userRepo)
        val req  = call.receive<ChangePasswordRequest>()

        ValidatorHelper(req.toMap())
            .required("currentPassword", "Password saat ini tidak boleh kosong")
            .required("newPassword", "Password baru tidak boleh kosong")
            .minLength("newPassword", 8, "Password baru minimal 8 karakter")
            .required("confirmPassword", "Konfirmasi password tidak boleh kosong")
            .matches("confirmPassword", "newPassword", "Konfirmasi password tidak sesuai")
            .validate()

        if (!verifyPassword(req.currentPassword, user.password)) {
            throw AppException(400, "Password saat ini tidak sesuai.")
        }

        if (req.currentPassword == req.newPassword) {
            throw AppException(400, "Password baru tidak boleh sama dengan password lama.")
        }

        val updated = user.copy(password = hashPassword(req.newPassword))
        userRepo.update(updated)

        call.respond(DataResponse<Unit>("success", "Password berhasil diubah."))
    }

    suspend fun putMyPhoto(call: ApplicationCall) {
        val user      = call.getAuthUser(userRepo)
        val savedPath = processImageUpload(call, "users", user.photo)
            ?: throw AppException(400, "Tidak ada file foto yang diunggah.")

        userRepo.updatePhoto(user.id, savedPath)

        val photoUrl = buildPhotoUrl(call, savedPath)
        call.respond(DataResponse("success", "Foto profil berhasil diperbarui.", mapOf("photoUrl" to photoUrl)))
    }

    suspend fun getPhoto(call: ApplicationCall) {
        val fileId = call.requireParam("id")

        val extensions = listOf("jpg", "jpeg", "png", "webp")
        val file = extensions.map { File("uploads/users/$fileId.$it") }.firstOrNull { it.exists() }
            ?: File("uploads/users/$fileId").takeIf { it.exists() }
            ?: return call.respond(HttpStatusCode.NotFound, DataResponse<Unit>("fail", "Foto tidak ditemukan."))

        call.respondFile(file)
    }
}
