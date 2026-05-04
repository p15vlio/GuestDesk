package net.kustax.opendelivery.web.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import net.kustax.opendelivery.data.request.*
import net.kustax.opendelivery.data.response.*
import net.kustax.opendelivery.web.auth.AuthSession
import net.kustax.opendelivery.web.auth.SessionController

class ApiClient(private val baseUrl: String, token: String) {

    companion object {
        const val BASE_URL = ""

        fun unauthenticated(): ApiClient = ApiClient(BASE_URL, "")
    }

    // Mutable so the token refresh logic can update it without rebuilding the client.
    private var currentToken: String = token

    private val client = HttpClient(Js) {
        install(ContentNegotiation) {
            json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                val responseException = exception as? ResponseException ?: return@handleResponseExceptionWithRequest
                // Let 401 propagate as ResponseException so withTokenRefresh can detect it.
                if (responseException.response.status == HttpStatusCode.Unauthorized) throw responseException
                val message = try {
                    responseException.response.body<ErrorResponse>().message
                } catch (_: Exception) {
                    "Request failed (${responseException.response.status.value})"
                }
                throw Exception(message)
            }
        }
    }

    private suspend fun <T> withTokenRefresh(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: ResponseException) {
            if (e.response.status != HttpStatusCode.Unauthorized) throw e
            refreshAndRetry(e, block)
        }
    }

    private suspend fun <T> refreshAndRetry(originalException: ResponseException, block: suspend () -> T): T {
        val session = AuthSession.load() ?: run {
            SessionController.markExpired()
            throw originalException
        }
        return try {
            val refreshResponse = client.post("$baseUrl/api/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(session.refreshToken))
            }
            val newTokens = refreshResponse.body<TokenResponse>()
            val newSession = session.copy(
                accessToken = newTokens.accessToken,
                refreshToken = newTokens.refreshToken
            )
            newSession.save()
            currentToken = newTokens.accessToken
            block()
        } catch (_: Exception) {
            SessionController.markExpired()
            throw originalException
        }
    }

    private suspend inline fun <reified T> get(path: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): T =
        withTokenRefresh {
            client.get("$baseUrl$path") {
                bearerAuth(currentToken)
                block()
            }.body()
        }

    private suspend inline fun <reified T> post(path: String, body: Any): T =
        withTokenRefresh {
            client.post("$baseUrl$path") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }

    private suspend inline fun <reified T> put(path: String, body: Any): T =
        withTokenRefresh {
            client.put("$baseUrl$path") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }

    private suspend inline fun <reified T> patch(path: String, body: Any): T =
        withTokenRefresh {
            client.patch("$baseUrl$path") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }

    private suspend fun delete(path: String) {
        withTokenRefresh {
            client.delete("$baseUrl$path") { bearerAuth(currentToken) }
        }
    }

    // Unauthenticated — bypasses withTokenRefresh entirely.
    suspend fun login(request: LoginRequest): TokenResponse {
        val response = client.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val message = try {
                response.body<ErrorResponse>().message
            } catch (_: Exception) {
                "Login failed (${response.status.value})"
            }
            throw Exception(message)
        }
        return response.body()
    }

    // Owners — PLATFORM_ADMIN
    suspend fun listOwners(): List<OwnerResponse> = get("/api/owners")
    suspend fun createOwner(request: CreateOwnerRequest): OwnerResponse = post("/api/owners", request)
    suspend fun updateOwner(id: String, request: UpdateOwnerRequest): OwnerResponse = put("/api/owners/$id", request)

    // Properties — OWNER
    suspend fun listProperties(): List<PropertyResponse> = get("/api/properties")
    suspend fun createProperty(request: CreatePropertyRequest): PropertyResponse = post("/api/properties", request)
    suspend fun updateProperty(id: String, request: UpdatePropertyRequest): PropertyResponse = put("/api/properties/$id", request)

    // Devices — OWNER
    suspend fun listDevices(propertyId: String): List<DeviceResponse> = get("/api/properties/$propertyId/devices")
    suspend fun createDevice(propertyId: String, request: CreateDeviceRequest): DeviceResponse = post("/api/properties/$propertyId/devices", request)

    // Products — OWNER
    suspend fun listProducts(): List<ProductResponse> = get("/api/products")
    suspend fun createProduct(request: CreateProductRequest): ProductResponse = post("/api/products", request)
    suspend fun updateProduct(id: String, request: UpdateProductRequest): ProductResponse = put("/api/products/$id", request)

    // Dedicated patch for availability — must supply all required UpdateProductRequest fields
    suspend fun toggleAvailability(product: ProductResponse, isAvailable: Boolean): ProductResponse =
        patch(
            "/api/products/${product.id}/availability",
            UpdateProductRequest(
                name = product.name,
                description = product.description,
                basePrice = product.basePrice,
                imageUrl = product.imageUrl,
                isAvailable = isAvailable
            )
        )

    // Dashboard — OWNER
    suspend fun getDashboardSummary(): DashboardSummaryResponse = get("/api/dashboard/summary")

    // Orders — OWNER; propertyId is always required by the backend
    suspend fun listOrders(propertyId: String, status: String? = null): List<OrderResponse> =
        get("/api/orders") {
            parameter("propertyId", propertyId)
            status?.let { parameter("status", it) }
        }

    suspend fun getOrder(id: String): OrderResponse = get("/api/orders/$id")

    suspend fun updateOrderStatus(id: String, status: String): OrderResponse =
        patch("/api/orders/$id/status", UpdateOrderStatusRequest(status))

    // Admin — PLATFORM_ADMIN
    suspend fun getOwnerStats(): List<OwnerWithStatsResponse> = get("/api/admin/owners/stats")
    suspend fun listAuditLogs(limit: Int, offset: Long): List<AuditLogResponse> =
        get("/api/audit-logs") {
            parameter("limit", limit)
            parameter("offset", offset)
        }

    // Admin cross-tenant reads — schemaName passed as query param
    suspend fun listPropertiesAdmin(schemaName: String): List<PropertyResponse> =
        get("/api/properties") { parameter("schemaName", schemaName) }

    suspend fun listDevicesAdmin(propertyId: String, schemaName: String): List<DeviceResponse> =
        get("/api/properties/$propertyId/devices") { parameter("schemaName", schemaName) }

    suspend fun listProductsAdmin(schemaName: String): List<ProductResponse> =
        get("/api/products") { parameter("schemaName", schemaName) }

    // Admin cross-tenant mutations — schemaName passed as query param
    suspend fun createPropertyAdmin(schemaName: String, request: CreatePropertyRequest): PropertyResponse =
        withTokenRefresh {
            client.post("$baseUrl/api/properties?schemaName=$schemaName") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    suspend fun createProductAdmin(schemaName: String, request: CreateProductRequest): ProductResponse =
        withTokenRefresh {
            client.post("$baseUrl/api/products") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                parameter("schemaName", schemaName)
                setBody(request)
            }.body()
        }

    suspend fun createDeviceAdmin(propertyId: String, schemaName: String, request: CreateDeviceRequest): DeviceResponse =
        withTokenRefresh {
            client.post("$baseUrl/api/properties/$propertyId/devices") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                parameter("schemaName", schemaName)
                setBody(request)
            }.body()
        }

    suspend fun updatePropertyAdmin(id: String, schemaName: String, request: UpdatePropertyRequest): PropertyResponse =
        withTokenRefresh {
            client.put("$baseUrl/api/properties/$id?schemaName=$schemaName") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    suspend fun deactivatePropertyAdmin(id: String, schemaName: String) =
        withTokenRefresh {
            client.delete("$baseUrl/api/properties/$id?schemaName=$schemaName") { bearerAuth(currentToken) }
        }

    suspend fun deactivateDeviceAdmin(id: String, schemaName: String) =
        withTokenRefresh {
            client.delete("$baseUrl/api/devices/$id?schemaName=$schemaName") { bearerAuth(currentToken) }
        }

    suspend fun toggleAvailabilityAdmin(schemaName: String, product: ProductResponse, isAvailable: Boolean): ProductResponse =
        withTokenRefresh {
            client.patch("$baseUrl/api/products/${product.id}/availability?schemaName=$schemaName") {
                bearerAuth(currentToken)
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateProductRequest(
                        name = product.name,
                        description = product.description,
                        basePrice = product.basePrice,
                        imageUrl = product.imageUrl,
                        isAvailable = isAvailable
                    )
                )
            }.body()
        }

    // Delete operations
    suspend fun deleteOwner(id: String) = delete("/api/owners/$id")
    suspend fun deleteProperty(id: String) = delete("/api/properties/$id")
    suspend fun deleteDevice(id: String) = delete("/api/devices/$id")
    suspend fun deleteProduct(id: String) = delete("/api/products/$id")

    suspend fun deleteProductAdmin(id: String, schemaName: String) {
        withTokenRefresh {
            client.delete("$baseUrl/api/products/$id") {
                bearerAuth(currentToken)
                parameter("schemaName", schemaName)
            }.let { if (!it.status.isSuccess()) throw Exception("Failed to delete product") }
        }
    }

    suspend fun getPayment(orderId: String): PaymentResponse? {
        return withTokenRefresh {
            val response = client.get("$baseUrl/api/orders/$orderId/payment") {
                bearerAuth(currentToken)
            }
            if (response.status == HttpStatusCode.NotFound) null
            else response.body()
        }
    }

    // Health check — unauthenticated, must NOT go through withTokenRefresh.
    suspend fun checkHealth(): Boolean = try {
        client.get("$baseUrl/api/health").status.value == 200
    } catch (_: Exception) {
        false
    }
}
