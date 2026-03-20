package org.course

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import org.course.helpers.JWTConstants
import org.course.helpers.configureDatabases
import org.course.module.appModule
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    // Load .env dan masukkan ke System properties agar terbaca oleh application.yaml
    val dotenv = dotenv {
        directory          = "."
        ignoreIfMissing    = true   // agar tidak error di production (env vars dari OS)
        systemProperties   = true
    }
    dotenv.entries().forEach { System.setProperty(it.key, it.value) }

    EngineMain.main(args)
}

fun Application.module() {
    val jwtSecret = environment.config.property("ktor.jwt.secret").getString()

    // ── JWT Authentication ────────────────────────────────────────────────────
    install(Authentication) {
        jwt(JWTConstants.NAME) {
            realm = JWTConstants.REALM
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(JWTConstants.ISSUER)
                    .withAudience(JWTConstants.AUDIENCE)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (!userId.isNullOrBlank()) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("status" to "error", "message" to "Token tidak valid atau sudah kedaluwarsa. Silakan login kembali.")
                )
            }
        }
    }

    // ── CORS ──────────────────────────────────────────────────────────────────
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    // ── Content Negotiation (JSON) ─────────────────────────────────────────────
    install(ContentNegotiation) {
        json(Json {
            prettyPrint        = true
            isLenient          = true
            ignoreUnknownKeys  = true
            explicitNulls      = false
            encodeDefaults     = true
        })
    }

    // ── Koin Dependency Injection ─────────────────────────────────────────────
    install(Koin) {
        slf4jLogger()
        modules(appModule(jwtSecret))
    }

    // ── Database + Routing ────────────────────────────────────────────────────
    configureDatabases()
    configureRouting()
}
