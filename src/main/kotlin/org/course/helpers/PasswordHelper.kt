package org.course.helpers

import org.mindrot.jbcrypt.BCrypt

fun hashPassword(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt(12))

fun verifyPassword(plain: String, hashed: String): Boolean =
    runCatching { BCrypt.checkpw(plain, hashed) }.getOrElse { false }
