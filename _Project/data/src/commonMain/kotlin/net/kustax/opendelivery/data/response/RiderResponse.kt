package net.kustax.opendelivery.data.response

import kotlinx.serialization.Serializable

@Serializable
data class RiderResponse(
    val id: String,
    val name: String,
    val contactPhone: String,
    val isActive: Boolean,
    val createdAt: Long
)
