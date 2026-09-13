# E-Bookstore — Database Design

**Document:** Database Design v1.0
**Based on:** Specification v1.0 + Implementation Plan v1.0
**Status:** AWAITING DESIGN APPROVAL
**Date:** 2026-08-24

---

## 1. Database

**PostgreSQL** (latest stable, Railway-managed).

All migrations managed by **Flyway** in sequential versioned files.
`spring.jpa.hibernate.ddl-auto=validate` — schema is never auto-generated.

---

## 2. Entity Relationship Diagram

```
users
  │
  ├──< addresses          (one user → many addresses)
  │
  ├──  carts              (one user → one cart)
  │       └──< cart_items >── books
  │
  ├──< orders             (one user → many orders)
  │       ├──< order_items >── books
  │       ├──  addresses  (delivery address snapshot FK)
  │       └──  payments   (one order → one payment)
  │
books
  ├──  categories         (many books → one category)
  └──  publishers         (many books → one publisher)
```

---

## 3. Table Definitions

---

### 3.1 `users`

**Migration:** `V2__users.sql`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `full_name` | `VARCHAR(150)` | NOT NULL | |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | Login identifier |
| `phone_number` | `VARCHAR(15)` | NOT NULL, UNIQUE | 10-digit Indian mobile `[CR-001]` |
| `password_hash` | `VARCHAR(255)` | NOT NULL | BCrypt hash |
| `gift_point_balance` | `INTEGER` | NOT NULL, DEFAULT 0, CHECK ≥ 0 | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |

```sql
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    full_name           VARCHAR(150)  NOT NULL,
    email               VARCHAR(255)  NOT NULL UNIQUE,
    phone_number        VARCHAR(15)   NOT NULL UNIQUE,
    password_hash       VARCHAR(255)  NOT NULL,
    gift_point_balance  INTEGER       NOT NULL DEFAULT 0 CHECK (gift_point_balance >= 0),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
```

---

### 3.2 `addresses`

**Migration:** `V3__addresses.sql`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | NOT NULL, FK → users.id | |
| `label` | `VARCHAR(100)` | NULL | e.g. "Home", "Office" |
| `street` | `VARCHAR(255)` | NOT NULL | |
| `city` | `VARCHAR(100)` | NOT NULL | |
| `state` | `VARCHAR(100)` | NOT NULL | |
| `pincode` | `VARCHAR(10)` | NOT NULL | |
| `is_default` | `BOOLEAN` | NOT NULL, DEFAULT false | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |

```sql
CREATE TABLE addresses (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label       VARCHAR(100),
    street      VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    pincode     VARCHAR(10)  NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
```

---

### 3.3 `categories`

**Migration:** `V4__categories.sql`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `name` | `VARCHAR(100)` | NOT NULL, UNIQUE |

```sql
CREATE TABLE categories (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);
```

---

### 3.4 `publishers`

**Migration:** `V5__publishers.sql`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `name` | `VARCHAR(255)` | NOT NULL, UNIQUE |

```sql
CREATE TABLE publishers (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(255) NOT NULL UNIQUE
);
```

---

### 3.5 `books`

**Migration:** `V6__books.sql`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `isbn` | `VARCHAR(13)` | NOT NULL, UNIQUE | ISBN-13 preferred |
| `title` | `VARCHAR(500)` | NOT NULL | |
| `authors` | `TEXT` | NOT NULL | Comma-separated list |
| `description` | `TEXT` | NOT NULL | |
| `cover_image_url` | `VARCHAR(1000)` | NOT NULL | Open Library cover URL |
| `publisher_id` | `BIGINT` | NOT NULL, FK → publishers.id | |
| `published_date` | `VARCHAR(10)` | NULL | Year string e.g. "2008" |
| `page_count` | `INTEGER` | NULL | |
| `language` | `VARCHAR(10)` | NOT NULL, DEFAULT 'en' | |
| `category_id` | `BIGINT` | NOT NULL, FK → categories.id | |
| `price` | `NUMERIC(10,2)` | NOT NULL, CHECK > 0 | In ₹ |
| `stock_quantity` | `INTEGER` | NOT NULL, DEFAULT 0, CHECK ≥ 0 | |
| `search_vector` | `TSVECTOR` | GENERATED ALWAYS, STORED | `[CR-002]` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |

