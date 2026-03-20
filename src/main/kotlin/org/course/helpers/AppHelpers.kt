package org.course.helpers

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.course.data.AppException
import org.course.entities.User
import org.course.repositories.IUserRepository
import java.io.File
import java.text.Normalizer
import java.util.UUID

// ── Slug ─────────────────────────────────────────────────────────────────────

fun String.toSlug(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized
        .replace(Regex("[^\\p{ASCII}]"), "")
        .lowercase()
        .trim()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .take(200)
}

fun makeUniqueSlug(title: String, existingSlugChecker: (String) -> Boolean): String {
    val base = title.toSlug()
    if (!existingSlugChecker(base)) return base
    var candidate: String
    do {
        candidate = "$base-${UUID.randomUUID().toString().take(6)}"
    } while (existingSlugChecker(candidate))
    return candidate
}

// ── File ─────────────────────────────────────────────────────────────────────

private val ALLOWED_IMAGE_TYPES = setOf("jpg", "jpeg", "png", "webp")

suspend fun processImageUpload(
    call: ApplicationCall,
    folder: String,
    oldFilePath: String? = null,
): String? {
    var savedPath: String? = null

    val multipart = call.receiveMultipart()
    multipart.forEachPart { part ->
        if (part is PartData.FileItem) {
            val originalName = part.originalFileName ?: ""
            val ext = originalName.substringAfterLast('.', "").lowercase()

            if (ext !in ALLOWED_IMAGE_TYPES) {
                part.dispose()
                throw AppException(400, "Format file tidak didukung. Gunakan JPG, PNG, atau WebP.")
            }

            val fileName = "${UUID.randomUUID()}.$ext"
            val filePath = "uploads/$folder/$fileName"

            withContext(Dispatchers.IO) {
                val file = File(filePath)
                file.parentFile?.mkdirs()
                part.streamProvider().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                savedPath = filePath
            }
        }
        part.dispose()
    }

    if (savedPath != null && oldFilePath != null) {
        withContext(Dispatchers.IO) {
            val old = File(oldFilePath)
            if (old.exists()) old.delete()
        }
    }

    return savedPath
}

fun deleteFileQuietly(filePath: String?) {
    if (filePath.isNullOrBlank()) return
    runCatching { File(filePath).delete() }
}

// ── Auth ──────────────────────────────────────────────────────────────────────

suspend fun ApplicationCall.getAuthUser(userRepository: IUserRepository): User {
    val principal = principal<JWTPrincipal>()
        ?: throw AppException(401, "Token tidak valid atau sudah kedaluwarsa.")
    val userId = principal.payload.getClaim("userId").asString()
        ?: throw AppException(401, "Token tidak valid.")
    return userRepository.getById(userId)
        ?: throw AppException(401, "Akun tidak ditemukan atau sudah dihapus.")
}

fun User.requireRole(vararg roles: String) {
    if (this.role !in roles)
        throw AppException(403, "Anda tidak memiliki izin untuk mengakses fitur ini.")
}

fun ApplicationCall.requireParam(name: String): String =
    parameters[name]?.takeIf { it.isNotBlank() }
        ?: throw AppException(400, "Parameter '$name' tidak valid.")

data class PageParams(val page: Int, val perPage: Int) {
    val offset: Long get() = ((page - 1) * perPage).toLong()
}

fun ApplicationCall.pageParams(): PageParams {
    val page    = request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val perPage = request.queryParameters["perPage"]?.toIntOrNull()?.coerceIn(1, 100) ?: 10
    return PageParams(page, perPage)
}