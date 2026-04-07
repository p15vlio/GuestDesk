package net.kustax.opendelivery.data.response

import kotlinx.serialization.Serializable

@Serializable
data class DeliveryAssignmentResponse(
    val id: String,
    val orderId: String,
    val riderId: String?,
    val storePartnerId: String?,
    val status: String,
    val assignedAt: Long,
    val pickedUpAt: Long?,
    val deliveredAt: Long?
)
