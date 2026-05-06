package net.kustax.opendelivery.backend.routing

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import net.kustax.opendelivery.backend.database.TenantContext
import net.kustax.opendelivery.backend.service.ForbiddenException
import net.kustax.opendelivery.backend.service.OrderService
import net.kustax.opendelivery.data.request.UpdateOrderStatusRequest
import net.kustax.opendelivery.data.request.CreateOrderRequest
import net.kustax.opendelivery.data.response.ErrorResponse
import net.kustax.opendelivery.domain.enum.OrderStatus
import net.kustax.opendelivery.domain.enum.UserRole

fun Route.orderRoutes(orderService: OrderService) {
    route("/orders") {
        // Unauthenticated — kiosk devices post orders without a JWT.
        // schemaName, deviceId, and propertyId are required query parameters.
        post {
            val schema = call.request.queryParameters["schemaName"]
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(code = "BAD_REQUEST", message = "schemaName query parameter is required")
                )
            val deviceId = call.request.queryParameters["deviceId"]
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(code = "BAD_REQUEST", message = "deviceId query parameter is required")
                )
            val propertyId = call.request.queryParameters["propertyId"]
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(code = "BAD_REQUEST", message = "propertyId query parameter is required")
                )
            val request = call.receive<CreateOrderRequest>()
            withContext(TenantContext(schema) + currentCoroutineContext()) {
                val response = orderService.create(request, deviceId, propertyId)
                call.respond(HttpStatusCode.Created, response)
            }
        }

        get {
            if (call.userRole() != UserRole.OWNER) throw ForbiddenException("Owner access required")
            val propertyId = call.request.queryParameters["propertyId"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(code = "BAD_REQUEST", message = "propertyId query parameter is required")
                )
            call.withTenant {
                val statusParam = call.request.queryParameters["status"]
                val response = if (statusParam != null) {
                    orderService.findByStatus(enumValueOf<OrderStatus>(statusParam))
                } else {
                    orderService.findByPropertyId(propertyId)
                }
                call.respond(HttpStatusCode.OK, response)
            }
        }

        get("/{id}") {
            if (call.userRole() != UserRole.OWNER) throw ForbiddenException("Owner access required")
            val id = call.parameters["id"]!!
            call.withTenant {
                val response = orderService.findById(id)
                call.respond(HttpStatusCode.OK, response)
            }
        }

        patch("/{id}/status") {
            if (call.userRole() != UserRole.OWNER) throw ForbiddenException("Owner access required")
            val id = call.parameters["id"]!!
            val request = call.receive<UpdateOrderStatusRequest>()
            call.withTenant {
                val response = orderService.updateStatus(id, request)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}

fun Route.deviceOrderRoutes(orderService: OrderService) {
    route("/device/orders") {
        get("/{id}") {
            val schema = call.request.queryParameters["schemaName"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(code = "BAD_REQUEST", message = "schemaName query parameter is required")
                )
            val id = call.parameters["id"]!!
            withContext(TenantContext(schema) + currentCoroutineContext()) {
                val response = orderService.findById(id)
                call.respond(HttpStatusCode.OK, response)
            }
        }

        patch("/{id}/cancel") {
            val schema = call.request.queryParameters["schemaName"]
                ?: return@patch call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(code = "BAD_REQUEST", message = "schemaName query parameter is required")
                )
            val id = call.parameters["id"]!!
            withContext(TenantContext(schema) + currentCoroutineContext()) {
                val response = orderService.updateStatus(
                    id,
                    UpdateOrderStatusRequest(status = "CANCELLED")
                )
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
