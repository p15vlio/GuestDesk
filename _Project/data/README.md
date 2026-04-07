# :data — Shared DTOs and Serialization Models

KMP module shared by `:backend`, `:owner-web`, `:rider`, and `:app`.
All types are `@Serializable data class` with no imports from `:domain`.
Enum-typed fields are `String` — mapping to domain enums happens in `:backend`.

---

## Request DTOs — `net.kustax.opendelivery.data.request`

| Class | Purpose |
|---|---|
| `LoginRequest` | Credentials for owner/rider/store-partner login |
| `RefreshTokenRequest` | Refresh an expired access token using a valid refresh token |
| `CreateOwnerRequest` | Platform admin creates a new owner account |
| `CreatePropertyRequest` | Owner creates a new property |
| `UpdatePropertyRequest` | Owner updates a property (full state PUT) |
| `CreateDeviceRequest` | Owner registers a new kiosk device |
| `CreatePartnerStoreRequest` | Owner onboards a partner store |
| `UpdatePartnerStoreRequest` | Owner updates partner store contact details |
| `CreateRiderRequest` | Owner adds a new rider |
| `UpdateRiderRequest` | Owner updates rider contact details |
| `CreateProductRequest` | Owner adds a product to the catalog |
| `UpdateProductRequest` | Owner updates a product (full state PUT) |
| `UpdateInventoryRequest` | Owner sets quantity and optional price override for a product at a property |
| `OrderItemRequest` | Single line item inside `CreateOrderRequest` |
| `CreateOrderRequest` | Guest places an order from the kiosk |
| `UpdateOrderStatusRequest` | Rider or store partner advances order state |
| `InitiatePaymentRequest` | Guest initiates payment for an order |

---

## Response DTOs — `net.kustax.opendelivery.data.response`

| Class | Purpose |
|---|---|
| `TokenResponse` | Access token + refresh token returned after successful login or token refresh |
| `OwnerResponse` | Owner account details |
| `PropertyResponse` | Property details |
| `DeviceResponse` | Kiosk device details |
| `PartnerStoreResponse` | Partner store details |
| `RiderResponse` | Rider details |
| `ProductResponse` | Product catalog entry |
| `InventoryResponse` | Stock level and price override for a product at a property |
| `OrderItemResponse` | Single line item embedded in `OrderResponse` |
| `OrderResponse` | Full order including embedded items list |
| `PaymentResponse` | Payment record |
| `DeliveryAssignmentResponse` | Delivery assignment linking an order to a rider or store partner |
| `ErrorResponse` | Uniform error body returned by the status-pages plugin |
