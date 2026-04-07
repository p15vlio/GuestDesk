package net.kustax.opendelivery.backend.database

import net.kustax.opendelivery.backend.database.table.platform.AuditLogTable
import net.kustax.opendelivery.backend.database.table.platform.OwnersTable
import net.kustax.opendelivery.backend.database.table.platform.PlatformAdminsTable
import net.kustax.opendelivery.backend.database.table.platform.RefreshTokensTable
import net.kustax.opendelivery.backend.database.table.tenant.DeliveryAssignmentsTable
import net.kustax.opendelivery.backend.database.table.tenant.DevicesTable
import net.kustax.opendelivery.backend.database.table.tenant.InventoryTable
import net.kustax.opendelivery.backend.database.table.tenant.OrderItemsTable
import net.kustax.opendelivery.backend.database.table.tenant.OrdersTable
import net.kustax.opendelivery.backend.database.table.tenant.PartnerStoresTable
import net.kustax.opendelivery.backend.database.table.tenant.PaymentsTable
import net.kustax.opendelivery.backend.database.table.tenant.ProductsTable
import net.kustax.opendelivery.backend.database.table.tenant.PropertiesTable
import net.kustax.opendelivery.backend.database.table.tenant.RidersTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object SchemaManager {

    fun createTenantSchema(schemaName: String) {
        transaction {
            exec("CREATE SCHEMA IF NOT EXISTS \"$schemaName\"")
        }
    }

    fun switchToTenant(schemaName: String) {
        transaction {
            exec("SET search_path TO \"$schemaName\"")
        }
    }

    fun createPlatformTables() {
        transaction {
            SchemaUtils.create(OwnersTable, PlatformAdminsTable, RefreshTokensTable, AuditLogTable)
        }
    }

    fun dropAndRecreate() {
        transaction {
            // Drop all tenant schemas first
            exec("""
                DO ${'$'}${'$'}
                DECLARE s TEXT;
                BEGIN
                    FOR s IN SELECT nspname FROM pg_namespace WHERE nspname LIKE 'tenant_%'
                    LOOP
                        EXECUTE 'DROP SCHEMA "' || s || '" CASCADE';
                    END LOOP;
                END
                ${'$'}${'$'};
            """)
            SchemaUtils.drop(AuditLogTable, RefreshTokensTable, PlatformAdminsTable, OwnersTable)
        }
        createPlatformTables()
    }

    fun createTenantTables() {
        transaction {
            SchemaUtils.create(
                PropertiesTable,
                DevicesTable,
                PartnerStoresTable,
                RidersTable,
                ProductsTable,
                InventoryTable,
                OrdersTable,
                OrderItemsTable,
                PaymentsTable,
                DeliveryAssignmentsTable
            )
        }
    }

    fun provisionTenant(schemaName: String) {
        createTenantSchema(schemaName)
        switchToTenant(schemaName)
        createTenantTables()
    }
}
