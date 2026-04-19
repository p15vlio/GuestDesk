package net.kustax.opendelivery.backend.adapter

import net.kustax.opendelivery.domain.enum.PaymentMethod
import net.kustax.opendelivery.domain.enum.PaymentStatus
import net.kustax.opendelivery.domain.port.PaymentGateway
import org.slf4j.LoggerFactory
import java.util.UUID

class EmulatedPaymentAdapter : PaymentGateway {

    private val logger = LoggerFactory.getLogger(EmulatedPaymentAdapter::class.java)

    override suspend fun initiatePayment(orderId: String, amount: Long, method: PaymentMethod): String {
        val transactionId = "EMULATED_TXN_${UUID.randomUUID()}"
        logger.info("Emulated payment initiated for order $orderId amount $amount method $method")
        return transactionId
    }

    override suspend fun getPaymentStatus(transactionId: String): PaymentStatus {
        return PaymentStatus.COMPLETED
    }

    override suspend fun refund(transactionId: String, amount: Long): Boolean {
        logger.info("Emulated refund for transaction $transactionId amount $amount")
        return true
    }
}
