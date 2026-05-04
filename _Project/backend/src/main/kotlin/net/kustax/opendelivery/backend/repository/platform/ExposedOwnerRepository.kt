package net.kustax.opendelivery.backend.repository.platform

import net.kustax.opendelivery.backend.database.DatabaseFactory
import net.kustax.opendelivery.backend.database.table.platform.OwnersTable
import net.kustax.opendelivery.domain.entity.platform.Owner
import net.kustax.opendelivery.domain.enum.OwnerType
import net.kustax.opendelivery.domain.repository.OwnerRepository
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

class ExposedOwnerRepository : OwnerRepository {

    override suspend fun create(owner: Owner): Owner = DatabaseFactory.dbQuery {
        OwnersTable.insert { row ->
            row[id] = owner.id
            row[ownerType] = owner.ownerType.name
            row[firstName] = owner.firstName
            row[lastName] = owner.lastName
            row[companyName] = owner.companyName
            row[vatId] = owner.vatId
            row[contactFirstName] = owner.contactFirstName
            row[contactLastName] = owner.contactLastName
            row[contactEmail] = owner.contactEmail
            row[passwordHash] = ""
            row[contactPhone] = owner.contactPhone
            row[secondaryContactName] = owner.secondaryContactName
            row[secondaryContactPhone] = owner.secondaryContactPhone
            row[secondaryContactEmail] = owner.secondaryContactEmail
            row[companyActivity] = owner.companyActivity
            row[address] = owner.address
            row[website] = owner.website
            row[notes] = owner.notes
            row[subscriptionPriceCents] = owner.subscriptionPriceCents
            row[subscriptionActiveUntil] = owner.subscriptionActiveUntil
            row[tenantSchemaName] = owner.schemaName
            row[isActive] = owner.isActive
            row[createdAt] = owner.createdAt
        }
        owner
    }

    // Backend-internal: inserts owner and password hash in a single statement
    suspend fun createWithHash(owner: Owner, hash: String): Owner = DatabaseFactory.dbQuery {
        OwnersTable.insert { row ->
            row[id] = owner.id
            row[ownerType] = owner.ownerType.name
            row[firstName] = owner.firstName
            row[lastName] = owner.lastName
            row[companyName] = owner.companyName
            row[vatId] = owner.vatId
            row[contactFirstName] = owner.contactFirstName
            row[contactLastName] = owner.contactLastName
            row[contactEmail] = owner.contactEmail
            row[passwordHash] = hash
            row[contactPhone] = owner.contactPhone
            row[secondaryContactName] = owner.secondaryContactName
            row[secondaryContactPhone] = owner.secondaryContactPhone
            row[secondaryContactEmail] = owner.secondaryContactEmail
            row[companyActivity] = owner.companyActivity
            row[address] = owner.address
            row[website] = owner.website
            row[notes] = owner.notes
            row[subscriptionPriceCents] = owner.subscriptionPriceCents
            row[subscriptionActiveUntil] = owner.subscriptionActiveUntil
            row[tenantSchemaName] = owner.schemaName
            row[isActive] = owner.isActive
            row[createdAt] = owner.createdAt
        }
        owner
    }

    override suspend fun findById(id: String): Owner? = DatabaseFactory.dbQuery {
        OwnersTable.selectAll()
            .where { OwnersTable.id eq id }
            .singleOrNull()
            ?.toOwner()
    }

    suspend fun findByEmail(email: String): Owner? = DatabaseFactory.dbQuery {
        OwnersTable.selectAll()
            .where { OwnersTable.contactEmail eq email }
            .singleOrNull()
            ?.toOwner()
    }

    override suspend fun findAll(): List<Owner> = DatabaseFactory.dbQuery {
        OwnersTable.selectAll().map { it.toOwner() }
    }

    override suspend fun update(owner: Owner): Owner = DatabaseFactory.dbQuery {
        OwnersTable.update({ OwnersTable.id eq owner.id }) { row ->
            row[firstName] = owner.firstName
            row[lastName] = owner.lastName
            row[companyName] = owner.companyName
            row[contactFirstName] = owner.contactFirstName
            row[contactLastName] = owner.contactLastName
            row[contactPhone] = owner.contactPhone
            row[secondaryContactName] = owner.secondaryContactName
            row[secondaryContactPhone] = owner.secondaryContactPhone
            row[secondaryContactEmail] = owner.secondaryContactEmail
            row[companyActivity] = owner.companyActivity
            row[address] = owner.address
            row[website] = owner.website
            row[notes] = owner.notes
            row[subscriptionPriceCents] = owner.subscriptionPriceCents
            row[subscriptionActiveUntil] = owner.subscriptionActiveUntil
            row[isActive] = owner.isActive
        }
        owner
    }

    override suspend fun deactivate(id: String): Unit = DatabaseFactory.dbQuery {
        OwnersTable.update({ OwnersTable.id eq id }) { it[isActive] = false }
    }

    override suspend fun delete(id: String): Unit = DatabaseFactory.dbQuery {
        OwnersTable.deleteWhere { OwnersTable.id eq id }
    }

    suspend fun setPasswordHash(ownerId: String, hash: String): Unit = DatabaseFactory.dbQuery {
        OwnersTable.update({ OwnersTable.id eq ownerId }) { it[passwordHash] = hash }
    }

    suspend fun findAllWithStats(): List<OwnerStats> = DatabaseFactory.dbQuery {
        OwnersTable.selectAll().map {
            OwnerStats(
                owner = it.toOwner(),
                propertyCount = 0, // Simplified for now
                deviceCount = 0,
                productCount = 0
            )
        }
    }
}

data class OwnerStats(
    val owner: Owner,
    val propertyCount: Int,
    val deviceCount: Int,
    val productCount: Int
)

private fun ResultRow.toOwner() = Owner(
    id = this[OwnersTable.id],
    ownerType = enumValueOf<OwnerType>(this[OwnersTable.ownerType]),
    firstName = this[OwnersTable.firstName],
    lastName = this[OwnersTable.lastName],
    companyName = this[OwnersTable.companyName],
    vatId = this[OwnersTable.vatId],
    contactFirstName = this[OwnersTable.contactFirstName],
    contactLastName = this[OwnersTable.contactLastName],
    contactEmail = this[OwnersTable.contactEmail],
    contactPhone = this[OwnersTable.contactPhone],
    secondaryContactName = this[OwnersTable.secondaryContactName],
    secondaryContactPhone = this[OwnersTable.secondaryContactPhone],
    secondaryContactEmail = this[OwnersTable.secondaryContactEmail],
    companyActivity = this[OwnersTable.companyActivity],
    address = this[OwnersTable.address],
    website = this[OwnersTable.website],
    notes = this[OwnersTable.notes],
    subscriptionPriceCents = this[OwnersTable.subscriptionPriceCents],
    subscriptionActiveUntil = this[OwnersTable.subscriptionActiveUntil],
    schemaName = this[OwnersTable.tenantSchemaName],
    isActive = this[OwnersTable.isActive],
    createdAt = this[OwnersTable.createdAt]
)
