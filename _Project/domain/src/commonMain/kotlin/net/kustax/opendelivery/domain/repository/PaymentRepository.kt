package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.Payment
import net.kustax.opendelivery.domain.enum.PaymentStatus

interface PaymentRepository {
    suspend fun create(payment: Payment): Payment
    suspend fun findByOrderId(orderId: String): Payment?
    suspend fun updateStatus(id: String, status: PaymentStatus): Payment
}
