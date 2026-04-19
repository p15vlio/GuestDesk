package net.kustax.opendelivery.backend.service

import net.kustax.opendelivery.data.response.OwnerResponse
import net.kustax.opendelivery.domain.entity.platform.Owner
import net.kustax.opendelivery.domain.enum.OwnerType

internal fun Owner.toOwnerResponse(tempPassword: String? = null) = OwnerResponse(
    id = id,
    ownerType = ownerType.name,
    displayName = if (ownerType == OwnerType.COMPANY) companyName ?: contactEmail
                  else "${firstName.orEmpty()} ${lastName.orEmpty()}".trim().ifEmpty { contactEmail },
    vatId = vatId,
    contactEmail = contactEmail,
    contactPhone = contactPhone,
    companyName = companyName,
    firstName = firstName,
    lastName = lastName,
    contactFirstName = contactFirstName,
    contactLastName = contactLastName,
    secondaryContactName = secondaryContactName,
    secondaryContactPhone = secondaryContactPhone,
    secondaryContactEmail = secondaryContactEmail,
    companyActivity = companyActivity,
    address = address,
    website = website,
    notes = notes,
    subscriptionPriceCents = subscriptionPriceCents,
    subscriptionActiveUntil = subscriptionActiveUntil,
    schemaName = schemaName,
    isActive = isActive,
    createdAt = createdAt,
    temporaryPassword = tempPassword
)
