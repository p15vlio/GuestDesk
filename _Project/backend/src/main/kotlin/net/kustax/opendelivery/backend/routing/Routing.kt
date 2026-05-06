package net.kustax.opendelivery.backend.routing

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import net.kustax.opendelivery.backend.adapter.EmulatedEmailAdapter
import net.kustax.opendelivery.backend.adapter.EmulatedFoodDeliveryAdapter
import net.kustax.opendelivery.backend.adapter.EmulatedLogisticsAdapter
import net.kustax.opendelivery.backend.adapter.EmulatedPaymentAdapter
import net.kustax.opendelivery.backend.repository.platform.ExposedAuditLogRepository
import net.kustax.opendelivery.backend.repository.platform.ExposedAuthRepository
import net.kustax.opendelivery.backend.repository.platform.ExposedOwnerRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedDeviceRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderItemRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedOrderRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedPaymentRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedProductRepository
import net.kustax.opendelivery.backend.repository.tenant.ExposedPropertyRepository
import net.kustax.opendelivery.backend.service.AuditLogService
import net.kustax.opendelivery.backend.service.AuthService
import net.kustax.opendelivery.backend.service.DashboardService
import net.kustax.opendelivery.backend.service.DeviceService
import net.kustax.opendelivery.backend.service.OrderService
import net.kustax.opendelivery.backend.service.OwnerService
import net.kustax.opendelivery.backend.service.PaymentService
import net.kustax.opendelivery.backend.service.ProductService
import net.kustax.opendelivery.backend.service.PropertyService

fun Application.configureRouting() {
    val auditLogRepository = ExposedAuditLogRepository()
    val auditLogService = AuditLogService(auditLogRepository)

    val authRepository = ExposedAuthRepository()
    val authService = AuthService(authRepository)

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
    val paymentRepository = ExposedPaymentRepository()

    val foodDeliveryAdapter = EmulatedFoodDeliveryAdapter()
    val logisticsAdapter = EmulatedLogisticsAdapter()
    val paymentAdapter = EmulatedPaymentAdapter()
    val emailAdapter = EmulatedEmailAdapter()

    val orderService = OrderService(orderRepository, orderItemRepository, productRepository, foodDeliveryAdapter, logisticsAdapter)
    val paymentService = PaymentService(paymentRepository, orderRepository, orderItemRepository, paymentAdapter, emailAdapter)

    val dashboardService = DashboardService(propertyRepository, deviceRepository, orderRepository)

    routing {
        route("/api") {
            healthRoute()
            authRoutes(authService)
            deviceActivateRoute(deviceService)
            deviceOrderRoutes(orderService)

            authenticate("jwt") {
                devicePingRoute(deviceService)
                orderRoutes(orderService)
                paymentRoutes(paymentService)
                productRoutes(productService)

                ownerRoutes(ownerService)
                propertyRoutes(propertyService)
                deviceRoutes(deviceService)
                dashboardRoutes(dashboardService)
                auditLogRoutes(auditLogRepository)
                adminRoutes(ownerRepository, propertyRepository, deviceRepository, productRepository)
            }
        }
    }
}
