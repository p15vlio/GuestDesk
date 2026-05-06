# Επισκόπηση Αρχιτεκτονικής & Δομής Έργου (Project Map)

Το Guest Desk ακολουθεί μια αρχιτεκτονική **Monorepo** με χρήση **Kotlin Multiplatform (KMP)**. Η δομή αυτή επιτρέπει τον διαμοιρασμό κώδικα (shared code) μεταξύ διαφορετικών πλατφορμών, διασφαλίζοντας τη συνέπεια των δεδομένων και της επιχειρησιακής λογικής.

### Δομή Modules

#### 1. `:domain` (Common Multiplatform)
Το κεντρικό module του συστήματος, υλοποιημένο με τις αρχές του **Clean Architecture**.
- **Σκοπός**: Περιέχει την αμιγή επιχειρησιακή λογική (Business Logic) και τους κανόνες του συστήματος.
- **Περιεχόμενα**: 
    - **Entities**: Οι βασικές οντότητες (Order, Product, Property κλπ).
    - **Ports (Interfaces)**: Ορισμοί για repositories και εξωτερικές υπηρεσίες.
    - **Use Cases**: Οι συγκεκριμένες λειτουργίες του συστήματος.
- **Ιδιαιτερότητα**: Δεν έχει καμία εξάρτηση από πλαίσια (frameworks) ή βάσεις δεδομένων.

#### 2. `:data` (Common Multiplatform)
- **Σκοπός**: Διαχειρίζεται τη μεταφορά δεδομένων και την επικοινωνία με το API.
- **Περιεχόμενα**: 
    - **Request/Response Models (DTOs)**: Τα αντικείμενα που ανταλλάσσονται μέσω REST API.
    - **API Client**: Κοινός κώδικας για την πραγματοποίηση κλήσεων δικτύου.
- **Χρήση**: Χρησιμοποιείται από όλα τα UI modules (Android, Web) για κοινή διαχείριση δεδομένων.

#### 3. `:backend` (JVM / Ktor)
- **Σκοπός**: Ο εξυπηρετητής (Server) του συστήματος.
- **Περιεχόμενα**: 
    - **Database (Exposed)**: Υλοποίηση των πινάκων και διαχείριση της PostgreSQL (Multi-tenant schema logic, SchemaManager, TenantContext).
    - **Security**: Διαχείριση αυθεντικοποίησης (JWT, BCrypt) και ασφάλειας.
    - **Repository Layer**: Υλοποίηση των repository interfaces μέσω Exposed DSL — `ExposedOwnerRepository`, `ExposedPropertyRepository`, `ExposedDeviceRepository`, `ExposedProductRepository`, `ExposedOrderRepository` κ.ά.
    - **Service Layer**: Επιχειρηματική λογική ανά οντότητα — `OwnerService` (δημιουργία tenant schema, προσωρινός κωδικός), `PropertyService`, `DeviceService` (QR activation), `ProductService`, `OrderService` (state machine), `AuditLogService`.
    - **Routing Layer**: ~32 REST endpoints — `OwnerRoutes`, `PropertyRoutes`, `DeviceRoutes`, `ProductRoutes`, `OrderRoutes`, `AuthRoutes`, `PaymentRoutes` (`POST /api/payments` χωρίς auth, `GET /api/orders/{id}/payment` OWNER), `AuditLogRoutes` (`GET /api/audit-logs` PLATFORM_ADMIN, pagination), `AdminRoutes` (`GET /api/admin/owners/stats`), `DashboardRoutes` (`GET /api/dashboard/summary`), `HealthRoute`. `RouteExtensions` για εξαγωγή JWT claims (role, schemaName) με fallback για PLATFORM_ADMIN cross-tenant πρόσβαση.
    - **Adapter Layer**: 4 emulated adapters που υλοποιούν τα port interfaces του domain — `EmulatedFoodDeliveryAdapter`, `EmulatedLogisticsAdapter`, `EmulatedPaymentAdapter`, `EmulatedEmailAdapter`.

#### 4. `:app` (Android / Compose)
- **Σκοπός**: Η εφαρμογή Guest Kiosk για tablet.
- **Περιεχόμενα**: 
    - **UI**: Διεπαφή χρήστη με Jetpack Compose.
    - **Kiosk Logic**: Υλοποίηση του "κλειδωμένου" περιβάλλοντος για δημόσια χρήση.

#### 5. `:owner-web` (WebAssembly - WasmJs / Compose)
- **Σκοπός**: Το κεντρικό διαχειριστικό περιβάλλον για Platform Admins και Owners.
- **Τεχνολογία**: Kotlin/Wasm (Compose Multiplatform), Ktor JS client, JWT αυθεντικοποίηση με αυτόματο token refresh.
- **Υποδομή**: `AppScaffold` (dark navy sidebar 240dp, top bar με role chip), `FormDialog` (sticky header + scrollable body + sticky footer), `OpenDeliveryTheme` (deep blue / teal, `AppShapes`), `BreadcrumbBar`, `EmptyState`, `QrCodeDialog`, `StatusChip`, `ActiveBadge`.
- **Οθόνες Platform Admin**:
    - `AdminDashboardScreen` — 7 stat cards, top-5 bar chart, recent owners, recent audit log
    - `OwnerListScreen` — CRUD με `TempPasswordDialog`, stats badges, search
    - `AdminOwnerViewScreen` — Tabbed tenant view (Dashboard / Properties / Products / Orders)
    - `AdminPropertiesScreen` — Όλα τα καταλύματα per owner, Add/Edit/Delete
    - `AdminDevicesScreen` — Συσκευές per owner→property, Add με QrCodeDialog, Delete
    - `AdminProductsScreen` — Προϊόντα per owner, availability toggle, Add/Delete
    - `AuditLogScreen` — Dual filter (Action + Entity), pagination, expandable details
    - `SettingsScreen`, `IntegrationsScreen` (6 emulated adapters), `AboutScreen`, `HelpScreen`, `AdminStubScreen`
- **Οθόνες Owner**:
    - `DashboardScreen` — 5 stat cards, quick action buttons, property grid, recent orders
    - `PropertyListScreen` — Πλήρες address model, Create/Edit/Delete, fulfillment dropdown
    - `DeviceListScreen` — Activation code με copy button, BreadcrumbBar, Delete
    - `ProductListScreen` — Optimistic availability toggle, Create/Edit/Delete, Snackbar
    - `OrderListScreen` — Status filter, expandable cards με payment info, BreadcrumbBar
    - `OwnerOrdersScreen` — Συνολικές παραγγελίες cross-property
- **LoginScreen**: Δύο-panel layout (40% gradient brand panel + 60% form panel), role selector, health indicator.

---

### Τεχνολογικό Stack
- **Γλώσσα**: Kotlin (100%)
- **Backend Framework**: Ktor
- **Database**: PostgreSQL (Schema-per-tenant architecture)
- **UI Framework**: Compose Multiplatform (Android & Wasm)
- **Dependency Management**: Gradle (Kotlin DSL)

---
[🔙 Επιστροφή στο README](../../README.md)
