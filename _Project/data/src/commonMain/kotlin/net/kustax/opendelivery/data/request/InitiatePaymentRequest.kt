package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class InitiatePaymentRequest(
    val orderId: String,
    val method: String
)
