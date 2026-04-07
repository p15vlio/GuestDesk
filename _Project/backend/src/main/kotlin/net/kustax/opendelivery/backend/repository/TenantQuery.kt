package net.kustax.opendelivery.backend.repository

import kotlinx.coroutines.currentCoroutineContext
import net.kustax.opendelivery.backend.database.TenantContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

suspend fun <T> tenantQuery(block: suspend Transaction.() -> T): T {
    val schema = currentCoroutineContext()[TenantContext]?.schemaName
        ?: error("TenantContext not set in coroutine context")
    return suspendTransaction {
        exec("SET search_path TO \"$schema\"")
        block()
    }
}
