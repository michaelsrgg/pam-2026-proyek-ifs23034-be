package org.course.helpers

import io.ktor.server.application.*
import org.course.data.response.*
import org.course.entities.*
import org.course.repositories.ICategoryRepository
import org.course.repositories.IUserRepository

/**
 * Membangun URL lengkap untuk file yang disimpan di server.
 */
fun buildFileUrl(call: ApplicationCall, filePath: String?, folder: String): String? {
    if (filePath.isNullOrBlank()) return null
    val fileName = filePath.substringAfterLast('/')
    val scheme   = call.request.local.scheme
    val host     = call.request.local.serverHost
    val port     = call.request.local.serverPort
    return "$scheme://$host:$port/images/$folder/$fileName"
}

/**
 * Menghitung harga final kursus (setelah diskon jika ada).
 */
fun Course.finalPrice(): Long = discountPrice ?: price

/**
 * Mengubah User entity menjadi PublicUserResponse.
 */
fun User.toPublicResponse(call: ApplicationCall) = PublicUserResponse(
    id       = id,
    name     = name,
    username = username,
    role     = role,
    bio      = bio,
    photoUrl = buildFileUrl(call, photo, "users"),
)

/**
 * Mengubah Category entity menjadi CategoryResponse.
 */
fun Category.toCategoryResponse(totalCourses: Int = 0) = CategoryResponse(
    id          = id,
    name        = name,
    description = description,
    icon        = icon,
    totalCourses = totalCourses,
    createdAt   = createdAt,
)

/**
 * Mengubah Course entity menjadi CourseListResponse (ringkasan).
 */
fun Course.toListResponse(call: ApplicationCall, instructorName: String, categoryName: String?) =
    CourseListResponse(
        id             = id,
        instructorName = instructorName,
        categoryName   = categoryName,
        title          = title,
        slug           = slug,
        thumbnailUrl   = buildFileUrl(call, thumbnail, "courses"),
        level          = level,
        price          = price,
        discountPrice  = discountPrice,
        finalPrice     = finalPrice(),
        totalDuration  = totalDuration,
        totalLessons   = totalLessons,
        avgRating      = avgRating,
        totalStudents  = totalStudents,
        updatedAt      = updatedAt,
    )

/**
 * Mengubah Course entity menjadi CourseResponse (detail lengkap).
 */
suspend fun Course.toDetailResponse(
    call: ApplicationCall,
    userRepo: IUserRepository,
    categoryRepo: ICategoryRepository,
    isEnrolled: Boolean = false,
): CourseResponse {
    val instructor = userRepo.getById(instructorId)
    val category   = categoryId?.let { categoryRepo.getById(it) }

    return CourseResponse(
        id            = id,
        instructor    = instructor?.toPublicResponse(call)
            ?: PublicUserResponse(instructorId, "Unknown", "", "instructor", null, null),
        category      = category?.toCategoryResponse(),
        title         = title,
        slug          = slug,
        description   = description,
        thumbnailUrl  = buildFileUrl(call, thumbnail, "courses"),
        level         = level,
        language      = language,
        price         = price,
        discountPrice = discountPrice,
        finalPrice    = finalPrice(),
        status        = status,
        totalDuration = totalDuration,
        totalLessons  = totalLessons,
        totalStudents = totalStudents,
        avgRating     = avgRating,
        totalReviews  = totalReviews,
        isEnrolled    = isEnrolled,
        createdAt     = createdAt,
        updatedAt     = updatedAt,
    )
}

/**
 * Mengubah Lesson entity menjadi LessonResponse.
 */
fun Lesson.toResponse(isCompleted: Boolean = false, watchedSeconds: Int = 0) = LessonResponse(
    id             = id,
    courseId       = courseId,
    title          = title,
    description    = description,
    contentType    = contentType,
    contentUrl     = contentUrl,
    content        = content,
    duration       = duration,
    orderIndex     = orderIndex,
    isFree         = isFree,
    isPublished    = isPublished,
    isCompleted    = isCompleted,
    watchedSeconds = watchedSeconds,
    createdAt      = createdAt,
)

fun Lesson.toListResponse(isCompleted: Boolean = false) = LessonListResponse(
    id          = id,
    title       = title,
    contentType = contentType,
    duration    = duration,
    orderIndex  = orderIndex,
    isFree      = isFree,
    isPublished = isPublished,
    isCompleted = isCompleted,
)

/**
 * Mengubah Review entity menjadi ReviewResponse.
 */
fun Review.toResponse(call: ApplicationCall, user: User) = ReviewResponse(
    id        = id,
    user      = user.toPublicResponse(call),
    courseId  = courseId,
    rating    = rating,
    comment   = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
