package net.kustax.opendelivery.data.response

import kotlinx.serialization.Serializable

@Serializable
data class InventoryResponse(
    val id: String,
    val productId: String,
    val propertyId: String,
    val quantity: Int,
    val priceOverride: Long?
)
