package net.kustax.opendelivery.backend

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import net.kustax.opendelivery.backend.database.DatabaseFactory
import net.kustax.opendelivery.backend.database.SchemaManager
import net.kustax.opendelivery.backend.routing.configureRouting
import net.kustax.opendelivery.backend.security.JwtConfig
import net.kustax.opendelivery.backend.service.AuthenticationException
import net.kustax.opendelivery.backend.service.ConflictException
import net.kustax.opendelivery.backend.service.ForbiddenException
import net.kustax.opendelivery.backend.service.NotFoundException
import net.kustax.opendelivery.data.response.ErrorResponse

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    if (System.getenv("RESET_DB") == "true") SchemaManager.dropAndRecreate()
    else SchemaManager.createPlatformTables()

    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(DefaultHeaders)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
    }

    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(code = "UNAUTHORIZED", message = cause.message ?: "Unauthorized")
            )
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(code = "NOT_FOUND", message = cause.message ?: "Not found"))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(code = "CONFLICT", message = cause.message ?: "Conflict"))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(code = "FORBIDDEN", message = cause.message ?: "Forbidden"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(code = "BAD_REQUEST", message = cause.message ?: "Invalid parameter value"))
        }
        exception<Exception> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(code = "INTERNAL_SERVER_ERROR", message = cause.message ?: "Internal server error")
            )
        }
    }

    install(Authentication) {
        jwt("jwt") {
            realm = "opendelivery"
            verifier(JwtConfig.verifier())
            validate { credential ->
                if (credential.payload.getClaim(JwtConfig.CLAIM_ROLE).asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
        }
    }

    configureRouting()
}
