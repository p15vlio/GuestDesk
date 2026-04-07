package net.kustax.opendelivery.domain.entity.tenant

import net.kustax.opendelivery.domain.enum.DeliveryAssignmentStatus

data class DeliveryAssignment(
    val id: String,
    val orderId: String,
    // set when provider = OWN_RIDER
    val riderId: String?,
    // set when provider = STORE_SELF
    val storePartnerId: String?,
    val status: DeliveryAssignmentStatus,
    val assignedAt: Long,
    val pickedUpAt: Long?,
    val deliveredAt: Long?
)
