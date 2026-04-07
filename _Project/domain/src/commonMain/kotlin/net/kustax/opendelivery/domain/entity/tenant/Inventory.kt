package net.kustax.opendelivery.domain.entity.tenant

// priceOverride = null means the product's basePrice applies
data class Inventory(
    val id: String,
    val productId: String,
    val propertyId: String,
    val quantity: Int,
    val priceOverride: Long?
)
