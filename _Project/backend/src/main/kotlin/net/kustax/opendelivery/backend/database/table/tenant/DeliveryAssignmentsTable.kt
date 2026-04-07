package net.kustax.opendelivery.backend.database.table.tenant

import org.jetbrains.exposed.v1.core.Table

object DeliveryAssignmentsTable : Table("delivery_assignments") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36)
    val riderId = varchar("rider_id", 36).nullable()
    val storePartnerId = varchar("store_partner_id", 36).nullable()
    val status = varchar("status", 32)
    val assignedAt = long("assigned_at")
    val pickedUpAt = long("picked_up_at").nullable()
    val deliveredAt = long("delivered_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
