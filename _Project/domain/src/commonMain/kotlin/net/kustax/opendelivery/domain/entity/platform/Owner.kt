package net.kustax.opendelivery.domain.entity.platform

import net.kustax.opendelivery.domain.enum.OwnerType

data class Owner(
    val id: String,
    val ownerType: OwnerType,
    val firstName: String?,
    val lastName: String?,
    val companyName: String?,
    val vatId: String,
    val contactFirstName: String?,
    val contactLastName: String?,
    val contactEmail: String,
    val contactPhone: String,
    val secondaryContactName: String?,
    val secondaryContactPhone: String?,
    val secondaryContactEmail: String?,
    val companyActivity: String?,
    val address: String?,
    val website: String?,
    val notes: String?,
    val subscriptionPriceCents: Long,
    val subscriptionActiveUntil: Long?,
    val schemaName: String,
    val isActive: Boolean,
    val createdAt: Long
)
