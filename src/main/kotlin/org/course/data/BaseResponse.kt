package org.course.data

import kotlinx.serialization.Serializable

/**
 * Exception kustom untuk error yang bisa dikontrol (HTTP error).
 */
class AppException(val code: Int, override val message: String) : Exception(message)

/**
 * Response standar untuk semua endpoint sukses.
 */
@Serializable
data class DataResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null,
)

/**
 * Response standar untuk semua error.
 */
@Serializable
data class ErrorResponse(
    val status: String,
    val message: String,
    val data: String? = null,
)

/**
 * Response dengan pagination.
 */
@Serializable
data class PaginatedResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null,
    val meta: PaginationMeta? = null,
)

@Serializable
data class PaginationMeta(
    val currentPage: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean,
)
