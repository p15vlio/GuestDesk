package net.kustax.opendelivery.backend.repository.platform

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import net.kustax.opendelivery.backend.database.DatabaseFactory
import net.kustax.opendelivery.backend.database.TenantContext
import net.kustax.opendelivery.backend.database.table.platform.OwnersTable
import net.kustax.opendelivery.backend.database.table.platform.PlatformAdminsTable
import net.kustax.opendelivery.backend.database.table.platform.RefreshTokensTable
import net.kustax.opendelivery.backend.database.table.tenant.PartnerStoresTable
import net.kustax.opendelivery.backend.database.table.tenant.RidersTable
import net.kustax.opendelivery.backend.repository.tenantQuery
import net.kustax.opendelivery.domain.enum.UserRole
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

data class AuthRecord(
    val id: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val schemaName: String?
)

data class RefreshTokenRecord(
    val id: String,
    val userId: String,
    val role: String,
    val tokenHash: String,
    val schemaName: String?,
    val expiresAt: Long,
    val createdAt: Long
)

class ExposedAuthRepository {

    suspend fun findPlatformAdminByEmail(email: String): AuthRecord? =
        DatabaseFactory.dbQuery {
            PlatformAdminsTable.selectAll()
                .where { PlatformAdminsTable.email eq email }
                .singleOrNull()
                ?.let { row ->
                    AuthRecord(
                        id = row[PlatformAdminsTable.id],
                        email = row[PlatformAdminsTable.email],
                        passwordHash = row[PlatformAdminsTable.passwordHash],
                        role = UserRole.PLATFORM_ADMIN,
                        schemaName = null
                    )
                }
        }

    suspend fun findOwnerByEmail(email: String): AuthRecord? =
        DatabaseFactory.dbQuery {
            OwnersTable.selectAll()
                .where { OwnersTable.contactEmail eq email }
                .singleOrNull()
                ?.let { row ->
                    AuthRecord(
                        id = row[OwnersTable.id],
                        email = row[OwnersTable.contactEmail],
                        passwordHash = row[OwnersTable.passwordHash],
                        role = UserRole.OWNER,
                        schemaName = row[OwnersTable.tenantSchemaName]
                    )
                }
        }

    suspend fun findOwnerSchemaByEmail(email: String): String? =
        DatabaseFactory.dbQuery {
            OwnersTable.select(OwnersTable.tenantSchemaName)
                .where { OwnersTable.contactEmail eq email }
                .singleOrNull()
                ?.get(OwnersTable.tenantSchemaName)
        }

    suspend fun findRiderByEmail(email: String, schemaName: String): AuthRecord? =
        withContext(TenantContext(schemaName) + currentCoroutineContext()) {
            tenantQuery {
                RidersTable.selectAll()
                    .where { RidersTable.loginEmail eq email }
                    .singleOrNull()
                    ?.let { row ->
                        AuthRecord(
                            id = row[RidersTable.id],
                            email = row[RidersTable.loginEmail],
                            passwordHash = row[RidersTable.passwordHash],
                            role = UserRole.RIDER,
                            schemaName = schemaName
                        )
                    }
            }
        }

    suspend fun findPartnerStoreByEmail(email: String, schemaName: String): AuthRecord? =
        withContext(TenantContext(schemaName) + currentCoroutineContext()) {
            tenantQuery {
                PartnerStoresTable.selectAll()
                    .where { PartnerStoresTable.contactEmail eq email }
                    .singleOrNull()
                    ?.let { row ->
                        AuthRecord(
                            id = row[PartnerStoresTable.id],
                            email = row[PartnerStoresTable.contactEmail],
                            passwordHash = row[PartnerStoresTable.passwordHash],
                            role = UserRole.STORE_PARTNER,
                            schemaName = schemaName
                        )
                    }
            }
        }

    suspend fun saveRefreshToken(record: RefreshTokenRecord): Unit =
        DatabaseFactory.dbQuery {
            RefreshTokensTable.insert {
                it[id] = record.id
                it[userId] = record.userId
                it[role] = record.role
                it[tokenHash] = record.tokenHash
                it[tenantSchemaName] = record.schemaName
                it[expiresAt] = record.expiresAt
                it[createdAt] = record.createdAt
            }
        }

    suspend fun findRefreshToken(tokenHash: String): RefreshTokenRecord? =
        DatabaseFactory.dbQuery {
            RefreshTokensTable.selectAll()
                .where { RefreshTokensTable.tokenHash eq tokenHash }
                .singleOrNull()
                ?.toRefreshTokenRecord()
        }

    suspend fun deleteRefreshToken(tokenHash: String): Unit =
        DatabaseFactory.dbQuery {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.tokenHash eq tokenHash }
        }
}

private fun ResultRow.toRefreshTokenRecord() = RefreshTokenRecord(
    id = this[RefreshTokensTable.id],
    userId = this[RefreshTokensTable.userId],
    role = this[RefreshTokensTable.role],
    tokenHash = this[RefreshTokensTable.tokenHash],
    schemaName = this[RefreshTokensTable.tenantSchemaName],
    expiresAt = this[RefreshTokensTable.expiresAt],
    createdAt = this[RefreshTokensTable.createdAt]
)
