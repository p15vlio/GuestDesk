package net.kustax.opendelivery.data.response

import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val id: String,
    val deviceId: String,
    val propertyId: String,
    val guestEmail: String?,
    val status: String,
    val type: String,
    val fulfillmentModel: String,
    val deliveryProvider: String?,
    val externalPlatform: String?,
    val partnerStoreId: String?,
    val totalAmount: Long,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<OrderItemResponse>
)
