package net.kustax.opendelivery.backend.service

import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderItemRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedPaymentRepository
import net.kustax.opendelivery.data.request.InitiatePaymentRequest
import net.kustax.opendelivery.data.response.PaymentResponse
import net.kustax.opendelivery.domain.entity.tenant.Payment
import net.kustax.opendelivery.domain.enum.PaymentMethod
import net.kustax.opendelivery.domain.enum.PaymentStatus
import net.kustax.opendelivery.domain.port.NotificationService
import net.kustax.opendelivery.domain.port.PaymentGateway
import java.util.UUID

class PaymentService(
    private val paymentRepository: ExposedPaymentRepository,
    private val orderRepository: ExposedOrderRepository,
    private val orderItemRepository: ExposedOrderItemRepository,
    private val paymentGateway: PaymentGateway,
    private val notificationService: NotificationService
) {

    suspend fun initiate(request: InitiatePaymentRequest): PaymentResponse {
        val order = orderRepository.findById(request.orderId)
            ?: throw NotFoundException("Order not found: ${request.orderId}")

        paymentRepository.findByOrderId(request.orderId)?.let {
            if (it.status == PaymentStatus.COMPLETED) {
                throw ConflictException("Order already paid: ${request.orderId}")
            }
        }

        val method = enumValueOf<PaymentMethod>(request.method)

        val payment = Payment(
            id = UUID.randomUUID().toString(),
            orderId = request.orderId,
            method = method,
            status = PaymentStatus.PENDING,
            amount = order.totalAmount,
            providerTransactionId = null,
            createdAt = System.currentTimeMillis()
        )
        paymentRepository.create(payment)

        // Gateway returns a transactionId but ExposedPaymentRepository has no updateTransactionId method.
        // MVP: transactionId is not persisted; status transition to COMPLETED is all that is recorded.
        paymentGateway.initiatePayment(request.orderId, order.totalAmount, method)

        val completedPayment = paymentRepository.updateStatus(payment.id, PaymentStatus.COMPLETED)

        order.guestEmail?.let { email ->
            val items = orderItemRepository.findByOrderId(request.orderId)
            notificationService.sendReceipt(email, request.orderId, order.totalAmount, items)
        }

        return completedPayment.toResponse()
    }

    suspend fun findByOrderId(orderId: String): PaymentResponse {
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw NotFoundException("No payment for order: $orderId")
        return payment.toResponse()
    }
}

private fun Payment.toResponse() = PaymentResponse(
    id = id,
    orderId = orderId,
    method = method.name,
    status = status.name,
    amount = amount,
    providerTransactionId = providerTransactionId,
    createdAt = createdAt
)
