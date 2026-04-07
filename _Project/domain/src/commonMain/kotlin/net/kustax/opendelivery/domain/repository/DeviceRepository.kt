package net.kustax.opendelivery.domain.repository

import net.kustax.opendelivery.domain.entity.tenant.Device

interface DeviceRepository {
    suspend fun create(device: Device): Device
    suspend fun findById(id: String): Device?
    suspend fun findByPropertyId(propertyId: String): List<Device>
    suspend fun updateLastSeen(id: String, timestamp: Long)
    suspend fun deactivate(id: String)
    suspend fun delete(id: String)
}
