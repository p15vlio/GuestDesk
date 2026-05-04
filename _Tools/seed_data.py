#!/usr/bin/env python3
"""
GuestDesk — Demo Data Seeder
=============================
Creates 5 owners with 2–5 properties each and 20 products per property.
Requires the backend running on localhost:8080 and a seeded Platform Admin.

Requirements:
    pip install requests

Usage:
    python seed_data.py

Environment variables (all optional):
    BASE_URL     (default: http://localhost:8080)
    ADMIN_EMAIL  (default: admin@guestdesk.io)
    ADMIN_PASS   (default: Admin@GuestDesk1)
"""

import os
import sys
import random
import time

try:
    import requests
except ImportError:
    sys.exit("Missing dependency: run  pip install requests")

BASE_URL    = os.getenv("BASE_URL",    "http://localhost:8080")
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL", "admin@guestdesk.io")
ADMIN_PASS  = os.getenv("ADMIN_PASS",  "Admin@GuestDesk1")

PRODUCTS_PER_PROPERTY = 20
UNAVAILABLE_PER_PROPERTY = 4  # products to mark unavailable after creation

# ── Product catalog ────────────────────────────────────────────────────────────
# (name, description, category, source, basePrice_cents)

CATALOG = [
    ("Still Water 500ml",           "Cold still mineral water, 500 ml bottle",                    "BEVERAGE",      "OWN_STOCK",           150),
    ("Sparkling Water 500ml",       "Chilled sparkling mineral water, 500 ml bottle",              "BEVERAGE",      "OWN_STOCK",           150),
    ("Orange Juice 250ml",          "Fresh-pressed orange juice carton",                           "BEVERAGE",      "OWN_STOCK",           220),
    ("Energy Drink 250ml",          "Chilled energy drink",                                        "BEVERAGE",      "OWN_STOCK",           280),
    ("Iced Coffee 330ml",           "Ready-to-drink cold-brew coffee",                             "BEVERAGE",      "OWN_STOCK",           320),
    ("Greek Coffee Pods (×2)",      "Nespresso-compatible Greek mountain coffee pods",             "HOT_DRINK",     "OWN_STOCK",           250),
    ("Hot Chocolate Sachet",        "Premium hot chocolate powder sachet",                         "HOT_DRINK",     "OWN_STOCK",           190),
    ("Green Tea Bags (×3)",         "Organic green tea bags",                                      "HOT_DRINK",     "OWN_STOCK",           150),
    ("Potato Chips 40g",            "Classic salted chips",                                        "SNACK",         "OWN_STOCK",           180),
    ("Mixed Nuts 80g",              "Roasted mixed nuts — cashews, almonds, peanuts",              "SNACK",         "OWN_STOCK",           320),
    ("Dark Chocolate 100g",         "70% cacao dark chocolate bar",                                "SNACK",         "OWN_STOCK",           290),
    ("Honey & Sesame Bar",          "Traditional Greek sesame and honey snack bar",                "SNACK",         "OWN_STOCK",           199),
    ("Greek Crackers 80g",          "Whole-grain crackers with olive oil",                         "SNACK",         "OWN_STOCK",           240),
    ("Pita Chips 50g",              "Baked pita chips with sea salt",                              "SNACK",         "OWN_STOCK",           210),
    ("Toothbrush Set",              "Soft toothbrush and mini toothpaste",                         "PERSONAL_CARE", "OWN_STOCK",           350),
    ("Shampoo Sachet 30ml",         "Hotel-size shampoo sachet",                                   "PERSONAL_CARE", "OWN_STOCK",           220),
    ("Sunscreen SPF50 50ml",        "Water-resistant sun protection SPF 50",                       "PERSONAL_CARE", "OWN_STOCK",           890),
    ("Shower Cap",                  "Disposable shower cap",                                       "PERSONAL_CARE", "OWN_STOCK",           120),
    ("Insect Repellent Spray 100ml","DEET-free insect repellent spray",                            "PERSONAL_CARE", "PARTNER_STORE",       590),
    ("USB-C Cable 1m",              "Braided USB-C to USB-C charging cable",                       "OTHER",         "OWN_STOCK",           790),
    ("Universal Travel Adapter",    "Universal plug adapter — EU / UK / US / AU",                 "OTHER",         "OWN_STOCK",           1290),
    ("Power Bank 5000mAh",          "Portable USB-A + USB-C power bank",                          "OTHER",         "OWN_STOCK",           1990),
    ("Paracetamol 500mg (10 tabs)", "Pain and fever relief tablets",                               "PHARMACY",      "PARTNER_STORE",       450),
    ("Antacid Tablets (8 tabs)",    "Fast-acting antacid relief tablets",                          "PHARMACY",      "PARTNER_STORE",       380),
    ("Blister Plasters (6 pack)",   "Hydrocolloid blister plasters",                              "PHARMACY",      "PARTNER_STORE",       299),
    ("After-Sun Lotion 100ml",      "Soothing after-sun moisturising lotion",                      "PHARMACY",      "PARTNER_STORE",       690),
    ("Greek Red Wine 750ml",        "Local label dry red wine",                                    "ALCOHOL",       "PARTNER_STORE",       1490),
    ("Greek White Wine 750ml",      "Local label crisp white wine",                                "ALCOHOL",       "PARTNER_STORE",       1390),
    ("Rosé Wine 750ml",             "Local label dry rosé wine",                                   "ALCOHOL",       "PARTNER_STORE",       1490),
    ("Greek Beer 330ml",            "Chilled local lager beer",                                    "ALCOHOL",       "OWN_STOCK",           280),
]

