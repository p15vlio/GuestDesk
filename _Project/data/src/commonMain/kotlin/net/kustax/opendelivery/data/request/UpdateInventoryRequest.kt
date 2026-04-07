package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInventoryRequest(
    val quantity: Int,
    val priceOverride: Long?
)
