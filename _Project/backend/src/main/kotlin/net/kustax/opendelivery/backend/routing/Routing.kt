package net.kustax.opendelivery.backend.routing

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import net.kustax.opendelivery.backend.adapter.EmulatedFoodDeliveryAdapter
import net.kustax.opendelivery.backend.adapter.EmulatedLogisticsAdapter
import net.kustax.opendelivery.backend.repository.platform.ExposedAuditLogRepository
import net.kustax.opendelivery.backend.repository.platform.ExposedAuthRepository
import net.kustax.opendelivery.backend.repository.platform.ExposedOwnerRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedDeviceRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderItemRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedProductRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedPropertyRepository
import net.kustax.opendelivery.backend.service.AuditLogService
import net.kustax.opendelivery.backend.service.AuthService
import net.kustax.opendelivery.backend.service.DashboardService
import net.kustax.opendelivery.backend.service.DeviceService
import net.kustax.opendelivery.backend.service.OrderService
import net.kustax.opendelivery.backend.service.OwnerService
import net.kustax.opendelivery.backend.service.ProductService
import net.kustax.opendelivery.backend.service.PropertyService

fun Application.configureRouting() {
    val authRepository = ExposedAuthRepository()
    val authService = AuthService(authRepository)

    val auditLogRepository = ExposedAuditLogRepository()
    val auditLogService = AuditLogService(auditLogRepository)

    val ownerRepository = ExposedOwnerRepository()
    val ownerService = OwnerService(ownerRepository, auditLogService)

    val propertyRepository = ExposedPropertyRepository()
    val propertyService = PropertyService(propertyRepository, auditLogService)

    val deviceRepository = ExposedDeviceRepository()
    val deviceService = DeviceService(deviceRepository, auditLogService)

    val productRepository = ExposedProductRepository()
    val productService = ProductService(productRepository)

    val orderRepository = ExposedOrderRepository()
    val orderItemRepository = ExposedOrderItemRepository()
    val foodDeliveryAdapter = EmulatedFoodDeliveryAdapter()
    val logisticsAdapter = EmulatedLogisticsAdapter()
    val orderService = OrderService(
        orderRepository, orderItemRepository, productRepository,
        foodDeliveryAdapter, logisticsAdapter
    )

    val dashboardService = DashboardService(propertyRepository, deviceRepository, orderRepository)

    routing {
        route("/api") {
            healthRoute()
            authRoutes(authService)
            devicePingRoute(deviceService)
            deviceActivateRoute(deviceService)
            orderRoutes(orderService)
            productRoutes(productService)

            authenticate("jwt") {
                ownerRoutes(ownerService)
                propertyRoutes(propertyService)
                deviceRoutes(deviceService)
                dashboardRoutes(dashboardService)
                adminRoutes(ownerRepository, propertyRepository, deviceRepository, productRepository)
            }
        }
    }
}
