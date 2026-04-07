package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.Rider

interface RiderRepository {
    suspend fun create(rider: Rider): Rider
    suspend fun findById(id: String): Rider?
    suspend fun findAll(): List<Rider>
    suspend fun update(rider: Rider): Rider
    suspend fun deactivate(id: String)
}
