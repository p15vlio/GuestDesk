package net.kustax.opendelivery.domain.entity.tenant

import net.kustax.opendelivery.domain.enum.PaymentMethod
import net.kustax.opendelivery.domain.enum.PaymentStatus

data class Payment(
    val id: String,
    val orderId: String,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val amount: Long,
    val providerTransactionId: String?,
    val createdAt: Long
)
