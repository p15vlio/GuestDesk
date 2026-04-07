package net.kustax.opendelivery.domain.port

import net.kustax.opendelivery.domain.enum.DeliveryAssignmentStatus

// WoltDrive and own riders both implement this contract
interface LogisticsProvider {
    suspend fun requestPickup(orderId: String, pickupAddress: String, dropAddress: String): String
    suspend fun getDeliveryStatus(trackingId: String): DeliveryAssignmentStatus
    suspend fun cancelDelivery(trackingId: String): Boolean
}
