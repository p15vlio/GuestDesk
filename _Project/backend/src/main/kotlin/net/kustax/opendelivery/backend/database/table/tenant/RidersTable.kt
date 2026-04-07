package net.kustax.opendelivery.backend.database.table.tenant

import org.jetbrains.exposed.v1.core.Table

object RidersTable : Table("riders") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val loginEmail = varchar("login_email", 255)
    val passwordHash = varchar("password_hash", 255)
    val contactPhone = varchar("contact_phone", 64)
    val isActive = bool("is_active")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
