package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.DeliveryAssignment
import net.kustax.opendelivery.domain.enum.DeliveryAssignmentStatus

interface DeliveryAssignmentRepository {
    suspend fun create(assignment: DeliveryAssignment): DeliveryAssignment
    suspend fun findByOrderId(orderId: String): DeliveryAssignment?
    suspend fun updateStatus(id: String, status: DeliveryAssignmentStatus): DeliveryAssignment
}
