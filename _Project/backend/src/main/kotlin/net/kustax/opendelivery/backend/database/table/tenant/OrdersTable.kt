package net.kustax.opendelivery.backend.database.table.tenant

import org.jetbrains.exposed.v1.core.Table

object OrdersTable : Table("orders") {
    val id = varchar("id", 36)
    val deviceId = varchar("device_id", 36)
    val propertyId = varchar("property_id", 36)
    val guestEmail = varchar("guest_email", 255).nullable()
    val status = varchar("status", 32)
    val type = varchar("type", 32)
    val fulfillmentModel = varchar("fulfillment_model", 32)
    val deliveryProvider = varchar("delivery_provider", 32).nullable()
    val externalPlatform = varchar("external_platform", 32).nullable()
    val partnerStoreId = varchar("partner_store_id", 36).nullable()
    val totalAmount = long("total_amount")
    val notes = varchar("notes", 1024).nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
