package net.kustax.opendelivery.backend.adapter

import net.kustax.opendelivery.domain.enum.DeliveryAssignmentStatus
import net.kustax.opendelivery.domain.port.LogisticsProvider
import org.slf4j.LoggerFactory
import java.util.UUID

class EmulatedLogisticsAdapter : LogisticsProvider {

    private val logger = LoggerFactory.getLogger(EmulatedLogisticsAdapter::class.java)

    override suspend fun requestPickup(orderId: String, pickupAddress: String, dropAddress: String): String {
        val trackingId = "EMULATED_TRACK_${UUID.randomUUID()}"
        logger.info("Emulated pickup requested for order $orderId")
        return trackingId
    }

    override suspend fun getDeliveryStatus(trackingId: String): DeliveryAssignmentStatus {
        return DeliveryAssignmentStatus.DELIVERED
    }

    override suspend fun cancelDelivery(trackingId: String): Boolean {
        logger.info("Emulated cancel delivery for tracking $trackingId")
        return true
    }
}
