package net.kustax.opendelivery.data.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateRiderRequest(
    val name: String,
    val contactPhone: String
)
