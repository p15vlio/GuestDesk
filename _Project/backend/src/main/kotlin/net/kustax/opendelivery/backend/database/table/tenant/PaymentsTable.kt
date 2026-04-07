package net.kustax.opendelivery.backend.database.table.tenant

import org.jetbrains.exposed.v1.core.Table

object PaymentsTable : Table("payments") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36)
    val method = varchar("method", 32)
    val status = varchar("status", 32)
    val amount = long("amount")
    val providerTransactionId = varchar("provider_transaction_id", 255).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
