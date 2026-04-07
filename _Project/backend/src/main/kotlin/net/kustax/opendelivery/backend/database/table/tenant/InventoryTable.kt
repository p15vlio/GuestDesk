package net.kustax.opendelivery.backend.database.table.tenant

import org.jetbrains.exposed.v1.core.Table

object InventoryTable : Table("inventory") {
    val id = varchar("id", 36)
    val productId = varchar("product_id", 36)
    val propertyId = varchar("property_id", 36)
    val quantity = integer("quantity")
    val priceOverride = long("price_override").nullable()

    override val primaryKey = PrimaryKey(id)
}
