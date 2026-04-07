package net.kustax.opendelivery.domain.entity.tenant

data class PartnerStore(
    val id: String,
    val name: String,
    val contactEmail: String,
    val contactPhone: String,
    val isPharmacy: Boolean,
    val isActive: Boolean,
    val createdAt: Long
)
