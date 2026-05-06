package net.kustax.opendelivery.backend.routing

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import net.kustax.opendelivery.backend.database.TenantContext
import net.kustax.opendelivery.backend.service.ForbiddenException
import net.kustax.opendelivery.backend.service.PaymentService
import net.kustax.opendelivery.data.request.InitiatePaymentRequest
import net.kustax.opendelivery.data.response.ErrorResponse
import net.kustax.opendelivery.domain.enum.UserRole

fun Route.paymentRoutes(paymentService: PaymentService) {
    // Authenticated kiosk device route
    post("/payments") {
        val request = call.receive<InitiatePaymentRequest>()
        call.withTenant {
            val response = paymentService.initiate(request)
            call.respond(HttpStatusCode.Created, response)
        }
    }

    get("/orders/{orderId}/payment") {
        if (call.userRole() != UserRole.OWNER) {
            throw ForbiddenException("Owner access required")
        }
        val orderId = call.parameters["orderId"]!!
        call.withTenant {
            val response = paymentService.findByOrderId(orderId)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
