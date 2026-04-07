package net.kustax.opendelivery.domain.port

import net.kustax.opendelivery.domain.enum.PaymentMethod
import net.kustax.opendelivery.domain.enum.PaymentStatus

// Viva App2App, Viva Cloud, and Revolut each implement this contract
interface PaymentGateway {
    suspend fun initiatePayment(orderId: String, amount: Long, method: PaymentMethod): String
    suspend fun getPaymentStatus(transactionId: String): PaymentStatus
    suspend fun refund(transactionId: String, amount: Long): Boolean
}
