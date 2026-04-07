package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.platform.Owner

interface OwnerRepository {
    suspend fun create(owner: Owner): Owner
    suspend fun findById(id: String): Owner?
    suspend fun findAll(): List<Owner>
    suspend fun update(owner: Owner): Owner
    suspend fun deactivate(id: String)
    suspend fun delete(id: String)
}
