package net.kustax.opendelivery.backend.repository.tenant

import net.kustax.opendelivery.backend.database.table.tenant.PaymentsTable
import net.kustax.opendelivery.backend.repository.tenantQuery
import net.kustax.opendelivery.domain.entity.tenant.Payment
import net.kustax.opendelivery.domain.enum.PaymentMethod
import net.kustax.opendelivery.domain.enum.PaymentStatus
import net.kustax.opendelivery.domain.repository.PaymentRepository
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

class ExposedPaymentRepository : PaymentRepository {

    override suspend fun create(payment: Payment): Payment = tenantQuery {
        PaymentsTable.insert {
            it[id] = payment.id
            it[orderId] = payment.orderId
            it[method] = payment.method.name
            it[status] = payment.status.name
            it[amount] = payment.amount
            it[providerTransactionId] = payment.providerTransactionId
            it[createdAt] = payment.createdAt
        }
        payment
    }

    override suspend fun findByOrderId(orderId: String): Payment? = tenantQuery {
        PaymentsTable.selectAll()
            .where { PaymentsTable.orderId eq orderId }
            .singleOrNull()
            ?.toPayment()
    }

    override suspend fun updateStatus(id: String, status: PaymentStatus): Payment = tenantQuery {
        PaymentsTable.update({ PaymentsTable.id eq id }) {
            it[PaymentsTable.status] = status.name
        }
        PaymentsTable.selectAll()
            .where { PaymentsTable.id eq id }
            .single()
            .toPayment()
    }
}

private fun ResultRow.toPayment() = Payment(
    id = this[PaymentsTable.id],
    orderId = this[PaymentsTable.orderId],
    method = enumValueOf<PaymentMethod>(this[PaymentsTable.method]),
    status = enumValueOf<PaymentStatus>(this[PaymentsTable.status]),
    amount = this[PaymentsTable.amount],
    providerTransactionId = this[PaymentsTable.providerTransactionId],
    createdAt = this[PaymentsTable.createdAt]
)
