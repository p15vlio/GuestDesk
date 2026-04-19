package net.kustax.opendelivery.backend.routing

import io.ktor.http.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.kustax.opendelivery.backend.service.ForbiddenException
import net.kustax.opendelivery.backend.service.OwnerService
import net.kustax.opendelivery.data.request.CreateOwnerRequest
import net.kustax.opendelivery.data.request.UpdateOwnerRequest
import net.kustax.opendelivery.domain.enum.UserRole

fun Route.ownerRoutes(ownerService: OwnerService) {
    route("/owners") {
        post {
            if (call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Platform admin access required")
            }
            val actorId = call.principal<JWTPrincipal>()?.subject ?: ""
            val actorRole = call.userRole().name
            val request = call.receive<CreateOwnerRequest>()
            val response = ownerService.create(request, actorId, actorRole)
            call.respond(HttpStatusCode.Created, response)
        }

        get {
            if (call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Platform admin access required")
            }
            val response = ownerService.findAll()
            call.respond(HttpStatusCode.OK, response)
        }

        get("/{id}") {
            if (call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Platform admin access required")
            }
            val response = ownerService.findById(call.parameters["id"]!!)
            call.respond(HttpStatusCode.OK, response)
        }

        put("/{id}") {
            if (call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Platform admin access required")
            }
            val actorId = call.principal<JWTPrincipal>()?.subject ?: ""
            val actorRole = call.userRole().name
            val request = call.receive<UpdateOwnerRequest>()
            val response = ownerService.update(call.parameters["id"]!!, request, actorId, actorRole)
            call.respond(HttpStatusCode.OK, response)
        }

        delete("/{id}") {
            if (call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Platform admin access required")
            }
            val actorId = call.principal<JWTPrincipal>()?.subject ?: ""
            val actorRole = call.userRole().name
            ownerService.delete(call.parameters["id"]!!, actorId, actorRole)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
