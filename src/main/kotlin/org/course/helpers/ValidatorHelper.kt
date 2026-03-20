package org.course.helpers

import org.course.data.AppException

/**
 * Helper untuk validasi input request.
 * Mengumpulkan semua error validasi sebelum melempar exception.
 */
class ValidatorHelper(private val data: Map<String, Any?>) {
    private val errors = mutableMapOf<String, MutableList<String>>()

    fun required(field: String, message: String): ValidatorHelper {
        val value = data[field]
        if (value == null || value.toString().isBlank()) {
            errors.getOrPut(field) { mutableListOf() }.add(message)
        }
        return this
    }

    fun minLength(field: String, min: Int, message: String): ValidatorHelper {
        val value = data[field]?.toString() ?: ""
        if (value.isNotBlank() && value.length < min) {
            errors.getOrPut(field) { mutableListOf() }.add(message)
        }
        return this
    }

    fun maxLength(field: String, max: Int, message: String): ValidatorHelper {
        val value = data[field]?.toString() ?: ""
        if (value.isNotBlank() && value.length > max) {
            errors.getOrPut(field) { mutableListOf() }.add(message)
        }
        return this
    }

    fun email(field: String, message: String): ValidatorHelper {
        val value = data[field]?.toString() ?: ""
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (value.isNotBlank() && !emailRegex.matches(value)) {
            errors.getOrPut(field) { mutableListOf() }.add(message)
        }
        return this
    }

    fun matches(field: String, other: String, message: String): ValidatorHelper {
        val value = data[field]?.toString() ?: ""
        val otherValue = data[other]?.toString() ?: ""
        if (value.isNotBlank() && value != otherValue) {
            errors.getOrPut(field) { mutableListOf() }.add(message)
        }
        return this
    }

    fun inList(field: String, allowed: List<String>, message: String): ValidatorHelper {
        val value = data[field]?.toString() ?: ""
        if (value.isNotBlank() && value !in allowed) {
            errors.getOrPut(field) { mutableListOf() }.add(message)
        }
        return this
    }

    fun positiveNumber(field: String, message: String): ValidatorHelper {
        val value = data[field]?.toString()?.toLongOrNull()
        if (value != null && value < 0) {
            errors.getOrPut(field) { mutableListOf() }.add(message)
        }
        return this
    }

    fun validate() {
        if (errors.isNotEmpty()) {
            val messages = errors.entries.joinToString("; ") { (_, msgs) -> msgs.joinToString(", ") }
            throw AppException(422, messages)
        }
    }
}
