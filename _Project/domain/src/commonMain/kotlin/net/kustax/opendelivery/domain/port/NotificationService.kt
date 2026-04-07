package net.kustax.opendelivery.domain.port

import net.kustax.opendelivery.domain.entity.tenant.OrderItem

interface NotificationService {
    suspend fun sendReceipt(toEmail: String, orderId: String, amount: Long, items: List<OrderItem>)
}
