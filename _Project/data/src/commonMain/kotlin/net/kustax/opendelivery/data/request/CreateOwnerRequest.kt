package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateOwnerRequest(
    val ownerType: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val vatId: String,
    val contactFirstName: String? = null,
    val contactLastName: String? = null,
    val contactEmail: String,
    val contactPhone: String,
    val secondaryContactName: String? = null,
    val secondaryContactPhone: String? = null,
    val secondaryContactEmail: String? = null,
    val companyActivity: String? = null,
    val address: String? = null,
    val website: String? = null,
    val notes: String? = null,
    val subscriptionPriceCents: Long = 0
)
