package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateOwnerRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val contactFirstName: String? = null,
    val contactLastName: String? = null,
    val contactPhone: String,
    val secondaryContactName: String? = null,
    val secondaryContactPhone: String? = null,
    val secondaryContactEmail: String? = null,
    val companyActivity: String? = null,
    val address: String? = null,
    val website: String? = null,
    val notes: String? = null,
    val subscriptionPriceCents: Long = 0,
    val subscriptionActiveUntil: Long? = null
)