```sql
CREATE TABLE books (
    id               BIGSERIAL        PRIMARY KEY,
    isbn             VARCHAR(13)      NOT NULL UNIQUE,
    title            VARCHAR(500)     NOT NULL,
    authors          TEXT             NOT NULL,
    description      TEXT             NOT NULL,
    cover_image_url  VARCHAR(1000)    NOT NULL,
    publisher_id     BIGINT           NOT NULL REFERENCES publishers(id),
    published_date   VARCHAR(10),
    page_count       INTEGER,
    language         VARCHAR(10)      NOT NULL DEFAULT 'en',
    category_id      BIGINT           NOT NULL REFERENCES categories(id),
    price            NUMERIC(10,2)    NOT NULL CHECK (price > 0),
    stock_quantity   INTEGER          NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    search_vector    TSVECTOR GENERATED ALWAYS AS (
                         setweight(to_tsvector('english', coalesce(title, '')),   'A') ||
                         setweight(to_tsvector('english', coalesce(authors, '')), 'B') ||
                         setweight(to_tsvector('simple',  coalesce(isbn, '')),    'D')
                     ) STORED,
    created_at       TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- GIN index for fast full-text search [CR-002]
CREATE INDEX books_search_idx   ON books USING GIN(search_vector);

-- Supporting indexes for filter queries
CREATE INDEX idx_books_category  ON books(category_id);
CREATE INDEX idx_books_publisher ON books(publisher_id);
CREATE INDEX idx_books_price     ON books(price);
CREATE INDEX idx_books_stock     ON books(stock_quantity);
```

---

### 3.6 `carts`

**Migration:** `V7__cart.sql`

One cart per registered user.

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `user_id` | `BIGINT` | NOT NULL, UNIQUE, FK → users.id |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() |

```sql
CREATE TABLE carts (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

### 3.7 `cart_items`

**Migration:** `V7__cart.sql`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `cart_id` | `BIGINT` | NOT NULL, FK → carts.id | |
| `book_id` | `BIGINT` | NOT NULL, FK → books.id | |
| `quantity` | `INTEGER` | NOT NULL, CHECK ≥ 1 | |
| `added_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |

Unique constraint: one row per (cart_id, book_id) — quantity updated in place.

```sql
CREATE TABLE cart_items (
    id         BIGSERIAL   PRIMARY KEY,
    cart_id    BIGINT      NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    book_id    BIGINT      NOT NULL REFERENCES books(id),
    quantity   INTEGER     NOT NULL CHECK (quantity >= 1),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (cart_id, book_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
```

---

### 3.8 `orders`

**Migration:** `V8__orders.sql`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | NOT NULL, FK → users.id | |
| `address_id` | `BIGINT` | NOT NULL, FK → addresses.id | Delivery address at time of order |
| `status` | `VARCHAR(30)` | NOT NULL, DEFAULT 'CONFIRMED' | Enum string |
| `subtotal` | `NUMERIC(10,2)` | NOT NULL | Before discount + delivery |
| `delivery_charge` | `NUMERIC(10,2)` | NOT NULL, DEFAULT 40.00 | Always ₹40 |
| `gift_points_redeemed` | `INTEGER` | NOT NULL, DEFAULT 0 | Points used |
| `gift_point_discount` | `NUMERIC(10,2)` | NOT NULL, DEFAULT 0.00 | points × 2 |
| `grand_total` | `NUMERIC(10,2)` | NOT NULL | Final amount paid |
| `tentative_delivery_date` | `DATE` | NOT NULL | order_date + 5 business days |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |

```sql
CREATE TABLE orders (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL REFERENCES users(id),
    address_id              BIGINT          NOT NULL REFERENCES addresses(id),
    status                  VARCHAR(30)     NOT NULL DEFAULT 'CONFIRMED',
    subtotal                NUMERIC(10,2)   NOT NULL,
    delivery_charge         NUMERIC(10,2)   NOT NULL DEFAULT 40.00,
    gift_points_redeemed    INTEGER         NOT NULL DEFAULT 0,
    gift_point_discount     NUMERIC(10,2)   NOT NULL DEFAULT 0.00,
    grand_total             NUMERIC(10,2)   NOT NULL,
    tentative_delivery_date DATE            NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
```

