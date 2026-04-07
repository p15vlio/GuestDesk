package net.kustax.opendelivery.backend.database.table.platform

import org.jetbrains.exposed.v1.core.Table

object OwnersTable : Table("owners") {
    val id = varchar("id", 36)
    val ownerType = varchar("owner_type", 16)
    val firstName = varchar("first_name", 128).nullable()
    val lastName = varchar("last_name", 128).nullable()
    val companyName = varchar("company_name", 255).nullable()
    val vatId = varchar("vat_id", 64)
    val contactFirstName = varchar("contact_first_name", 128).nullable()
    val contactLastName = varchar("contact_last_name", 128).nullable()
    val contactEmail = varchar("contact_email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val contactPhone = varchar("contact_phone", 64)
    val secondaryContactName = varchar("secondary_contact_name", 255).nullable()
    val secondaryContactPhone = varchar("secondary_contact_phone", 64).nullable()
    val secondaryContactEmail = varchar("secondary_contact_email", 255).nullable()
    val companyActivity = varchar("company_activity", 255).nullable()
    val address = varchar("address", 512).nullable()
    val website = varchar("website", 255).nullable()
    val notes = text("notes").nullable()
    val subscriptionPriceCents = long("subscription_price_cents").default(0L)
    val subscriptionActiveUntil = long("subscription_active_until").nullable()
    val tenantSchemaName = varchar("schema_name", 128)
    val isActive = bool("is_active")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
