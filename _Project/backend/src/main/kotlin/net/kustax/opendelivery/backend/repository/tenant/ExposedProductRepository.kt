package net.kustax.opendelivery.backend.repository.tenant

import net.kustax.opendelivery.backend.database.table.tenant.ProductsTable
import net.kustax.opendelivery.backend.repository.tenantQuery
import net.kustax.opendelivery.domain.entity.tenant.Product
import net.kustax.opendelivery.domain.enum.ProductCategory
import net.kustax.opendelivery.domain.enum.ProductSource
import net.kustax.opendelivery.domain.repository.ProductRepository
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

class ExposedProductRepository : ProductRepository {

    override suspend fun create(product: Product): Product = tenantQuery {
        ProductsTable.insert {
            it[id] = product.id
            it[name] = product.name
            it[description] = product.description
            it[category] = product.category.name
            it[productSource] = product.source.name
            it[basePrice] = product.basePrice
            it[imageUrl] = product.imageUrl
            it[isAvailable] = product.isAvailable
            it[createdAt] = product.createdAt
        }
        product
    }

    override suspend fun findById(id: String): Product? = tenantQuery {
        ProductsTable.selectAll()
            .where { ProductsTable.id eq id }
            .singleOrNull()
            ?.toProduct()
    }

    override suspend fun findAll(): List<Product> = tenantQuery {
        ProductsTable.selectAll().map { it.toProduct() }
    }

    override suspend fun findByCategory(category: ProductCategory): List<Product> = tenantQuery {
        ProductsTable.selectAll()
            .where { ProductsTable.category eq category.name }
            .map { it.toProduct() }
    }

    override suspend fun update(product: Product): Product = tenantQuery {
        ProductsTable.update({ ProductsTable.id eq product.id }) {
            it[name] = product.name
            it[description] = product.description
            it[category] = product.category.name
            it[productSource] = product.source.name
            it[basePrice] = product.basePrice
            it[imageUrl] = product.imageUrl
            it[isAvailable] = product.isAvailable
        }
        product
    }

    override suspend fun toggleAvailability(id: String, isAvailable: Boolean): Unit = tenantQuery {
        ProductsTable.update({ ProductsTable.id eq id }) {
            it[ProductsTable.isAvailable] = isAvailable
        }
    }
}

private fun ResultRow.toProduct() = Product(
    id = this[ProductsTable.id],
    name = this[ProductsTable.name],
    description = this[ProductsTable.description],
    category = enumValueOf<ProductCategory>(this[ProductsTable.category]),
    source = enumValueOf<ProductSource>(this[ProductsTable.productSource]),
    basePrice = this[ProductsTable.basePrice],
    imageUrl = this[ProductsTable.imageUrl],
    isAvailable = this[ProductsTable.isAvailable],
    createdAt = this[ProductsTable.createdAt]
)
