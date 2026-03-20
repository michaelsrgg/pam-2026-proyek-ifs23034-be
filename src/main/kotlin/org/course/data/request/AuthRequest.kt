package org.course.data.request

import kotlinx.serialization.Serializable
import org.course.entities.User

@Serializable
data class RegisterRequest(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "student",
) {
    fun toMap() = mapOf(
        "name" to name,
        "username" to username,
        "email" to email,
        "password" to password,
    )

    fun toEntity(hashedPassword: String) = User(
        name = name.trim(),
        username = username.trim().lowercase(),
        email = email.trim().lowercase(),
        password = hashedPassword,
        role = if (role == "instructor") "instructor" else "student",
    )
}

@Serializable
data class LoginRequest(
    val usernameOrEmail: String = "",
    val password: String = "",
) {
    fun toMap() = mapOf(
        "usernameOrEmail" to usernameOrEmail,
        "password" to password,
    )
}

@Serializable
data class RefreshTokenRequest(
    val authToken: String = "",
    val refreshToken: String = "",
) {
    fun toMap() = mapOf(
        "authToken" to authToken,
        "refreshToken" to refreshToken,
    )
}

@Serializable
data class LogoutRequest(
    val authToken: String = "",
) {
    fun toMap() = mapOf("authToken" to authToken)
}