# ── Owner definitions ──────────────────────────────────────────────────────────

OWNERS = [
    {
        "ownerType": "INDIVIDUAL",
        "firstName": "Alexandros",
        "lastName": "Papadopoulos",
        "vatId": "123456789",
        "contactFirstName": "Alexandros",
        "contactLastName": "Papadopoulos",
        "contactEmail": "a.papadopoulos@guestdesk-demo.io",
        "contactPhone": "+306971000001",
        "companyActivity": "Short-term rental — Athens",
        "address": "Ermou 15, 10563 Athens",
        "subscriptionPriceCents": 4900,
        "properties": [
            {
                "name": "Syntagma Studio",
                "streetName": "Mitropoleos", "streetNo": "8", "postalCode": "10557",
                "area": "Syntagma", "level": 3, "nameOnDoorbell": "Papadopoulos",
                "contactPhone": "+306971000001", "fulfillmentModel": "HYBRID",
            },
            {
                "name": "Koukaki Apartment",
                "streetName": "Veikou", "streetNo": "22", "postalCode": "11742",
                "area": "Koukaki", "level": 1, "nameOnDoorbell": "Papadopoulos",
                "contactPhone": "+306971000001", "fulfillmentModel": "OWN_INFRASTRUCTURE",
            },
            {
                "name": "Psirri Loft",
                "streetName": "Aristofanous", "streetNo": "5", "postalCode": "10554",
                "area": "Psirri", "level": 4, "nameOnDoorbell": "Papadopoulos",
                "contactPhone": "+306971000001", "fulfillmentModel": "EXTERNAL_SERVICE",
            },
        ],
    },
    {
        "ownerType": "INDIVIDUAL",
        "firstName": "Eleni",
        "lastName": "Georgiou",
        "vatId": "234567891",
        "contactFirstName": "Eleni",
        "contactLastName": "Georgiou",
        "contactEmail": "e.georgiou@guestdesk-demo.io",
        "contactPhone": "+306932000002",
        "companyActivity": "Short-term rental — Thessaloniki",
        "address": "Tsimiski 44, 54623 Thessaloniki",
        "subscriptionPriceCents": 3900,
        "properties": [
            {
                "name": "City Center Suite",
                "streetName": "Tsimiski", "streetNo": "44", "postalCode": "54623",
                "area": "Center", "level": 2, "nameOnDoorbell": "Georgiou",
                "contactPhone": "+306932000002", "fulfillmentModel": "OWN_INFRASTRUCTURE",
            },
            {
                "name": "Ladadika Boutique",
                "streetName": "Katouni", "streetNo": "7", "postalCode": "54625",
                "area": "Ladadika", "level": 0, "nameOnDoorbell": "Georgiou",
                "contactPhone": "+306932000002", "fulfillmentModel": "HYBRID",
            },
        ],
    },
    {
        "ownerType": "COMPANY",
        "companyName": "Hellenic Stays Ltd",
        "vatId": "800123456",
        "contactFirstName": "Nikos",
        "contactLastName": "Stavros",
        "contactEmail": "info@hellenicstays-demo.io",
        "contactPhone": "+302241000003",
        "companyActivity": "Property management — Rhodes",
        "address": "Ippotou 18, 85100 Rhodes",
        "website": "https://hellenicstays-demo.io",
        "subscriptionPriceCents": 9900,
        "properties": [
            {
                "name": "Rhodes Old Town Villa",
                "streetName": "Ippotou", "streetNo": "18", "postalCode": "85100",
                "area": "Old Town", "level": 0, "nameOnDoorbell": "Hellenic Stays",
                "contactPhone": "+302241000003", "fulfillmentModel": "HYBRID",
            },
            {
                "name": "Lindos Sea View",
                "streetName": "Akropolis", "streetNo": "3", "postalCode": "85107",
                "area": "Lindos", "level": 1, "nameOnDoorbell": "Hellenic Stays",
                "contactPhone": "+302241000003", "fulfillmentModel": "EXTERNAL_SERVICE",
            },
            {
                "name": "Ixia Beach Studio",
                "streetName": "Ethnikis Antistaseos", "streetNo": "120", "postalCode": "85101",
                "area": "Ixia", "level": 0, "nameOnDoorbell": "Hellenic Stays",
                "contactPhone": "+302241000003", "fulfillmentModel": "OWN_INFRASTRUCTURE",
            },
            {
                "name": "Diagoras Airport Suite",
                "streetName": "Paradisi", "streetNo": "1", "postalCode": "85102",
                "area": "Paradisi", "level": 2, "nameOnDoorbell": "Hellenic Stays",
                "contactPhone": "+302241000003", "fulfillmentModel": "OWN_INFRASTRUCTURE",
            },
            {
                "name": "Faliraki Summer Flat",
                "streetName": "Elpidas", "streetNo": "9", "postalCode": "85105",
                "area": "Faliraki", "level": 1, "nameOnDoorbell": "Hellenic Stays",
                "contactPhone": "+302241000003", "fulfillmentModel": "HYBRID",
            },
        ],
    },
    {
        "ownerType": "INDIVIDUAL",
        "firstName": "Konstantinos",
        "lastName": "Nikolaou",
        "vatId": "345678912",
        "contactFirstName": "Konstantinos",
        "contactLastName": "Nikolaou",
        "contactEmail": "k.nikolaou@guestdesk-demo.io",
        "contactPhone": "+302289000004",
        "companyActivity": "Short-term rental — Mykonos",
        "address": "Fabrika, 84600 Mykonos",
        "subscriptionPriceCents": 5900,
        "properties": [
            {
                "name": "Mykonos Town Suite",
                "streetName": "Matoyianni", "streetNo": "14", "postalCode": "84600",
                "area": "Town", "level": 1, "nameOnDoorbell": "Nikolaou",
                "contactPhone": "+302289000004", "fulfillmentModel": "HYBRID",
            },
            {
                "name": "Little Venice Balcony",
                "streetName": "Agiou Anargirou", "streetNo": "2", "postalCode": "84600",
                "area": "Little Venice", "level": 0, "nameOnDoorbell": "Nikolaou",
                "contactPhone": "+302289000004", "fulfillmentModel": "HYBRID",
            },
            {
                "name": "Ornos Beach Apartment",
                "streetName": "Ornos", "streetNo": "5", "postalCode": "84600",
                "area": "Ornos", "level": 0, "nameOnDoorbell": "Nikolaou",
                "contactPhone": "+302289000004", "fulfillmentModel": "OWN_INFRASTRUCTURE",
            },
            {
                "name": "Psarou Hideaway",
                "streetName": "Psarou", "streetNo": "1", "postalCode": "84600",
                "area": "Psarou", "level": 0, "nameOnDoorbell": "Nikolaou",
                "contactPhone": "+302289000004", "fulfillmentModel": "HYBRID",
            },
        ],
    },
    {
        "ownerType": "COMPANY",
        "companyName": "Santorini Dreams LLC",
        "vatId": "900456123",
        "contactFirstName": "Maria",
        "contactLastName": "Economou",
        "contactEmail": "reservations@santorinidreams-demo.io",
        "contactPhone": "+302286000005",
        "companyActivity": "Luxury rental management — Santorini",
        "address": "Oia, 84702 Santorini",
        "website": "https://santorinidreams-demo.io",
        "subscriptionPriceCents": 14900,
        "properties": [
            {
                "name": "Caldera View Suite",
                "streetName": "Oia", "streetNo": "12", "postalCode": "84702",
                "area": "Oia", "level": 0, "nameOnDoorbell": "Santorini Dreams",
                "contactPhone": "+302286000005", "fulfillmentModel": "HYBRID",
            },
            {
                "name": "Oia Sunset Villa",
                "streetName": "Oia", "streetNo": "25", "postalCode": "84702",
                "area": "Oia", "level": 1, "nameOnDoorbell": "Santorini Dreams",
                "contactPhone": "+302286000005", "fulfillmentModel": "HYBRID",
            },
            {
                "name": "Fira Boutique Apartment",
                "streetName": "Ypapantis", "streetNo": "8", "postalCode": "84700",
                "area": "Fira", "level": 2, "nameOnDoorbell": "Santorini Dreams",
                "contactPhone": "+302286000005", "fulfillmentModel": "EXTERNAL_SERVICE",
            },
            {
                "name": "Pyrgos Retreat",
                "streetName": "Pyrgos", "streetNo": "3", "postalCode": "84701",
                "area": "Pyrgos", "level": 0, "nameOnDoorbell": "Santorini Dreams",
                "contactPhone": "+302286000005", "fulfillmentModel": "OWN_INFRASTRUCTURE",
            },
        ],
    },
]


