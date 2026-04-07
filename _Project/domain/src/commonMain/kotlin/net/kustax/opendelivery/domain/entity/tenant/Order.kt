package net.kustax.opendelivery.domain.entity.tenant

import net.kustax.opendelivery.domain.enum.DeliveryProvider
import net.kustax.opendelivery.domain.enum.ExternalPlatform
import net.kustax.opendelivery.domain.enum.FulfillmentModel
import net.kustax.opendelivery.domain.enum.OrderStatus
import net.kustax.opendelivery.domain.enum.OrderType

data class Order(
    val id: String,
    val deviceId: String,
    val propertyId: String,
    val guestEmail: String?,
    val status: OrderStatus,
    val type: OrderType,
    val fulfillmentModel: FulfillmentModel,
    val deliveryProvider: DeliveryProvider?,
    val externalPlatform: ExternalPlatform?,
    // set when type = PARTNER_STORE
    val partnerStoreId: String?,
    val totalAmount: Long,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)
