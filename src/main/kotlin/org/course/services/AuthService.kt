package org.course.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.course.data.AppException
import org.course.data.DataResponse
import org.course.data.request.LoginRequest
import org.course.data.request.LogoutRequest
import org.course.data.request.RefreshTokenRequest
import org.course.data.request.RegisterRequest
import org.course.entities.RefreshToken
import org.course.helpers.*
import org.course.repositories.IRefreshTokenRepository
import org.course.repositories.IUserRepository
import java.util.*

class AuthService(
    private val jwtSecret: String,
    private val userRepo: IUserRepository,
    private val refreshTokenRepo: IRefreshTokenRepository,
) {
    companion object {
        private const val ACCESS_TOKEN_EXPIRY_MS  = 60L * 60 * 1000          // 1 jam
        private const val REFRESH_TOKEN_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000 // 30 hari
    }

    private fun generateAccessToken(userId: String): String =
        JWT.create()
            .withAudience(JWTConstants.AUDIENCE)
            .withIssuer(JWTConstants.ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
            .sign(Algorithm.HMAC256(jwtSecret))

    suspend fun postRegister(call: ApplicationCall) {
        val req = call.receive<RegisterRequest>()

        ValidatorHelper(req.toMap())
            .required("name", "Nama tidak boleh kosong")
            .minLength("name", 2, "Nama minimal 2 karakter")
            .maxLength("name", 150, "Nama maksimal 150 karakter")
            .required("username", "Username tidak boleh kosong")
            .minLength("username", 3, "Username minimal 3 karakter")
            .maxLength("username", 80, "Username maksimal 80 karakter")
            .required("email", "Email tidak boleh kosong")
            .email("email", "Format email tidak valid")
            .required("password", "Password tidak boleh kosong")
            .minLength("password", 8, "Password minimal 8 karakter")
            .validate()

        if (userRepo.getByUsername(req.username) != null) {
            throw AppException(409, "Username sudah digunakan. Silakan pilih username lain.")
        }
        if (userRepo.getByEmail(req.email) != null) {
            throw AppException(409, "Email sudah terdaftar. Silakan gunakan email lain atau login.")
        }

        val hashed = hashPassword(req.password)
        val user   = req.toEntity(hashed)
        val userId = userRepo.create(user)

        call.respond(
            HttpStatusCode.Created,
            DataResponse("success", "Registrasi berhasil! Silakan login.", mapOf("userId" to userId))
        )
    }

    suspend fun postLogin(call: ApplicationCall) {
        val req = call.receive<LoginRequest>()

        ValidatorHelper(req.toMap())
            .required("usernameOrEmail", "Username atau email tidak boleh kosong")
            .required("password", "Password tidak boleh kosong")
            .validate()

        val user = userRepo.getByUsernameOrEmail(req.usernameOrEmail)
            ?: throw AppException(401, "Username/email atau password salah.")

        if (!user.isActive) {
            throw AppException(403, "Akun Anda telah dinonaktifkan. Hubungi administrator.")
        }

        if (!verifyPassword(req.password, user.password)) {
            throw AppException(401, "Username/email atau password salah.")
        }

        // Hapus token lama milik user
        refreshTokenRepo.deleteByUserId(user.id)

        val authToken    = generateAccessToken(user.id)
        val refreshToken = UUID.randomUUID().toString()

        refreshTokenRepo.create(
            RefreshToken(userId = user.id, authToken = authToken, refreshToken = refreshToken)
        )

        call.respond(
            DataResponse(
                "success",
                "Login berhasil. Selamat datang, ${user.name}!",
                mapOf(
                    "authToken"    to authToken,
                    "refreshToken" to refreshToken,
                    "userId"       to user.id,
                    "role"         to user.role,
                )
            )
        )
    }

    suspend fun postRefreshToken(call: ApplicationCall) {
        val req = call.receive<RefreshTokenRequest>()

        ValidatorHelper(req.toMap())
            .required("authToken", "Auth token tidak boleh kosong")
            .required("refreshToken", "Refresh token tidak boleh kosong")
            .validate()

        val tokenPair = refreshTokenRepo.getByTokenPair(req.authToken, req.refreshToken)

        // Hapus token lama sebelum validasi (mencegah reuse attack)
        refreshTokenRepo.deleteByAuthToken(req.authToken)

        if (tokenPair == null) {
            throw AppException(401, "Token tidak valid atau sudah kedaluwarsa. Silakan login kembali.")
        }

        val user = userRepo.getById(tokenPair.userId)
            ?: throw AppException(401, "Akun tidak ditemukan.")

        if (!user.isActive) {
            throw AppException(403, "Akun Anda telah dinonaktifkan.")
        }

        val newAuthToken    = generateAccessToken(user.id)
        val newRefreshToken = UUID.randomUUID().toString()

        refreshTokenRepo.create(
            RefreshToken(userId = user.id, authToken = newAuthToken, refreshToken = newRefreshToken)
        )

        call.respond(
            DataResponse(
                "success",
                "Token berhasil diperbarui.",
                mapOf("authToken" to newAuthToken, "refreshToken" to newRefreshToken)
            )
        )
    }

    suspend fun postLogout(call: ApplicationCall) {
        val req = call.receive<LogoutRequest>()

        ValidatorHelper(req.toMap())
            .required("authToken", "Auth token tidak boleh kosong")
            .validate()

        // Decode token untuk mendapat userId (tidak perlu verify expiry saat logout)
        runCatching {
            val decoded = JWT.decode(req.authToken)
            val userId  = decoded.getClaim("userId").asString()
            if (!userId.isNullOrBlank()) {
                refreshTokenRepo.deleteByUserId(userId)
            }
        }
        refreshTokenRepo.deleteByAuthToken(req.authToken)

        call.respond(DataResponse<Unit>("success", "Logout berhasil."))
    }
}
