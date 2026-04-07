# Τοπικό Περιβάλλον Ανάπτυξης

## Προϋποθέσεις

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (για την εκτέλεση της βάσης δεδομένων)
- [Python 3.x](https://www.python.org/) (για το seed script)
- JDK 21+

---

## Βάση Δεδομένων & Backend (via Docker)

Το σύστημα είναι πλήρως dockerized. Η διαχείριση γίνεται μέσω του `manage-db.bat` από το **root** του project.

### Εκκίνηση / Διακοπή

```bash
# Εκκίνηση συστήματος (PostgreSQL & Backend)
_Tools\Docker\manage-db.bat up

# Διακοπή και αφαίρεση containers
_Tools\Docker\manage-db.bat down

# Εμφάνιση κατάστασης
_Tools\Docker\manage-db.bat status

# Παρακολούθηση logs σε πραγματικό χρόνο
_Tools\Docker\manage-db.bat logs
```

### Διαπιστευτήρια

Τα credentials σύνδεσης βρίσκονται στο `_Tools/Docker/.env`:

| Μεταβλητή         | Τιμή          |
|-------------------|---------------|
| `POSTGRES_DB`     | opendelivery  |
| `POSTGRES_USER`   | postgres      |
| `POSTGRES_PASSWORD` | opendelivery |

> **Σημείωση**: Το `.env` ανεβαίνει σκόπιμα στο repository για διευκόλυνση (demo credentials). Υπό συνθήκες παραγωγής αυτό δεν γίνεται ποτέ.

### JDBC URL (για το backend)

```
jdbc:postgresql://localhost:5432/opendelivery
```

---

## Platform Admin Seed

Μετά την πρώτη εκκίνηση του backend (που δημιουργεί τους πίνακες), εισάγετε τον Platform Admin λογαριασμό:

```bash
pip install bcrypt psycopg2-binary
python _Tools/seed_admin.py
```

Προεπιλεγμένα credentials: `admin@guestdesk.io` / `Admin@GuestDesk1`

Δείτε [`_Tools/seed_admin.py`](../../_Tools/seed_admin.py) για override μέσω environment variables.
