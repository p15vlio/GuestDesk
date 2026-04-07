package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePartnerStoreRequest(
    val contactEmail: String,
    val contactPhone: String
)
