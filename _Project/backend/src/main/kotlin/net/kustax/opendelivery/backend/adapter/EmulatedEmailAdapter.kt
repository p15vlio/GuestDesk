package net.kustax.opendelivery.backend.adapter

import net.kustax.opendelivery.domain.entity.tenant.OrderItem
import net.kustax.opendelivery.domain.port.NotificationService
import org.slf4j.LoggerFactory

class EmulatedEmailAdapter : NotificationService {

    private val logger = LoggerFactory.getLogger(EmulatedEmailAdapter::class.java)

    override suspend fun sendReceipt(toEmail: String, orderId: String, amount: Long, items: List<OrderItem>) {
        logger.info("[EMAIL] Receipt for order $orderId sent to $toEmail | Total: $amount cents | Items: ${items.size}")
    }
}
