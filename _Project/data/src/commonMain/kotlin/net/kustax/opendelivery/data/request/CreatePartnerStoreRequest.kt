package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class CreatePartnerStoreRequest(
    val name: String,
    val contactEmail: String,
    val contactPhone: String,
    val isPharmacy: Boolean
)
