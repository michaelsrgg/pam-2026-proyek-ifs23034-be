package org.course.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LessonProgress(
    var id: String = UUID.randomUUID().toString(),
    var userId: String,
    var lessonId: String,
    var courseId: String,
    var isCompleted: Boolean = false,
    var watchedSeconds: Int = 0,
    @Contextual var completedAt: Instant? = null,
    @Contextual val createdAt: Instant = Clock.System.now(),
    @Contextual var updatedAt: Instant = Clock.System.now(),
)
