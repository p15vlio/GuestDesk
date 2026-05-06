# Guest Desk

**Σχεδίαση και Υλοποίηση Συστήματος Ψηφιακών Παραγγελιών και Υπηρεσιών για Καταλύματα Βραχυχρόνιας Μίσθωσης**

---

## 📝 Περιγραφή Έργου
Το **Guest Desk** είναι μια multi-tenant SaaS πλατφόρμα που στοχεύει στον ψηφιακό μετασχηματισμό των παρεχόμενων υπηρεσιών σε καταλύματα βραχυχρόνιας μίσθωσης. Το σύστημα επιτρέπει στους επισκέπτες να αποκτούν πρόσβαση σε τοπικά προϊόντα και υπηρεσίες μέσω μιας συσκευής Android (Kiosk) εντός του καταλύματος, ενώ οι ιδιοκτήτες διαχειρίζονται το περιεχόμενο και τις παραγγελίες μέσω ενός κεντρικού Web Panel.

Το έργο αναπτύσσεται στο πλαίσιο πτυχιακής εργασίας στο Τμήμα Πληροφορικής.

---

## 🏗️ Αρχιτεκτονική Προσέγγιση
Το σύστημα ακολουθεί τις αρχές του **Clean Architecture** και είναι οργανωμένο ως **Monorepo**, κάνοντας χρήση της τεχνολογίας **Kotlin Multiplatform (KMP)**. Η προσέγγιση αυτή επιτρέπει:
- **Διαμοιρασμό Κώδικα (Shared Code)**: Η επιχειρησιακή λογική και τα μοντέλα δεδομένων είναι κοινά για όλες τις πλατφόρμες (Backend, Android, Web).
- **Multi-tenancy**: Πλήρης απομόνωση των δεδομένων κάθε ιδιοκτήτη (Owner) μέσω ανεξάρτητων PostgreSQL schemas.
- **Επεκτασιμότητα**: Η χρήση του Port & Adapter pattern επιτρέπει την εύκολη ενσωμάτωση εξωτερικών υπηρεσιών στο μέλλον.

---

## 📊 Τρέχουσα Κατάσταση Έργου
Μέχρι την παρούσα φάση έχουν ολοκληρωθεί τα εξής στάδια:

1.  **Ανάλυση Απαιτήσεων**: Πλήρης καταγραφή των λειτουργικών και μη λειτουργικών απαιτήσεων, των ρόλων χρηστών και των use cases του συστήματος.
2.  **Αρχιτεκτονικός Σχεδιασμός**: Καθορισμός της δομής των modules και επιλογή του τεχνολογικού stack.
3.  **Μοντελοποίηση Δεδομένων**: Σχεδιασμός και τεχνική υλοποίηση των core οντοτήτων (Entities) και του σχήματος της βάσης δεδομένων.
4.  **Σχεδιασμός API**: Αποτύπωση των βασικών διεπαφών επικοινωνίας (REST endpoints).
5.  **Database Layer & Authentication**: Πλήρης υλοποίηση του DB layer (14 πίνακες), υποδομή JWT, BCrypt, refresh token rotation, endpoints `/api/auth/login`, `/api/auth/refresh`, `/api/health`.
6.  **Service Layer & REST API**: Πλήρης υλοποίηση service layer (`OwnerService`, `PropertyService`, `DeviceService`, `ProductService`, `OrderService`, `AuditLogService`) και REST routing (~27 endpoints λειτουργικά). QR activation flow για devices. Emulated adapters για FoodDelivery, Logistics, Payment, Email (Port & Adapter pattern).
7.  **Web Panel — Αυθεντικοποίηση & Dashboard**: Ολοκλήρωση του `:owner-web` module (Compose Multiplatform / wasmJs) — Login Screen με JWT αυθεντικοποίηση, αυτόματο token refresh, και Dashboard με stat cards, λίστα καταλυμάτων και πρόσφατες παραγγελίες.
8.  **Web Panel — Ολοκλήρωση Admin & Owner Screens**: Υλοποίηση του συνόλου των οθονών του `:owner-web` module. Για τον ρόλο **Platform Admin**: `AdminOwnerViewScreen` (tabbed tenant view), `AdminPropertiesScreen`, `AdminDevicesScreen` (με QR flow), `AdminProductsScreen`, `AuditLogScreen` (dual filter, pagination), `SettingsScreen`, `IntegrationsScreen` (6 emulated adapters), `AboutScreen` (health indicator), `HelpScreen` (FAQ accordion), `AdminStubScreen`. Για τον ρόλο **Owner**: `PropertyListScreen`, `DeviceListScreen` (BreadcrumbBar, activation code), `ProductListScreen` (optimistic toggle), `OrderListScreen` (expandable cards, payment info), `OwnerOrdersScreen`. Τρία νέα UI components (`BreadcrumbBar`, `EmptyState`, `QrCodeDialog`). Backend συμπληρώθηκε με `PaymentRoutes` (POST/GET) και `AuditLogRoutes` (GET με pagination). UI polish: dark navy sidebar (slate-900), custom `FormDialog` με sticky header/footer, δύο-panel `LoginScreen`, ενισχυμένα χρώματα θέματος (Theme.kt), `AppShapes` στο MaterialTheme.

