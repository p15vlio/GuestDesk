# :domain

Shared domain layer. The innermost module — no external dependencies, no framework imports.
Every other module depends on this one (directly or transitively).

**Depends on**: nothing
**Used by**: `:data`, `:backend`, `:owner-web`, `:rider`, `:app`

---

## Enums (`domain/enum/`)

| File | Values |
|------|--------|
| `UserRole` | `PLATFORM_ADMIN, OWNER, STORE_PARTNER, RIDER` |
| `OrderStatus` | `PENDING, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED` |
| `OrderType` | `IN_ROOM_STORE, PARTNER_STORE, FOOD_DELIVERY` |
| `ProductCategory` | `BEVERAGE, HOT_DRINK, FOOD, SNACK, ALCOHOL, PERSONAL_CARE, PHARMACY, OTHER` |
| `ProductSource` | `OWN_STOCK, PARTNER_STORE, EXTERNAL_PLATFORM` |
| `FulfillmentModel` | `OWN_INFRASTRUCTURE, EXTERNAL_SERVICE, HYBRID` |
| `DeliveryProvider` | `OWN_RIDER, WOLT_DRIVE, STORE_SELF` |
| `DeliveryAssignmentStatus` | `ASSIGNED, PICKED_UP, DELIVERED, FAILED` |
| `ExternalPlatform` | `EFOOD, WOLT` |
| `PaymentMethod` | `VIVA_APP2APP, VIVA_CLOUD, REVOLUT_QR` |
| `PaymentStatus` | `PENDING, COMPLETED, FAILED, REFUNDED` |

---

## Entities (`domain/entity/`)

Pure Kotlin `data class`. No annotations. IDs are `String` (UUID). Timestamps are `Long` (epoch ms). Money is `Long` (cents).

### Platform-level (`entity/platform/`) — public DB schema
| Entity | Key fields |
|--------|-----------|
| `Owner` | id, name, email, phone, schemaName, isActive, createdAt |
| `PlatformAdmin` | id, email, createdAt |

### Tenant-level (`entity/tenant/`) — tenant_{id} DB schema
| Entity | Key fields |
|--------|-----------|
| `Property` | id, name, address, city, country, timezone, fulfillmentModel, isActive, createdAt |
| `Device` | id, propertyId, name, androidDeviceId, isActive, isKioskEnabled, lastSeenAt |
| `PartnerStore` | id, name, contactEmail, contactPhone, isPharmacy, isActive, createdAt |
| `Rider` | id, name, contactPhone, isActive, createdAt |
| `Product` | id, name, description, category, source, basePrice, imageUrl, isAvailable, createdAt |
| `Inventory` | id, productId, propertyId, quantity, priceOverride |
| `Order` | id, deviceId, propertyId, guestEmail, status, type, fulfillmentModel, deliveryProvider, externalPlatform, partnerStoreId, totalAmount, notes, createdAt, updatedAt |
| `OrderItem` | id, orderId, productId, productName, unitPrice, quantity, subtotal |
| `Payment` | id, orderId, method, status, amount, providerTransactionId, createdAt |
| `DeliveryAssignment` | id, orderId, riderId, storePartnerId, status, assignedAt, pickedUpAt, deliveredAt |

---

## Repository Interfaces (`domain/repository/`)

One interface per aggregate root. All methods are `suspend`. Returns are bare domain types.

`OwnerRepository` · `PropertyRepository` · `DeviceRepository` · `PartnerStoreRepository` · `RiderRepository` · `ProductRepository` · `InventoryRepository` · `OrderRepository` · `PaymentRepository` · `DeliveryAssignmentRepository`

---

## Port Interfaces (`domain/port/`)

Contracts that external adapters in `:backend` must implement.

| Interface | Implemented by |
|-----------|---------------|
| `FoodDeliveryProvider` | efood adapter, Wolt adapter |
| `LogisticsProvider` | Wolt Drive adapter, own-rider dispatcher |
| `PaymentGateway` | Viva App2App, Viva Cloud, Revolut adapters |
| `NotificationService` | Email receipt service |
