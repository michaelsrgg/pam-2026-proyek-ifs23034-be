package org.course.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Review(
    var id: String = UUID.randomUUID().toString(),
    var userId: String,
    var courseId: String,
    var rating: Int,
    var comment: String? = null,
    @Contextual val createdAt: Instant = Clock.System.now(),
    @Contextual var updatedAt: Instant = Clock.System.now(),
)
