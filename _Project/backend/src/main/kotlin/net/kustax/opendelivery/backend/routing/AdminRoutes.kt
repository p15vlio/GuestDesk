package net.kustax.opendelivery.backend.routing

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.kustax.opendelivery.backend.repository.platform.ExposedOwnerRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedDeviceRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedProductRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedPropertyRepository
import net.kustax.opendelivery.backend.service.ForbiddenException
import net.kustax.opendelivery.backend.service.toOwnerResponse
import net.kustax.opendelivery.data.response.OwnerWithStatsResponse
import net.kustax.opendelivery.domain.enum.UserRole

fun Route.adminRoutes(
    ownerRepository: ExposedOwnerRepository,
    propertyRepository: ExposedPropertyRepository,
    deviceRepository: ExposedDeviceRepository,
    productRepository: ExposedProductRepository
) {
    authenticate("jwt") {
        get("/admin/owners/stats") {
            if (call.userRole() != UserRole.PLATFORM_ADMIN) throw ForbiddenException("Platform admin access required")
            
            val stats = ownerRepository.findAllWithStats()
            val result = stats.map { stat ->
                OwnerWithStatsResponse(
                    owner = stat.owner.toOwnerResponse(),
                    propertyCount = stat.propertyCount,
                    deviceCount = stat.deviceCount,
                    productCount = stat.productCount
                )
            }
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
