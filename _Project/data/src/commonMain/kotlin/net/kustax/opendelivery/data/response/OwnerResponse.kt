package net.kustax.opendelivery.data.response

import kotlinx.serialization.Serializable

@Serializable
data class OwnerResponse(
    val id: String,
    val ownerType: String,
    val displayName: String,
    val vatId: String,
    val contactEmail: String,
    val contactPhone: String,
    val companyName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val contactFirstName: String? = null,
    val contactLastName: String? = null,
    val secondaryContactName: String? = null,
    val secondaryContactPhone: String? = null,
    val secondaryContactEmail: String? = null,
    val companyActivity: String? = null,
    val address: String? = null,
    val website: String? = null,
    val notes: String? = null,
    val subscriptionPriceCents: Long,
    val subscriptionActiveUntil: Long? = null,
    val schemaName: String,
    val isActive: Boolean,
    val createdAt: Long,
    val temporaryPassword: String? = null
)
