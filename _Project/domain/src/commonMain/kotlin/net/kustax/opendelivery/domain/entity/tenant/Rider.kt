package net.kustax.opendelivery.domain.entity.tenant

data class Rider(
    val id: String,
    val name: String,
    val contactPhone: String,
    val isActive: Boolean,
    val createdAt: Long
)
