package net.kustax.opendelivery.domain.port

import net.kustax.opendelivery.domain.entity.tenant.Order
import net.kustax.opendelivery.domain.entity.tenant.OrderItem
import net.kustax.opendelivery.domain.enum.OrderStatus

// efood and Wolt both implement this contract
interface FoodDeliveryProvider {
    suspend fun placeOrder(order: Order, items: List<OrderItem>): String
    suspend fun getOrderStatus(externalOrderId: String): OrderStatus
    suspend fun cancelOrder(externalOrderId: String): Boolean
}
