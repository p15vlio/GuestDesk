# Οδηγός Ανάπτυξης και Δοκιμών (Build & Test Guide)

Αυτό το έγγραφο περιγράφει τη διαδικασία μεταγλώττισης (build) και εκτέλεσης του συστήματος για τοπική ανάπτυξη και αξιολόγηση.

## 1. Αρχιτεκτονική Docker

Το σύστημα είναι πλήρως dockerized για την αποφυγή προβλημάτων συμβατότητας. Χρησιμοποιούμε δύο βασικά containers:
1. **`opendelivery-db`**: PostgreSQL 16 για την αποθήκευση δεδομένων.
2. **`opendelivery-backend`**: Η Ktor εφαρμογή, η οποία μεταγλωττίζεται αυτόματα μέσω Docker multi-stage build (JDK 17).

---

## 2. Οδηγίες Εκτέλεσης

### Βήμα 1: Εκκίνηση του Συστήματος (Full Stack)
Ανοίξτε ένα τερματικό στο **root** του project και εκτελέστε:

```bash
_Tools\Docker\manage-db.bat up
```

**Τι συμβαίνει:**
- Ξεκινά η PostgreSQL (`opendelivery-db`).
- Ξεκινά το Backend (`opendelivery-backend`) το οποίο κάνει compile τον κώδικα από το φάκελο `_Project/` και τρέχει στο port `8080`.

### Βήμα 2: Έλεγχος Λειτουργίας
Βεβαιωθείτε ότι όλα τρέχουν:
```bash
_Tools\Docker\manage-db.bat status
```
Θα πρέπει να δείτε και τα δύο containers σε κατάσταση `Up`.

### Βήμα 3: Εισαγωγή Δειγματικών Δεδομένων (Seed)
Εφόσον το σύστημα τρέχει, δημιουργήστε τον Platform Admin (από το root):
```bash
# Εγκατάσταση (αν δεν έχει γίνει ήδη)
pip install bcrypt psycopg2-binary

# Εκτέλεση του seed script
python _Tools/seed_admin.py
```
**Credentials:** `admin@guestdesk.io` / `Admin@GuestDesk1`

---

## 3. Δοκιμές με Postman

Η συλλογή Postman βρίσκεται στο `_Tools/postman_collection.json`. Χρησιμοποιεί collection variables (`{{admin_token}}`, `{{owner_token}}`, `{{owner_id}}` κ.ά.) που συμπληρώνονται αυτόματα από τα test scripts κάθε request.

### Πλήρης Ροή Δοκιμής (Run Order)

1. **Login (Platform Admin)**: `POST /api/auth/login` → αποθηκεύεται `admin_token`
2. **Health Check**: `GET /api/health`
3. **Δημιουργία Owner**: `POST /api/owners` → αποθηκεύεται `owner_id`, `owner_schema`, `owner_temp_password`
4. **Login (Owner)**: `POST /api/auth/login` με `contactEmail` + `temporaryPassword` → αποθηκεύεται `owner_token`
5. **Δημιουργία Property**: `POST /api/properties` → αποθηκεύεται `property_id`
6. **Δημιουργία Device**: `POST /api/properties/{{property_id}}/devices` → αποθηκεύεται `device_id`, `activation_code`
7. **Ενεργοποίηση Device (QR flow)**: `POST /api/devices/{{device_id}}/activate` με `activationCode` + `androidDeviceId`
8. **Δημιουργία Product**: `POST /api/products` → αποθηκεύεται `product_id`
9. **Δημιουργία Order (kiosk, χωρίς auth)**: `POST /api/orders?schemaName=...&deviceId=...&propertyId=...`
10. **Ενημέρωση Status Παραγγελίας**: `PATCH /api/orders/{{order_id}}/status`

> **Σημείωση**: Τα payment endpoints (`POST /api/payments`, `GET /api/orders/{id}/payment`) δεν είναι ακόμη ενεργά — προγραμματίζονται για επόμενη φάση.

---

## 4. Αντιμετώπιση Προβλημάτων

- **Database Connection Refused**: Βεβαιωθείτε ότι η PostgreSQL έχει ξεκινήσει πλήρως (χρειάζεται ~5-10 δευτερόλεπτα την πρώτη φορά).
- **Python Seed Script Error**: Το script χρειάζεται πρόσβαση στο port 5432. Βεβαιωθείτε ότι δεν τρέχει άλλη τοπική υπηρεσία PostgreSQL στο ίδιο port.

---
[🔙 Επιστροφή στο README](../../README.md)
