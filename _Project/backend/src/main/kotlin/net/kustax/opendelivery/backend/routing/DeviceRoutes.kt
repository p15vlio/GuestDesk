package net.kustax.opendelivery.backend.routing

import io.ktor.http.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import net.kustax.opendelivery.backend.database.TenantContext
import net.kustax.opendelivery.backend.service.DeviceService
import net.kustax.opendelivery.backend.service.ForbiddenException
import net.kustax.opendelivery.data.request.ActivateDeviceRequest
import net.kustax.opendelivery.data.request.CreateDeviceRequest
import net.kustax.opendelivery.data.response.ErrorResponse
import net.kustax.opendelivery.domain.enum.UserRole

fun Route.deviceRoutes(deviceService: DeviceService) {
    route("/properties/{propertyId}/devices") {
        post {
            if (call.userRole() != UserRole.OWNER && call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Owner access required")
            }
            val actorId = call.principal<JWTPrincipal>()?.subject ?: ""
            val actorRole = call.userRole().name
            val schema = call.tenantSchema() ?: ""
            val propertyId = call.parameters["propertyId"]!!
            val request = call.receive<CreateDeviceRequest>()
            call.withTenant {
                val response = deviceService.create(request, propertyId, actorId, actorRole, schema)
                call.respond(HttpStatusCode.Created, response)
            }
        }

        get {
            if (call.userRole() != UserRole.OWNER && call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Owner access required")
            }
            val propertyId = call.parameters["propertyId"]!!
            call.withTenant {
                val response = deviceService.findByPropertyId(propertyId)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }

    route("/devices/{id}") {
        get {
            if (call.userRole() != UserRole.OWNER && call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Owner access required")
            }
            val id = call.parameters["id"]!!
            call.withTenant {
                val response = deviceService.findById(id)
                call.respond(HttpStatusCode.OK, response)
            }
        }

        delete {
            if (call.userRole() != UserRole.OWNER && call.userRole() != UserRole.PLATFORM_ADMIN) {
                throw ForbiddenException("Owner access required")
            }
            val actorId = call.principal<JWTPrincipal>()?.subject ?: ""
            val actorRole = call.userRole().name
            val schema = call.tenantSchema() ?: ""
            val id = call.parameters["id"]!!
            call.withTenant {
                deviceService.delete(id, actorId, actorRole, schema)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// Mounted outside the authenticate block in Routing.kt — the kiosk device has no JWT.
// The schema name is passed as a query param so TenantContext can be set for the tenantQuery.
fun Route.devicePingRoute(deviceService: DeviceService) {
    patch("/devices/{id}/ping") {
        val id = call.parameters["id"]!!
        val schema = call.request.queryParameters["schemaName"]
            ?: return@patch call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "BAD_REQUEST", message = "schemaName query parameter is required")
            )
        withContext(TenantContext(schema) + currentCoroutineContext()) {
            deviceService.updateLastSeen(id)
        }
        call.respond(HttpStatusCode.NoContent)
    }
}

// Mounted outside the authenticate block — the kiosk app self-registers using a QR-scanned activation code.
fun Route.deviceActivateRoute(deviceService: DeviceService) {
    post("/devices/{id}/activate") {
        val id = call.parameters["id"]!!
        val schema = call.request.queryParameters["schemaName"]
            ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "BAD_REQUEST", message = "schemaName query parameter is required")
            )
        val request = call.receive<ActivateDeviceRequest>()
        withContext(TenantContext(schema) + currentCoroutineContext()) {
            val response = deviceService.activate(id, request.activationCode, request.androidDeviceId)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