# ── HTTP helpers ───────────────────────────────────────────────────────────────

def post(path: str, body: dict, token: str = "") -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = requests.post(f"{BASE_URL}{path}", json=body, headers=headers, timeout=15)
    if not r.ok:
        raise RuntimeError(f"POST {path} → {r.status_code}: {r.text}")
    return r.json()


def patch(path: str, body: dict, token: str) -> dict:
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}
    r = requests.patch(f"{BASE_URL}{path}", json=body, headers=headers, timeout=15)
    if not r.ok:
        raise RuntimeError(f"PATCH {path} → {r.status_code}: {r.text}")
    return r.json()


# ── Seed logic ─────────────────────────────────────────────────────────────────

def login(email: str, password: str, role: str) -> str:
    resp = post("/api/auth/login", {"email": email, "password": password, "role": role, "schemaName": None})
    return resp["accessToken"]


def create_owner(admin_token: str, owner: dict) -> dict:
    payload = {k: v for k, v in owner.items() if k != "properties"}
    return post("/api/owners", payload, admin_token)


def create_property(owner_token: str, prop: dict) -> dict:
    return post("/api/properties", prop, owner_token)


def create_product(owner_token: str, entry: tuple) -> dict:
    name, description, category, source, base_price = entry
    return post("/api/products", {
        "name": name,
        "description": description,
        "category": category,
        "source": source,
        "basePrice": base_price,
        "imageUrl": None,
    }, owner_token)


