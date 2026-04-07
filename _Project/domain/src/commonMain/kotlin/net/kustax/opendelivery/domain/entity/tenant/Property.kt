package net.kustax.opendelivery.domain.entity.tenant

import net.kustax.opendelivery.domain.enum.FulfillmentModel

data class Property(
    val id: String,
    val name: String,
    val streetName: String,
    val streetNo: String,
    val postalCode: String,
    val area: String?,
    val level: Int,
    val nameOnDoorbell: String,
    val contactPhone: String,
    val city: String,
    val country: String,
    val timezone: String,
    val fulfillmentModel: FulfillmentModel,
    val notes: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isActive: Boolean,
    val createdAt: Long
)
