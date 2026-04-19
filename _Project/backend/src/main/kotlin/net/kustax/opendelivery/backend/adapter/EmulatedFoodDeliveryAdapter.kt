package net.kustax.opendelivery.backend.adapter

import net.kustax.opendelivery.domain.entity.tenant.Order
import net.kustax.opendelivery.domain.entity.tenant.OrderItem
import net.kustax.opendelivery.domain.enum.OrderStatus
import net.kustax.opendelivery.domain.port.FoodDeliveryProvider
import org.slf4j.LoggerFactory
import java.util.UUID

class EmulatedFoodDeliveryAdapter : FoodDeliveryProvider {

    private val logger = LoggerFactory.getLogger(EmulatedFoodDeliveryAdapter::class.java)

    override suspend fun placeOrder(order: Order, items: List<OrderItem>): String {
        val externalOrderId = "EMULATED_EXT_${UUID.randomUUID()}"
        logger.info("Emulated food delivery order placed for order ${order.id}")
        return externalOrderId
    }

    override suspend fun getOrderStatus(externalOrderId: String): OrderStatus {
        return OrderStatus.CONFIRMED
    }

    override suspend fun cancelOrder(externalOrderId: String): Boolean {
        logger.info("Emulated cancel for external order $externalOrderId")
        return true
    }
}
