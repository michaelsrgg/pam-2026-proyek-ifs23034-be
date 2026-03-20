package org.course.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RefreshToken(
    var id: String = UUID.randomUUID().toString(),
    var userId: String,
    var authToken: String,
    var refreshToken: String,
    @Contextual val createdAt: Instant = Clock.System.now(),
)
