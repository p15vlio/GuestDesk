package net.kustax.opendelivery.data.response

import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val id: String,
    val orderId: String,
    val method: String,
    val status: String,
    val amount: Long,
    val providerTransactionId: String?,
    val createdAt: Long
)
