package net.kustax.opendelivery.backend.routing

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import net.kustax.opendelivery.backend.repository.platform.ExposedAuthRepository
import net.kustax.opendelivery.backend.service.AuthService

fun Application.configureRouting() {
    val authRepository = ExposedAuthRepository()
    val authService = AuthService(authRepository)

    routing {
        route("/api") {
            healthRoute()
            authRoutes(authService)
        }
    }
}
