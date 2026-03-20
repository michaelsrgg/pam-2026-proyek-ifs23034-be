package org.course.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var username: String,
    var email: String,
    var password: String,
    var role: String = "student",
    var bio: String? = null,
    var photo: String? = null,
    var isActive: Boolean = true,
    @Contextual val createdAt: Instant = Clock.System.now(),
    @Contextual var updatedAt: Instant = Clock.System.now(),
)
