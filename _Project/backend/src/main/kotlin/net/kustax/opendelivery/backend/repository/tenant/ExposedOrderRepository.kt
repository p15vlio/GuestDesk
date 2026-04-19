package net.kustax.opendelivery.backend.repository.tenant

import net.kustax.opendelivery.backend.database.table.tenant.OrdersTable
import net.kustax.opendelivery.backend.repository.tenantQuery
import net.kustax.opendelivery.domain.entity.tenant.Order
import net.kustax.opendelivery.domain.enum.DeliveryProvider
import net.kustax.opendelivery.domain.enum.ExternalPlatform
import net.kustax.opendelivery.domain.enum.FulfillmentModel
import net.kustax.opendelivery.domain.enum.OrderStatus
import net.kustax.opendelivery.domain.enum.OrderType
import net.kustax.opendelivery.domain.repository.OrderRepository
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

class ExposedOrderRepository : OrderRepository {

    override suspend fun create(order: Order): Order = tenantQuery {
        OrdersTable.insert {
            it[id] = order.id
            it[deviceId] = order.deviceId
            it[propertyId] = order.propertyId
            it[guestEmail] = order.guestEmail
            it[status] = order.status.name
            it[type] = order.type.name
            it[fulfillmentModel] = order.fulfillmentModel.name
            it[deliveryProvider] = order.deliveryProvider?.name
            it[externalPlatform] = order.externalPlatform?.name
            it[partnerStoreId] = order.partnerStoreId
            it[totalAmount] = order.totalAmount
            it[notes] = order.notes
            it[createdAt] = order.createdAt
            it[updatedAt] = order.updatedAt
        }
        order
    }

    override suspend fun findById(id: String): Order? = tenantQuery {
        OrdersTable.selectAll()
            .where { OrdersTable.id eq id }
            .singleOrNull()
            ?.toOrder()
    }

    override suspend fun findByPropertyId(propertyId: String): List<Order> = tenantQuery {
        OrdersTable.selectAll()
            .where { OrdersTable.propertyId eq propertyId }
            .map { it.toOrder() }
    }

    override suspend fun findByDeviceId(deviceId: String): List<Order> = tenantQuery {
        OrdersTable.selectAll()
            .where { OrdersTable.deviceId eq deviceId }
            .map { it.toOrder() }
    }

    override suspend fun findByStatus(status: OrderStatus): List<Order> = tenantQuery {
        OrdersTable.selectAll()
            .where { OrdersTable.status eq status.name }
            .map { it.toOrder() }
    }

    override suspend fun updateStatus(id: String, status: OrderStatus): Order = tenantQuery {
        val now = System.currentTimeMillis()
        OrdersTable.update({ OrdersTable.id eq id }) {
            it[OrdersTable.status] = status.name
            it[updatedAt] = now
        }
        OrdersTable.selectAll()
            .where { OrdersTable.id eq id }
            .single()
            .toOrder()
    }

    // The DeliveryAssignment record already stores orderId — the link is queryable from that direction
    override suspend fun assignDelivery(orderId: String, deliveryAssignmentId: String) = Unit
}

private fun ResultRow.toOrder() = Order(
    id = this[OrdersTable.id],
    deviceId = this[OrdersTable.deviceId],
    propertyId = this[OrdersTable.propertyId],
    guestEmail = this[OrdersTable.guestEmail],
    status = enumValueOf<OrderStatus>(this[OrdersTable.status]),
    type = enumValueOf<OrderType>(this[OrdersTable.type]),
    fulfillmentModel = enumValueOf<FulfillmentModel>(this[OrdersTable.fulfillmentModel]),
    deliveryProvider = this[OrdersTable.deliveryProvider]?.let { enumValueOf<DeliveryProvider>(it) },
    externalPlatform = this[OrdersTable.externalPlatform]?.let { enumValueOf<ExternalPlatform>(it) },
    partnerStoreId = this[OrdersTable.partnerStoreId],
    totalAmount = this[OrdersTable.totalAmount],
    notes = this[OrdersTable.notes],
    createdAt = this[OrdersTable.createdAt],
    updatedAt = this[OrdersTable.updatedAt]
)
