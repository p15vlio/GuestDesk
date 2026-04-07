package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.Property

interface PropertyRepository {
    suspend fun create(property: Property): Property
    suspend fun findById(id: String): Property?
    suspend fun findByOwnerId(ownerId: String): List<Property>
    suspend fun update(property: Property): Property
    suspend fun deactivate(id: String)
    suspend fun delete(id: String)
}
