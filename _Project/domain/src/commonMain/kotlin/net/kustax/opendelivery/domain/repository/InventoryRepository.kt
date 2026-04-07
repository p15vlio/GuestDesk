package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.Inventory

interface InventoryRepository {
    suspend fun findByProductAndProperty(productId: String, propertyId: String): Inventory?
    suspend fun update(inventory: Inventory): Inventory
    suspend fun decreaseQuantity(id: String, amount: Int)
}
