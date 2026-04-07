package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val guestEmail: String?,
    val type: String,
    val partnerStoreId: String?,
    val items: List<OrderItemRequest>,
    val notes: String?
)
