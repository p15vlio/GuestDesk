package net.kustax.opendelivery.data.response

import kotlinx.serialization.Serializable

@Serializable
data class PartnerStoreResponse(
    val id: String,
    val name: String,
    val contactEmail: String,
    val contactPhone: String,
    val isPharmacy: Boolean,
    val isActive: Boolean,
    val createdAt: Long
)