def mark_unavailable(owner_token: str, product: dict) -> None:
    patch(f"/api/products/{product['id']}/availability", {
        "name": product["name"],
        "description": product["description"],
        "basePrice": product["basePrice"],
        "imageUrl": product["imageUrl"],
        "isAvailable": False,
    }, owner_token)


def wait_for_backend(retries: int = 20) -> None:
    print(f"Checking backend at {BASE_URL}/api/health …")
    for attempt in range(retries):
        try:
            r = requests.get(f"{BASE_URL}/api/health", timeout=5)
            if r.ok:
                print("Backend is up.\n")
                return
        except requests.ConnectionError:
            pass
        print(f"  Not ready yet, retrying ({attempt + 1}/{retries}) …")
        time.sleep(3)
    sys.exit("Backend did not become ready in time. Is it running?")


def seed() -> None:
    wait_for_backend()

    print("Logging in as Platform Admin …")
    admin_token = login(ADMIN_EMAIL, ADMIN_PASS, "PLATFORM_ADMIN")
    print("  Admin token acquired.\n")

    total_properties = 0
    total_products   = 0

    for owner_def in OWNERS:
        display = owner_def.get("companyName") or f"{owner_def.get('firstName')} {owner_def.get('lastName')}"
        print(f"Creating owner: {display} …")
        owner_resp = create_owner(admin_token, owner_def)
        temp_password = owner_resp["temporaryPassword"]
        owner_email   = owner_resp["contactEmail"]
        print(f"  Created — email: {owner_email}  temp-password: {temp_password}")

        owner_token = login(owner_email, temp_password, "OWNER")
        print(f"  Owner token acquired.")

        for prop_def in owner_def["properties"]:
            print(f"  Creating property: {prop_def['name']} …")
            prop_resp = create_property(owner_token, prop_def)
            prop_id   = prop_resp["id"]
            total_properties += 1

            catalog_sample = random.sample(CATALOG, PRODUCTS_PER_PROPERTY)
            created_products = []
            for entry in catalog_sample:
                p = create_product(owner_token, entry)
                created_products.append(p)
            total_products += len(created_products)

            # Mark a few products unavailable to simulate realistic stock state
            to_deactivate = random.sample(created_products, UNAVAILABLE_PER_PROPERTY)
            for p in to_deactivate:
                mark_unavailable(owner_token, p)

            available   = len(created_products) - UNAVAILABLE_PER_PROPERTY
            print(f"    {len(created_products)} products created ({available} available, {UNAVAILABLE_PER_PROPERTY} unavailable)")

        print()

    print("=" * 60)
    print(" Seeding complete!")
    print(f"  Owners:     {len(OWNERS)}")
    print(f"  Properties: {total_properties}")
    print(f"  Products:   {total_products}")
    print("=" * 60)
    print()
    print("Admin credentials:")
    print(f"  Email    : {ADMIN_EMAIL}")
    print(f"  Password : {ADMIN_PASS}")
    print()
    print("Re-run to seed again (duplicate owners will be rejected).")


if __name__ == "__main__":
    seed()
