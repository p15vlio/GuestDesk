package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.Order
import net.kustax.opendelivery.domain.enum.OrderStatus

interface OrderRepository {
    suspend fun create(order: Order): Order
    suspend fun findById(id: String): Order?
    suspend fun findByPropertyId(propertyId: String): List<Order>
    suspend fun findByDeviceId(deviceId: String): List<Order>
    suspend fun findByStatus(status: OrderStatus): List<Order>
    suspend fun updateStatus(id: String, status: OrderStatus): Order
    suspend fun assignDelivery(orderId: String, deliveryAssignmentId: String)
}