## 🚀 Γρήγορη Εκκίνηση (Quick Start)

Για να τρέξετε το σύστημα τοπικά από το **root** του project:

1.  **Εκκίνηση Docker (DB + Backend + Web Panel)**:
    ```bash
    _Tools\Docker\manage-db.bat up
    ```
    Εκκινεί τρεις υπηρεσίες: `postgres` (5432), `backend` (8080), `frontend` / nginx (80).

2.  **Seed Platform Admin**:
    ```bash
    # Προϋπόθεση: pip install bcrypt psycopg2-binary
    python _Tools/seed_admin.py
    ```
    *Credentials: `admin@guestdesk.io` / `Admin@GuestDesk1`*

3.  **Seed Demo Data** (προαιρετικό — 5 owners, 18 properties, ~360 products):
    ```bash
    # Προϋπόθεση: pip install requests
    python _Tools/seed_data.py
    ```

4.  **Έλεγχος**:
    - Backend health: `http://localhost:8080/api/health`
    - Web Panel: `http://localhost`

---

## 📂 Τεκμηρίωση & Εργαλεία
Για την καλύτερη παρακολούθηση του έργου, μπορείτε να ανατρέξετε στα παρακάτω αρχεία:

- 📑 **Εβδομαδιαίες Αναφορές**:
    - [Εβδομάδα 01 — Έναρξη & Έρευνα](_Docs/Weekly%20Reports/Week_01_230326.md)
    - [Εβδομάδα 02 — Ανάλυση & Μοντελοποίηση](_Docs/Weekly%20Reports/Week_02_300326.md)
    - [Εβδομάδα 03 — Database Layer & Authentication](_Docs/Weekly%20Reports/Week_03_060426.md)
    - [Εβδομάδα 04 — Περίληψη & Εισαγωγή](_Docs/Weekly%20Reports/Week_04_130426.md)
    - [Εβδομάδα 05 — Service Layer & REST API](_Docs/Weekly%20Reports/Week_05_200426.md)
    - [Εβδομάδα 06 — Web Panel: Login & Dashboard](_Docs/Weekly%20Reports/Week_06_270426.md)
    - [Εβδομάδα 07 — Web Panel: Admin & Owner Screens](_Docs/Weekly%20Reports/Week_07_040526.md)
- 🗺️ **Αρχιτεκτονική Επισκόπηση**: [Χάρτης Modules & Δομή Έργου](_Docs/Info/Project_Structure.md)
- 💻 **Τοπικό Περιβάλλον Ανάπτυξης**: [Docker, DB seed, JDBC config](_Docs/Info/Dev_Environment.md)
- 🧪 **Οδηγός Ανάπτυξης και Δοκιμών**: [Build & Test Guide](_Docs/Info/Build_and_Dev_Guide.md)
- 🛠️ **Εργαλεία Ελέγχου**: [Postman Collection (API)](_Tools/postman_collection.json)

---
© 2026 Κωνσταντίνος Βλιώρας — Τμήμα Πληροφορικής
