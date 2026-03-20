package org.course.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Enrollment(
    var id: String = UUID.randomUUID().toString(),
    var userId: String,
    var courseId: String,
    var status: String = "active",
    var progress: Int = 0,
    var paidAmount: Long = 0,
    @Contextual val enrolledAt: Instant = Clock.System.now(),
    @Contextual var completedAt: Instant? = null,
    @Contextual var updatedAt: Instant = Clock.System.now(),
)
