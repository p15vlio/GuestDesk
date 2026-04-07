package net.kustax.opendelivery.backend.database.table.tenant

import org.jetbrains.exposed.v1.core.Table

object OrderItemsTable : Table("order_items") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36)
    val productId = varchar("product_id", 36)
    val productName = varchar("product_name", 255)
    val unitPrice = long("unit_price")
    val quantity = integer("quantity")
    val subtotal = long("subtotal")

    override val primaryKey = PrimaryKey(id)
}
