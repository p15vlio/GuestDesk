package net.kustax.opendelivery.backend.service

import java.util.UUID
import net.kustax.opendelivery.backend.repository.tenant.ExposedPropertyRepository
import net.kustax.opendelivery.data.request.CreatePropertyRequest
import net.kustax.opendelivery.data.request.UpdatePropertyRequest
import net.kustax.opendelivery.data.response.PropertyResponse
import net.kustax.opendelivery.domain.entity.tenant.Property
import net.kustax.opendelivery.domain.enum.FulfillmentModel

class PropertyService(
    private val propertyRepository: ExposedPropertyRepository,
    private val auditLogService: AuditLogService
) {

    suspend fun create(request: CreatePropertyRequest, actorId: String, actorRole: String, schemaName: String): PropertyResponse {
        val property = Property(
            id = UUID.randomUUID().toString(),
            name = request.name,
            streetName = request.streetName,
            streetNo = request.streetNo,
            postalCode = request.postalCode,
            area = request.area,
            level = request.level,
            nameOnDoorbell = request.nameOnDoorbell,
            contactPhone = request.contactPhone,
            city = "Corfu",
            country = "Greece",
            timezone = "Europe/Athens",
            fulfillmentModel = enumValueOf<FulfillmentModel>(request.fulfillmentModel),
            notes = request.notes,
            latitude = null,
            longitude = null,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        propertyRepository.create(property)
        auditLogService.log(actorId, actorRole, "CREATE_PROPERTY", "Property", property.id, schemaName)
        return property.toResponse()
    }

    // ownerId is unused in the tenant-scoped repo — the entire schema is the owner's scope
    suspend fun findAll(): List<PropertyResponse> =
        propertyRepository.findByOwnerId("").map { it.toResponse() }

    suspend fun findById(id: String): PropertyResponse {
        val property = propertyRepository.findById(id)
            ?: throw NotFoundException("Property not found: $id")
        return property.toResponse()
    }

    suspend fun update(id: String, request: UpdatePropertyRequest, actorId: String, actorRole: String, schemaName: String): PropertyResponse {
        val existing = propertyRepository.findById(id)
            ?: throw NotFoundException("Property not found: $id")
        val updated = existing.copy(
            name = request.name,
            streetName = request.streetName,
            streetNo = request.streetNo,
            postalCode = request.postalCode,
            area = request.area,
            level = request.level,
            nameOnDoorbell = request.nameOnDoorbell,
            contactPhone = request.contactPhone,
            fulfillmentModel = enumValueOf<FulfillmentModel>(request.fulfillmentModel),
            notes = request.notes
        )
        propertyRepository.update(updated)
        auditLogService.log(actorId, actorRole, "UPDATE_PROPERTY", "Property", id, schemaName)
        return updated.toResponse()
    }

    suspend fun deactivate(id: String, actorId: String, actorRole: String, schemaName: String) {
        propertyRepository.findById(id) ?: throw NotFoundException("Property not found: $id")
        propertyRepository.deactivate(id)
        auditLogService.log(actorId, actorRole, "DEACTIVATE_PROPERTY", "Property", id, schemaName)
    }

    suspend fun delete(id: String, actorId: String, actorRole: String, schemaName: String) {
        propertyRepository.findById(id) ?: throw NotFoundException("Property not found: $id")
        propertyRepository.delete(id)
        auditLogService.log(actorId, actorRole, "DELETE_PROPERTY", "Property", id, schemaName)
    }
}

private fun Property.toResponse() = PropertyResponse(
    id = id,
    name = name,
    streetName = streetName,
    streetNo = streetNo,
    postalCode = postalCode,
    area = area,
    level = level,
    nameOnDoorbell = nameOnDoorbell,
    contactPhone = contactPhone,
    city = city,
    country = country,
    timezone = timezone,
    fulfillmentModel = fulfillmentModel.name,
    notes = notes,
    latitude = latitude,
    longitude = longitude,
    isActive = isActive,
    createdAt = createdAt
)
