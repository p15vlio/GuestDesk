package net.kustax.opendelivery.backend.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import net.kustax.opendelivery.backend.repository.tenant.ExposedDeviceRepository
import net.kustax.opendelivery.data.request.CreateDeviceRequest
import net.kustax.opendelivery.data.response.DeviceResponse
import net.kustax.opendelivery.domain.entity.tenant.Device

class DeviceService(
    private val deviceRepository: ExposedDeviceRepository,
    private val auditLogService: AuditLogService
) {

    suspend fun create(request: CreateDeviceRequest, propertyId: String, actorId: String, actorRole: String, schemaName: String): DeviceResponse {
        val id = UUID.randomUUID().toString()
        val activationCode = generateActivationCode()
        val qrContent = """{"deviceId":"$id","activationCode":"$activationCode","schemaName":"$schemaName"}"""
        val qrBase64 = generateQrBase64(qrContent)
        val device = Device(
            id = id,
            propertyId = propertyId,
            name = request.name,
            androidDeviceId = null,
            isActive = false,
            isKioskEnabled = false,
            lastSeenAt = null,
            activationCode = activationCode,
            activatedAt = null
        )
        deviceRepository.create(device)
        auditLogService.log(actorId, actorRole, "CREATE_DEVICE", "Device", id, schemaName)
        return device.toResponse(qrBase64, activationCode)
    }

    suspend fun activate(id: String, activationCode: String, androidDeviceId: String): DeviceResponse {
        val device = deviceRepository.findById(id) ?: throw NotFoundException("Device not found: $id")
        if (device.activatedAt != null) throw ConflictException("Device is already activated")
        if (device.activationCode != activationCode) throw ForbiddenException("Invalid activation code")
        val now = System.currentTimeMillis()
        deviceRepository.activate(id, androidDeviceId, now)
        return device.copy(androidDeviceId = androidDeviceId, activatedAt = now, activationCode = null, isActive = true).toResponse()
    }

    suspend fun findByPropertyId(propertyId: String): List<DeviceResponse> =
        deviceRepository.findByPropertyId(propertyId).map { it.toResponse() }

    suspend fun findById(id: String): DeviceResponse {
        val device = deviceRepository.findById(id) ?: throw NotFoundException("Device not found: $id")
        return device.toResponse()
    }

    suspend fun deactivate(id: String, actorId: String, actorRole: String, schemaName: String) {
        deviceRepository.findById(id) ?: throw NotFoundException("Device not found: $id")
        deviceRepository.deactivate(id)
        auditLogService.log(actorId, actorRole, "DEACTIVATE_DEVICE", "Device", id, schemaName)
    }

    suspend fun delete(id: String, actorId: String, actorRole: String, schemaName: String) {
        deviceRepository.findById(id) ?: throw NotFoundException("Device not found: $id")
        deviceRepository.delete(id)
        auditLogService.log(actorId, actorRole, "DELETE_DEVICE", "Device", id, schemaName)
    }

    suspend fun updateLastSeen(id: String) {
        deviceRepository.updateLastSeen(id, System.currentTimeMillis())
    }

    private fun generateActivationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }

    private fun generateQrBase64(content: String): String {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 256, 256)
        val bos = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(matrix, "PNG", bos)
        return Base64.getEncoder().encodeToString(bos.toByteArray())
    }
}

// activationCode is only passed from create() — never exposed on subsequent reads
private fun Device.toResponse(qrBase64: String? = null, activationCode: String? = null) = DeviceResponse(
    id = id,
    propertyId = propertyId,
    name = name,
    androidDeviceId = androidDeviceId,
    isActive = isActive,
    isKioskEnabled = isKioskEnabled,
    lastSeenAt = lastSeenAt,
    activationCode = activationCode,
    activatedAt = activatedAt,
    qrCodeBase64 = qrBase64
)
