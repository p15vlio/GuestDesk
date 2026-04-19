package net.kustax.opendelivery.backend.service

import net.kustax.opendelivery.backend.repository.tenant.ExposedProductRepository
import net.kustax.opendelivery.data.request.CreateProductRequest
import net.kustax.opendelivery.data.request.UpdateProductRequest
import net.kustax.opendelivery.data.response.ProductResponse
import net.kustax.opendelivery.domain.entity.tenant.Product
import net.kustax.opendelivery.domain.enum.ProductCategory
import net.kustax.opendelivery.domain.enum.ProductSource
import java.util.UUID

class ProductService(private val productRepository: ExposedProductRepository) {

    suspend fun create(request: CreateProductRequest): ProductResponse {
        val product = Product(
            id = UUID.randomUUID().toString(),
            name = request.name,
            description = request.description,
            category = enumValueOf<ProductCategory>(request.category),
            source = enumValueOf<ProductSource>(request.source),
            basePrice = request.basePrice,
            imageUrl = request.imageUrl,
            isAvailable = true,
            createdAt = System.currentTimeMillis()
        )
        productRepository.create(product)
        return product.toResponse()
    }

    suspend fun findAll(): List<ProductResponse> =
        productRepository.findAll().map { it.toResponse() }

    suspend fun findAvailable(): List<ProductResponse> =
        productRepository.findAll()
            .filter { it.isAvailable }
            .map { it.toResponse() }

    suspend fun findById(id: String): ProductResponse =
        productRepository.findById(id)?.toResponse()
            ?: throw NotFoundException("Product not found: $id")

    suspend fun update(id: String, request: UpdateProductRequest): ProductResponse {
        val existing = productRepository.findById(id)
            ?: throw NotFoundException("Product not found: $id")
        val updated = existing.copy(
            name = request.name,
            description = request.description,
            basePrice = request.basePrice,
            imageUrl = request.imageUrl,
            isAvailable = request.isAvailable
        )
        productRepository.update(updated)
        return updated.toResponse()
    }

    suspend fun toggleAvailability(id: String, isAvailable: Boolean): ProductResponse {
        val existing = productRepository.findById(id)
            ?: throw NotFoundException("Product not found: $id")
        productRepository.toggleAvailability(id, isAvailable)
        return existing.copy(isAvailable = isAvailable).toResponse()
    }

    private fun Product.toResponse() = ProductResponse(
        id = id,
        name = name,
        description = description,
        category = category.name,
        source = source.name,
        basePrice = basePrice,
        imageUrl = imageUrl,
        isAvailable = isAvailable,
        createdAt = createdAt
    )
}
