package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.PartnerStore

interface PartnerStoreRepository {
    suspend fun create(store: PartnerStore): PartnerStore
    suspend fun findById(id: String): PartnerStore?
    suspend fun findAll(): List<PartnerStore>
    suspend fun update(store: PartnerStore): PartnerStore
    suspend fun deactivate(id: String)
}
