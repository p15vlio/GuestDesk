package net.kustax.opendelivery.backend.routing

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.kustax.opendelivery.backend.service.AuthService
import net.kustax.opendelivery.data.request.LoginRequest
import net.kustax.opendelivery.data.request.RefreshTokenRequest

fun Route.authRoutes(authService: AuthService) {
    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val response = authService.login(request)
        call.respond(HttpStatusCode.OK, response)
    }

    post("/auth/refresh") {
        val request = call.receive<RefreshTokenRequest>()
        val response = authService.refresh(request)
        call.respond(HttpStatusCode.OK, response)
    }
}
