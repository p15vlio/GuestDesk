package net.kustax.opendelivery.backend.service

import java.util.UUID
import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderItemRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedProductRepository
import net.kustax.opendelivery.data.request.CreateOrderRequest
import net.kustax.opendelivery.data.request.UpdateOrderStatusRequest
import net.kustax.opendelivery.data.response.OrderItemResponse
import net.kustax.opendelivery.data.response.OrderResponse
import net.kustax.opendelivery.domain.entity.tenant.Order
import net.kustax.opendelivery.domain.entity.tenant.OrderItem
import net.kustax.opendelivery.domain.enum.DeliveryProvider
import net.kustax.opendelivery.domain.enum.ExternalPlatform
import net.kustax.opendelivery.domain.enum.FulfillmentModel
import net.kustax.opendelivery.domain.enum.OrderStatus
import net.kustax.opendelivery.domain.enum.OrderType
import net.kustax.opendelivery.domain.port.FoodDeliveryProvider
import net.kustax.opendelivery.domain.port.LogisticsProvider

class OrderService(
    private val orderRepository: ExposedOrderRepository,
    private val orderItemRepository: ExposedOrderItemRepository,
    private val productRepository: ExposedProductRepository,
    private val foodDeliveryProvider: FoodDeliveryProvider,
    private val logisticsProvider: LogisticsProvider
) {

    suspend fun create(request: CreateOrderRequest, deviceId: String, propertyId: String): OrderResponse {
        val products = request.items.map { itemRequest ->
            val product = productRepository.findById(itemRequest.productId)
                ?: throw NotFoundException("Product not found: ${itemRequest.productId}")
            if (!product.isAvailable) throw ConflictException("Product not available: ${itemRequest.productId}")
            product to itemRequest.quantity
        }

        val totalAmount = products.sumOf { (product, quantity) -> product.basePrice * quantity }
        val orderId = UUID.randomUUID().toString()
        val orderType = enumValueOf<OrderType>(request.type)
        val now = System.currentTimeMillis()

        val (fulfillmentModel, deliveryProvider, externalPlatform) = when (orderType) {
            OrderType.IN_ROOM_STORE -> Triple(FulfillmentModel.OWN_INFRASTRUCTURE, null, null)
            OrderType.PARTNER_STORE -> Triple(FulfillmentModel.OWN_INFRASTRUCTURE, DeliveryProvider.OWN_RIDER, null)
            OrderType.FOOD_DELIVERY -> Triple(FulfillmentModel.EXTERNAL_SERVICE, DeliveryProvider.WOLT_DRIVE, ExternalPlatform.WOLT)
        }

        val order = Order(
            id = orderId,
            deviceId = deviceId,
            propertyId = propertyId,
            guestEmail = request.guestEmail,
            status = OrderStatus.PENDING,
            type = orderType,
            fulfillmentModel = fulfillmentModel,
            deliveryProvider = deliveryProvider,
            externalPlatform = externalPlatform,
            partnerStoreId = request.partnerStoreId,
            totalAmount = totalAmount,
            notes = request.notes,
            createdAt = now,
            updatedAt = now
        )
        orderRepository.create(order)

        val orderItems = products.map { (product, quantity) ->
            OrderItem(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                productId = product.id,
                productName = product.name,
                unitPrice = product.basePrice,
                quantity = quantity,
                subtotal = product.basePrice * quantity
            )
        }
        orderItems.forEach { orderItemRepository.insert(it) }

        when (orderType) {
            OrderType.FOOD_DELIVERY -> foodDeliveryProvider.placeOrder(order, orderItems)
            OrderType.PARTNER_STORE -> logisticsProvider.requestPickup(orderId, "", "")
            OrderType.IN_ROOM_STORE -> Unit
        }

        orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED)

        val confirmedOrder = orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found after creation: $orderId")
        val items = orderItemRepository.findByOrderId(orderId)
        return confirmedOrder.toResponse(items)
    }

    suspend fun updateStatus(id: String, request: UpdateOrderStatusRequest): OrderResponse {
        val order = orderRepository.findById(id) ?: throw NotFoundException("Order not found: $id")
        val newStatus = enumValueOf<OrderStatus>(request.status)

        if (!isValidTransition(order.status, newStatus)) {
            throw ConflictException("Cannot transition from ${order.status} to $newStatus")
        }

        orderRepository.updateStatus(id, newStatus)
        val items = orderItemRepository.findByOrderId(id)
        val updated = orderRepository.findById(id)
            ?: throw NotFoundException("Order not found after status update: $id")
        return updated.toResponse(items)
    }

    suspend fun findById(id: String): OrderResponse {
        val order = orderRepository.findById(id) ?: throw NotFoundException("Order not found: $id")
        val items = orderItemRepository.findByOrderId(id)
        return order.toResponse(items)
    }

    suspend fun findByPropertyId(propertyId: String): List<OrderResponse> =
        orderRepository.findByPropertyId(propertyId).map { order ->
            val items = orderItemRepository.findByOrderId(order.id)
            order.toResponse(items)
        }

    suspend fun findByStatus(status: OrderStatus): List<OrderResponse> =
        orderRepository.findByStatus(status).map { order ->
            val items = orderItemRepository.findByOrderId(order.id)
            order.toResponse(items)
        }

    private fun isValidTransition(from: OrderStatus, to: OrderStatus): Boolean {
        val finalStates = setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
        if (from in finalStates) return false
        if (to == OrderStatus.CANCELLED) return true
        return when (from) {
            OrderStatus.PENDING -> to == OrderStatus.CONFIRMED
            OrderStatus.CONFIRMED -> to == OrderStatus.PREPARING
            OrderStatus.PREPARING -> to == OrderStatus.OUT_FOR_DELIVERY
            OrderStatus.OUT_FOR_DELIVERY -> to == OrderStatus.DELIVERED
            else -> false
        }
    }
}

private fun Order.toResponse(items: List<OrderItem>) = OrderResponse(
    id = id,
    deviceId = deviceId,
    propertyId = propertyId,
    guestEmail = guestEmail,
    status = status.name,
    type = type.name,
    fulfillmentModel = fulfillmentModel.name,
    deliveryProvider = deliveryProvider?.name,
    externalPlatform = externalPlatform?.name,
    partnerStoreId = partnerStoreId,
    totalAmount = totalAmount,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    items = items.map { it.toItemResponse() }
)

private fun OrderItem.toItemResponse() = OrderItemResponse(
    id = id,
    orderId = orderId,
    productId = productId,
    productName = productName,
    unitPrice = unitPrice,
    quantity = quantity,
    subtotal = subtotal
)
