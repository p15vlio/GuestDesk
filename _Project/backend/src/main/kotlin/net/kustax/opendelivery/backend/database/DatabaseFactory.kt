package net.kustax.opendelivery.backend.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

object DatabaseFactory {

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:postgresql://localhost:5432/opendelivery"
            username = System.getenv("DB_USER") ?: "postgres"
            password = System.getenv("DB_PASSWORD") ?: ""
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        Database.connect(HikariDataSource(config))
    }

    suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
        suspendTransaction {
            // Reset search_path to public before every platform query.
            // HikariCP reuses connections — a previous tenantQuery may have left
            // the path pointing at a tenant schema on this connection.
            exec("SET search_path TO public")
            block()
        }
}
