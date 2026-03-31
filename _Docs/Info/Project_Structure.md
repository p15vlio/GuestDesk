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
    - **Routing**: Ορισμός των REST endpoints.
    - **Database (Exposed)**: Υλοποίηση των πινάκων και διαχείριση της PostgreSQL (Multi-tenant schema logic).
    - **Security**: Διαχείριση αυθεντικοποίησης (JWT) και κρυπτογράφησης.

#### 4. `:app` (Android / Compose)
- **Σκοπός**: Η εφαρμογή Guest Kiosk για tablet.
- **Περιεχόμενα**: 
    - **UI**: Διεπαφή χρήστη με Jetpack Compose.
    - **Kiosk Logic**: Υλοποίηση του "κλειδωμένου" περιβάλλοντος για δημόσια χρήση.

#### 5. `:owner-web` (WebAssembly - WasmJs / Compose)
- **Σκοπός**: Το διαχειριστικό περιβάλλον (Panel) για τους ιδιοκτήτες καταλυμάτων.
- **Περιεχόμενα**: 
    - **Dashboard**: Διαχείριση προϊόντων, παραγγελιών και ρυθμίσεων καταλύματος.
- **Τεχνολογία**: Χρησιμοποιεί το Kotlin/Wasm για υψηλή απόδοση στο πρόγραμμα περιήγησης.

---

### Τεχνολογικό Stack
- **Γλώσσα**: Kotlin (100%)
- **Backend Framework**: Ktor
- **Database**: PostgreSQL (Schema-per-tenant architecture)
- **UI Framework**: Compose Multiplatform (Android & Wasm)
- **Dependency Management**: Gradle (Kotlin DSL)
