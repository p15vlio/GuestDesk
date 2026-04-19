package net.kustax.opendelivery.backend.service

import java.util.UUID
import net.kustax.opendelivery.backend.database.SchemaManager
import net.kustax.opendelivery.backend.repository.platform.ExposedOwnerRepository
import net.kustax.opendelivery.backend.security.PasswordHasher
import net.kustax.opendelivery.data.request.CreateOwnerRequest
import net.kustax.opendelivery.data.request.UpdateOwnerRequest
import net.kustax.opendelivery.data.response.OwnerResponse
import net.kustax.opendelivery.domain.entity.platform.Owner
import net.kustax.opendelivery.domain.enum.OwnerType

class OwnerService(
    private val ownerRepository: ExposedOwnerRepository,
    private val auditLogService: AuditLogService
) {

    suspend fun create(request: CreateOwnerRequest, actorId: String, actorRole: String): OwnerResponse {
        if (ownerRepository.findByEmail(request.contactEmail) != null) {
            throw ConflictException("An owner with email '${request.contactEmail}' already exists")
        }
        val id = UUID.randomUUID().toString()
        val schemaName = "tenant_${id.replace("-", "").take(12)}"
        val tempPassword = generateTempPassword()
        val owner = Owner(
            id = id,
            ownerType = enumValueOf<OwnerType>(request.ownerType),
            firstName = request.firstName,
            lastName = request.lastName,
            companyName = request.companyName,
            vatId = request.vatId,
            contactFirstName = request.contactFirstName,
            contactLastName = request.contactLastName,
            contactEmail = request.contactEmail,
            contactPhone = request.contactPhone,
            secondaryContactName = request.secondaryContactName,
            secondaryContactPhone = request.secondaryContactPhone,
            secondaryContactEmail = request.secondaryContactEmail,
            companyActivity = request.companyActivity,
            address = request.address,
            website = request.website,
            notes = request.notes,
            subscriptionPriceCents = request.subscriptionPriceCents,
            subscriptionActiveUntil = null,
            schemaName = schemaName,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        ownerRepository.createWithHash(owner, PasswordHasher.hash(tempPassword))
        SchemaManager.provisionTenant(schemaName)
        auditLogService.log(actorId, actorRole, "CREATE_OWNER", "Owner", id)
        return owner.toResponse(tempPassword)
    }

    suspend fun findAll(): List<OwnerResponse> =
        ownerRepository.findAll().map { it.toResponse() }

    suspend fun findById(id: String): OwnerResponse {
        val owner = ownerRepository.findById(id) ?: throw NotFoundException("Owner not found: $id")
        return owner.toResponse()
    }

    suspend fun update(id: String, request: UpdateOwnerRequest, actorId: String, actorRole: String): OwnerResponse {
        val existing = ownerRepository.findById(id) ?: throw NotFoundException("Owner not found: $id")
        val updated = existing.copy(
            firstName = request.firstName,
            lastName = request.lastName,
            companyName = request.companyName,
            contactFirstName = request.contactFirstName,
            contactLastName = request.contactLastName,
            contactPhone = request.contactPhone,
            secondaryContactName = request.secondaryContactName,
            secondaryContactPhone = request.secondaryContactPhone,
            secondaryContactEmail = request.secondaryContactEmail,
            companyActivity = request.companyActivity,
            address = request.address,
            website = request.website,
            notes = request.notes,
            subscriptionPriceCents = request.subscriptionPriceCents,
            subscriptionActiveUntil = request.subscriptionActiveUntil
        )
        ownerRepository.update(updated)
        auditLogService.log(actorId, actorRole, "UPDATE_OWNER", "Owner", id)
        return updated.toResponse()
    }

    suspend fun deactivate(id: String, actorId: String, actorRole: String) {
        ownerRepository.findById(id) ?: throw NotFoundException("Owner not found: $id")
        ownerRepository.deactivate(id)
        auditLogService.log(actorId, actorRole, "DEACTIVATE_OWNER", "Owner", id)
    }

    suspend fun delete(id: String, actorId: String, actorRole: String) {
        ownerRepository.findById(id) ?: throw NotFoundException("Owner not found: $id")
        ownerRepository.delete(id)
        auditLogService.log(actorId, actorRole, "DELETE_OWNER", "Owner", id)
    }

    private fun generateTempPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..12).map { chars.random() }.joinToString("")
    }
}

private fun Owner.toResponse(tempPassword: String? = null) = toOwnerResponse(tempPassword)
