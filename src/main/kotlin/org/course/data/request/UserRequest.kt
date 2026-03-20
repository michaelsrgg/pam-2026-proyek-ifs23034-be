package org.course.data.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val name: String = "",
    val bio: String? = null,
) {
    fun toMap() = mapOf("name" to name)
}

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
) {
    fun toMap() = mapOf(
        "currentPassword" to currentPassword,
        "newPassword" to newPassword,
        "confirmPassword" to confirmPassword,
    )
}
