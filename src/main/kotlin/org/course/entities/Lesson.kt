package org.course.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Lesson(
    var id: String = UUID.randomUUID().toString(),
    var courseId: String,
    var title: String,
    var description: String? = null,
    var contentType: String = "video",
    var contentUrl: String? = null,
    var content: String? = null,
    var duration: Int = 0,
    var orderIndex: Int = 0,
    var isFree: Boolean = false,
    var isPublished: Boolean = false,
    @Contextual val createdAt: Instant = Clock.System.now(),
    @Contextual var updatedAt: Instant = Clock.System.now(),
)
