#!/usr/bin/env python3
"""
GuestDesk — Platform Admin Seed Script
=======================================
Inserts a Platform Admin account into the `public.platform_admins` table.

Requirements:
    pip install bcrypt psycopg2-binary

Usage:
    python seed_admin.py

    Override defaults via environment variables:
        DB_HOST      (default: localhost)
        DB_PORT      (default: 5432)
        DB_NAME      (default: opendelivery)
        DB_USER      (default: postgres)
        DB_PASSWORD  (default: empty string)
        ADMIN_EMAIL  (default: admin@guestdesk.io)
        ADMIN_PASS   (default: Admin@GuestDesk1)

Notes:
    - BCrypt cost factor is 12, matching PasswordHasher.kt in the backend.
    - The script is idempotent: re-running it with the same email is a no-op.
    - The `platform_admins` table must already exist (run the backend once with
      RESET_DB=true or call SchemaManager.createPlatformTables() first).
"""

import os
import sys
import uuid
import time

try:
    import bcrypt
except ImportError:
    sys.exit("Missing dependency: run  pip install bcrypt")

try:
    import psycopg2
except ImportError:
    sys.exit("Missing dependency: run  pip install psycopg2-binary")


# ── Configuration ─────────────────────────────────────────────────────────────

DB_HOST     = os.getenv("DB_HOST",     "localhost")
DB_PORT     = int(os.getenv("DB_PORT", "5432"))
DB_NAME     = os.getenv("DB_NAME",     "opendelivery")
DB_USER     = os.getenv("DB_USER",     "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "opendelivery")

ADMIN_EMAIL = os.getenv("ADMIN_EMAIL", "admin@guestdesk.io")
ADMIN_PASS  = os.getenv("ADMIN_PASS",  "Admin@GuestDesk1")

BCRYPT_COST = 12  # Must match PasswordHasher.kt (BCrypt.withDefaults().hashToString(12, ...))


# ── Hashing ───────────────────────────────────────────────────────────────────

def hash_password(password: str) -> str:
    """BCrypt hash with cost 12. $2b$ prefix is accepted by the Java at.favre library."""
    salt = bcrypt.gensalt(rounds=BCRYPT_COST)
    return bcrypt.hashpw(password.encode("utf-8"), salt).decode("utf-8")


# ── Seed ──────────────────────────────────────────────────────────────────────

def seed():
    print(f"Connecting to {DB_HOST}:{DB_PORT}/{DB_NAME} as {DB_USER} ...")

    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            dbname=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD,
        )
    except psycopg2.OperationalError as e:
        sys.exit(f"Connection failed: {e}")

    cur = conn.cursor()

    # Check if admin already exists
    cur.execute("SELECT id FROM platform_admins WHERE email = %s", (ADMIN_EMAIL,))
    if cur.fetchone():
        print(f"Admin '{ADMIN_EMAIL}' already exists — nothing to do.")
        cur.close()
        conn.close()
        return

    print(f"Hashing password (BCrypt cost {BCRYPT_COST}) ...")
    password_hash = hash_password(ADMIN_PASS)

    admin_id   = str(uuid.uuid4())
    created_at = int(time.time() * 1000)  # epoch ms, matching all backend timestamps

    cur.execute(
        """
        INSERT INTO platform_admins (id, email, password_hash, created_at)
        VALUES (%s, %s, %s, %s)
        """,
        (admin_id, ADMIN_EMAIL, password_hash, created_at),
    )

    conn.commit()
    cur.close()
    conn.close()

    print()
    print("✓ Platform Admin seeded successfully.")
    print(f"  Email    : {ADMIN_EMAIL}")
    print(f"  Password : {ADMIN_PASS}")
    print(f"  ID       : {admin_id}")
    print()
    print("Change the password after first login.")


if __name__ == "__main__":
    seed()