**Order Status Values:** `CONFIRMED` → `PROCESSING` → `OUT_FOR_DELIVERY` → `DELIVERED`
Cancellable statuses: `CONFIRMED`, `PROCESSING`

---

### 3.9 `order_items`

**Migration:** `V8__orders.sql`

Snapshots book title and price at time of purchase — prices may change later.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `order_id` | `BIGINT` | NOT NULL, FK → orders.id | |
| `book_id` | `BIGINT` | NOT NULL, FK → books.id | |
| `title_snapshot` | `VARCHAR(500)` | NOT NULL | Title at time of purchase |
| `price_snapshot` | `NUMERIC(10,2)` | NOT NULL | Price at time of purchase (₹) |
| `quantity` | `INTEGER` | NOT NULL, CHECK ≥ 1 | |

```sql
CREATE TABLE order_items (
    id              BIGSERIAL      PRIMARY KEY,
    order_id        BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id         BIGINT         NOT NULL REFERENCES books(id),
    title_snapshot  VARCHAR(500)   NOT NULL,
    price_snapshot  NUMERIC(10,2)  NOT NULL,
    quantity        INTEGER        NOT NULL CHECK (quantity >= 1)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
```

---

### 3.10 `payments`

**Migration:** `V9__payments.sql`

One payment record per order (simulated).

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `order_id` | `BIGINT` | NOT NULL, UNIQUE, FK → orders.id | |
| `payment_method` | `VARCHAR(20)` | NOT NULL | `CREDIT_CARD` or `DEBIT_CARD` |
| `payment_reference` | `VARCHAR(50)` | NOT NULL, UNIQUE | UUID — simulated |
| `amount_paid` | `NUMERIC(10,2)` | NOT NULL | grand_total at time of payment |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'SUCCESS' | Simulated — always SUCCESS |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | |

```sql
CREATE TABLE payments (
    id                  BIGSERIAL      PRIMARY KEY,
    order_id            BIGINT         NOT NULL UNIQUE REFERENCES orders(id),
    payment_method      VARCHAR(20)    NOT NULL,
    payment_reference   VARCHAR(50)    NOT NULL UNIQUE,
    amount_paid         NUMERIC(10,2)  NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'SUCCESS',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
```

---

## 4. Gift Points — Calculation Reference

Stored on `users.gift_point_balance` (integer).

| Rule | Formula |
|---|---|
| Earn | `floor(grand_total / 50)` points added after payment |
| Redeem value | `points_redeemed × 2` = ₹ discount |
| Grand total | `max(subtotal − gift_point_discount + 40, 40)` |
| Expiry | Never |
| On cancellation | Redeemed points forfeited (NOT restored) |

---

## 5. Indexes Summary

| Table | Index | Type | Purpose |
|---|---|---|---|
| `books` | `books_search_idx` | GIN | Full-text search `[CR-002]` |
| `books` | `idx_books_category` | B-tree | Category filter |
| `books` | `idx_books_publisher` | B-tree | Publisher filter |
| `books` | `idx_books_price` | B-tree | Price range filter |
| `books` | `idx_books_stock` | B-tree | In-stock filter |
| `addresses` | `idx_addresses_user_id` | B-tree | Address lookup by user |
| `cart_items` | `idx_cart_items_cart_id` | B-tree | Cart item lookup |
| `orders` | `idx_orders_user_id` | B-tree | Order history by user |
| `order_items` | `idx_order_items_order_id` | B-tree | Order item lookup |

---

## 6. Constraints Summary

| Rule | Constraint |
|---|---|
| One cart per user | `UNIQUE (user_id)` on `carts` |
| One book per cart | `UNIQUE (cart_id, book_id)` on `cart_items` |
| One payment per order | `UNIQUE (order_id)` on `payments` |
| Gift balance never negative | `CHECK (gift_point_balance >= 0)` on `users` |
| Price always positive | `CHECK (price > 0)` on `books` |
| Stock never negative | `CHECK (stock_quantity >= 0)` on `books` |
| Quantity always ≥ 1 | `CHECK (quantity >= 1)` on `cart_items`, `order_items` |
