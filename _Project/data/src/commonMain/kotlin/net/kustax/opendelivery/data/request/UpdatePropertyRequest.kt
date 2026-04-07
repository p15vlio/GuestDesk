package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePropertyRequest(
    val name: String,
    val streetName: String,
    val streetNo: String,
    val postalCode: String,
    val area: String? = null,
    val level: Int = 0,
    val nameOnDoorbell: String,
    val contactPhone: String,
    val fulfillmentModel: String,
    val notes: String? = null
)
