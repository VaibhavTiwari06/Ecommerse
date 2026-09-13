# E-Bookstore — Implementation Plan

**Document:** Implementation Plan v1.0
**Project:** AI-Assisted E-Commerce Bookstore (Capstone)
**Status:** AWAITING APPROVAL
**Date:** 2026-08-24
**Based on:** `specs/01-specification/specification.md` v1.0

---

## 1. Purpose

This document defines **WHAT** will be built and in **WHAT ORDER**.

It breaks the approved specification into milestones, tasks, and test
activities with full traceability to Requirement IDs.

---

## 2. Approval Gate

This plan was produced after:

```
SPECIFICATION APPROVED ✅ (2026-08-24)
```

Implementation does not begin until:

```
PLAN APPROVED
```

---

## 3. Milestone Overview

| Milestone | Description | Depends On |
|---|---|---|
| **M1** | Foundation — project scaffold, DB, security skeleton | — |
| **M2** | Auth & Users — registration, login, JWT, addresses | M1 |
| **M3** | Book Catalogue — entities, seed loader, browse, search | M1 |
| **M4** | Cart — guest localStorage (frontend), registered DB cart, merge | M2, M3 |
| **M5** | Order Management — create, history, buy again, cancel | M4 |
| **M6** | Checkout — address selection, delivery charge, delivery date | M5 |
| **M7** | Payment — simulated flow, card selection, confirmation | M6, M8 |
| **M8** | Gift Points — earn, redeem, balance | M5 |
| **M9** | Recommendations — rule-based engine, display | M5 |
| **M10** | Seed Pipeline — already complete ✅ | — |
| **M11** | Frontend — all React pages and components | M2–M9 (per feature) |
| **M12** | Testing & Spec Validation | All milestones |

---

## 4. Execution Order

```
M1 Foundation
      ↓
M2 Auth & Users ←──────────────────────────────┐
      ↓                                         │
M3 Catalogue + M10 Seed ✅ (parallel)           │
      ↓                                         │
M4 Cart                                         │
      ↓                                         │
M5 Orders                                       │
      ↓                   ↓                     │
M6 Checkout          M8 Gift Points             │
      ↓                   ↓                     │
M7 Payment ←─────────────┘                      │
      ↓                                         │
M9 Recommendations                              │
      ↓                                         │
M11 Frontend (built alongside each milestone) ──┘
      ↓
M12 Testing & Spec Validation
```

---

## 5. Milestone M1 — Foundation

**Goal:** Working Spring Boot app connecting to PostgreSQL with security skeleton,
Flyway migrations configured, and project structure established.

**Requirements:** REQ-NFR-001, REQ-NFR-005, REQ-NFR-006

---

### TASK-FOUND-001 — Backend Project Verification
**Req:** REQ-NFR-005, REQ-NFR-006
**Description:** Verify the existing Spring Boot scaffold compiles and starts.
Confirm `application.properties` loads from environment variables.
Confirm no secrets are hardcoded.

**Deliverables:**
- `mvn clean verify` passes with no errors.
- Application starts and `/actuator/health` returns `{"status":"UP"}`.

---

### TASK-FOUND-002 — Flyway Migration Setup
**Req:** REQ-NFR-001, REQ-NFR-005
**Description:** Add Flyway dependency to `pom.xml`. Create the initial baseline
migration directory `src/main/resources/db/migration/`.
Create `V1__baseline.sql` as a no-op baseline marker.

**Deliverables:**
- Flyway runs on startup without errors.
- `flyway_schema_history` table created in PostgreSQL.

---

### TASK-FOUND-003 — Spring Security Skeleton
**Req:** REQ-NFR-001
**Description:** Configure Spring Security to:
- Permit unauthenticated access to public endpoints (catalogue, search, auth).
- Require JWT authentication for all other endpoints.
- Register a `JwtAuthenticationFilter` stub (full implementation in M2).

**Deliverables:**
- Public endpoints accessible without a token.
- Protected endpoint returns 401 without a token.

---

### TASK-FOUND-004 — Logging Configuration
**Req:** REQ-NFR-006
**Description:** Confirm structured logging is configured. Verify no sensitive
fields are logged. Add a log on application startup confirming environment.

**Deliverables:**
- Application logs at INFO level on startup.
- No passwords or tokens appear in any log output.

**Tests:** TEST-FOUND-001 — Spring context loads, health endpoint returns UP.

---

## 6. Milestone M2 — Auth & Users

**Goal:** Users can register, log in, receive a JWT, manage their profile,
and manage delivery addresses.

**Requirements:** REQ-USR-004, REQ-USR-005, REQ-USR-006, REQ-NFR-001

---

### TASK-AUTH-001 — User Entity & Migration
**Req:** REQ-USR-004, REQ-USR-005, CR-001
**Description:** Create Flyway migration `V2__users.sql` with the `users` table.
Create `User` JPA entity, `UserRepository`.

**Fields:** id, full_name, email (unique), phone_number (unique, VARCHAR(15)),
password_hash, gift_point_balance, created_at, updated_at.

**Deliverables:**
- Migration runs cleanly.
- `UserRepository` can save and find by email.

---

### TASK-AUTH-002 — User Registration Endpoint
**Req:** REQ-USR-004, CR-001
**Description:** Implement `POST /api/auth/register`.
- Validate: name, email (format), phone_number (10 digits numeric), password (min 8 chars).
- Hash password with BCrypt.
- Return JWT on success.
- Return 409 if email already exists.
- Return 409 if phone number already exists.
- Return 400 if phone number is not exactly 10 digits.

**Deliverables:**
- `AuthController.register()`, `AuthService.register()`.
- `RegisterRequestDTO`, `AuthResponseDTO`.

---

### TASK-AUTH-003 — User Login & JWT Endpoint
**Req:** REQ-USR-005, REQ-NFR-001, CR-001
**Description:** Implement `POST /api/auth/login`.
- Request body: `{ "identifier": "<email or phone>", "password": "..." }`.
- Resolve user: `WHERE email = :identifier OR phone_number = :identifier`.
- Validate credentials using BCrypt.
- Return JWT on success.
- Return 401 with generic message on failure (no hint about which field was wrong).
- JWT secret loaded from environment variable `JWT_SECRET`.
- JWT expiry: 24 hours.

**Deliverables:**
- `AuthController.login()`, `AuthService.login()`, `JwtService`.
- `LoginRequestDTO` (field: `identifier`, `password`), `AuthResponseDTO`.

---

### TASK-AUTH-004 — JWT Filter (Full Implementation)
**Req:** REQ-NFR-001
**Description:** Implement `JwtAuthenticationFilter` to:
- Extract JWT from `Authorization: Bearer` header.
- Validate signature and expiry.
- Set `SecurityContext` on valid token.
- Return 401 on invalid/expired token.

**Deliverables:**
- `JwtAuthenticationFilter` wired into Spring Security filter chain.

---

### TASK-AUTH-005 — Delivery Address Entity & Endpoints
**Req:** REQ-USR-006, REQ-CHK-001
**Description:** Create Flyway migration `V3__addresses.sql`.
Create `Address` JPA entity (id, user_id FK, label, street, city, state,
pincode, is_default).
Implement:
- `POST /api/user/addresses` — add address.
- `GET /api/user/addresses` — list all addresses for authenticated user.

**Deliverables:**
- `AddressController`, `AddressService`, `AddressRepository`.
- `AddressDTO`.

**Tests:**
- TEST-AUTH-001 — Register with valid data (incl. phone) returns 200 + JWT.
- TEST-AUTH-002 — Register with duplicate email returns 409.
- TEST-AUTH-002b — Register with duplicate phone number returns 409.
- TEST-AUTH-002c — Register with invalid phone number (9 digits) returns 400.
- TEST-AUTH-003 — Login with email + password returns JWT.
- TEST-AUTH-003b — Login with phone number + password returns JWT.
- TEST-AUTH-004 — Login with wrong password returns 401.
- TEST-AUTH-005 — Protected endpoint without JWT returns 401.
- TEST-AUTH-006 — Protected endpoint with valid JWT returns 200.
- TEST-AUTH-007 — Add address, list addresses returns correct count.

---

## 7. Milestone M3 — Book Catalogue

**Goal:** Full book catalogue is seeded from JSON on startup and exposed
via REST endpoints for browsing, filtering, search, related books, and availability.

**Requirements:** REQ-CAT-001–006, REQ-SRC-001–002, REQ-SEED-002

---

### TASK-CAT-001 — Category & Publisher Entities + Migrations
**Req:** REQ-CAT-002, REQ-CAT-003
**Description:** Create migrations `V4__categories.sql`, `V5__publishers.sql`.
Create `Category` entity (id, name), `Publisher` entity (id, name).
Create repositories.

**Deliverables:**
- `CategoryRepository`, `PublisherRepository`.
- `GET /api/categories` — list all categories.
- `GET /api/publishers` — list all publishers.

---

### TASK-CAT-002 — Book Entity & Migration
**Req:** REQ-CAT-001, REQ-CAT-004, REQ-CAT-006, CR-002
**Description:** Create migration `V6__books.sql`.
Create `Book` JPA entity with all required fields:
id, isbn (unique), title, authors (stored as comma-separated or JSON array),
description, cover_image_url, publisher_id (FK), published_date, page_count,
language, category_id (FK), price (decimal), stock_quantity, created_at,
search_vector (tsvector GENERATED ALWAYS AS weighted combination of title + authors + isbn, STORED).

Also create GIN index: `CREATE INDEX books_search_idx ON books USING GIN(search_vector)`.

**Deliverables:**
- `Book` entity, `BookRepository`.
- `search_vector` generated column + `books_search_idx` GIN index in migration.

---

### TASK-CAT-003 — Catalogue Seed Loader
**Req:** REQ-SEED-002
**Description:** Implement `BookLoader` — a Spring Boot `ApplicationRunner` that
reads `data/seed/books.json` on startup and inserts all records if the
`books` table is empty.
- Resolves or creates `Category` and `Publisher` records by name.
- Logs the number of books inserted.
- Skips loading if books table is non-empty.

**Deliverables:**
- `BookLoader.java` in the `book` module.

---

### TASK-CAT-004 — Browse & Filter Endpoints
**Req:** REQ-CAT-001, REQ-CAT-002, REQ-CAT-003, REQ-SRC-002
**Description:** Implement paginated book listing with optional filters:
`GET /api/books?category=&publisher=&minPrice=&maxPrice=&inStock=&page=&size=`

**Deliverables:**
- `BookController.listBooks()`, `BookService.findBooks()`.
- `BookSummaryDTO` (id, title, authors, coverImageUrl, price, category,
  publisher, inStock).
- `BookFilterParams` query param object.

---

### TASK-CAT-005 — Book Detail Endpoint
**Req:** REQ-CAT-004, REQ-CAT-006
**Description:** Implement `GET /api/books/{id}`.
Returns full book detail including `inStock` flag and `tentativeDeliveryDate`
(current date + 5 business days if in stock).

**Deliverables:**
- `BookController.getBook()`, `BookDetailDTO`.
- `DeliveryDateCalculator` utility (current date + 5 business days).

---

### TASK-CAT-006 — Related Books Endpoint
**Req:** REQ-CAT-005
**Description:** Implement `GET /api/books/{id}/related`.
Returns up to 4 books sharing the same category OR primary author,
excluding the requested book, ordered by price descending.

**Deliverables:**
- `BookController.getRelated()`, `BookRepository.findRelated()` (custom query).

---

### TASK-CAT-007 — Search Endpoint
**Req:** REQ-SRC-001, CR-002
**Description:** Implement `GET /api/books/search?q={term}`.
Uses PostgreSQL Full-Text Search (`tsvector` + `tsquery`) with GIN index.
- Title and authors searched via `search_vector @@ to_tsquery('english', :term)`.
- Publisher name and category name matched via JOIN + ILIKE fallback.
- Results ordered by `ts_rank` descending (most relevant first).

**Deliverables:**
- `BookController.searchBooks()`.
- `BookRepository.search()` — native query using `search_vector @@ to_tsquery(...)` + `ts_rank`.

**Tests:**
- TEST-CAT-001 — Seed loader inserts 113 books on empty DB, skips on second start.
- TEST-CAT-002 — `GET /api/books` returns paginated results.
- TEST-CAT-003 — `GET /api/books?category=Fiction` returns only Fiction books.
- TEST-CAT-004 — `GET /api/books?inStock=true` returns only in-stock books.
- TEST-CAT-005 — `GET /api/books/{id}` returns full detail with tentativeDeliveryDate.
- TEST-CAT-006 — `GET /api/books/{id}` for out-of-stock book has no delivery date.
- TEST-CAT-007 — `GET /api/books/{id}/related` returns ≤ 4 books, none matching id.
- TEST-CAT-008 — `GET /api/books/search?q=fiction` returns matching books.
- TEST-CAT-009 — `GET /api/books/search?q=` returns 400.

---

## 8. Milestone M4 — Cart

**Goal:** Registered users have a server-side cart. Guest users have a
localStorage cart (frontend only). On login, guest cart merges into server cart.

**Requirements:** REQ-CRT-001–005, REQ-USR-002, REQ-USR-003

---

### TASK-CRT-001 — Cart & CartItem Entities + Migration
**Req:** REQ-CRT-001
**Description:** Create migration `V7__cart.sql`.
`cart` table: id, user_id (FK, unique — one cart per user), created_at.
`cart_items` table: id, cart_id (FK), book_id (FK), quantity, added_at.

**Deliverables:**
- `Cart`, `CartItem` entities. `CartRepository`, `CartItemRepository`.

---

### TASK-CRT-002 — Add / Update Cart Item Endpoint
**Req:** REQ-CRT-001, REQ-CRT-003
**Description:** Implement `POST /api/cart/items`.
- If book already in cart → increment quantity.
- If book out of stock → return 400.
Implement `PUT /api/cart/items/{itemId}` — update quantity (min 1).
Implement `DELETE /api/cart/items/{itemId}` — remove item.

**Deliverables:**
- `CartController`, `CartService`.
- `AddToCartRequestDTO`, `CartItemDTO`.

---

### TASK-CRT-003 — View Cart Endpoint
**Req:** REQ-CRT-002
**Description:** Implement `GET /api/cart`.
Returns items, quantities, per-item subtotal, cart subtotal, delivery charge
(₹40), and grand total.

**Deliverables:**
- `CartController.getCart()`.
- `CartResponseDTO` (items, subtotal, deliveryCharge, grandTotal).

---

### TASK-CRT-004 — Guest Cart (Frontend — localStorage)
**Req:** REQ-USR-002, REQ-CRT-004
**Description:** Frontend-only task. Implement a `cartStore` (React context or
Zustand) that reads/writes cart items to `localStorage` for unauthenticated users.
No backend API calls.

**Deliverables:**
- `cartStore.ts` — guest cart state management.
- Add to cart, remove, update quantity, clear cart functions.

---

### TASK-CRT-005 — Cart Merge on Login (Frontend + Backend)
**Req:** REQ-USR-003, REQ-CRT-005
**Description:**
- Backend: `POST /api/cart/merge` — accepts a list of `{bookId, quantity}` items,
  merges them into the authenticated user's server cart (sum quantities on duplicates).
- Frontend: After successful login, read `localStorage` cart, call merge endpoint,
  then clear `localStorage` cart.

**Deliverables:**
- `CartController.mergeCart()`, `CartService.merge()`.
- Frontend merge hook `useCartMerge.ts`.

**Tests:**
- TEST-CRT-001 — Add book to cart, `GET /api/cart` shows item.
- TEST-CRT-002 — Adding same book twice → quantity = 2.
- TEST-CRT-003 — Adding out-of-stock book → 400.
- TEST-CRT-004 — `GET /api/cart` returns correct subtotal, ₹40 delivery, grandTotal.
- TEST-CRT-005 — `DELETE /api/cart/items/{id}` removes item.
- TEST-CRT-006 — Merge: guest items appear in server cart after login.
- TEST-CRT-007 — Merge: duplicate book quantities are summed.

---

## 9. Milestone M5 — Order Management

**Goal:** Registered users can create orders, view order history, use
Buy Again, and cancel orders that have not yet been delivered.

**Requirements:** REQ-ORD-001–004

---

### TASK-ORD-001 — Order & OrderItem Entities + Migration
**Req:** REQ-ORD-001
**Description:** Create migration `V8__orders.sql`.
`orders` table: id, user_id (FK), address_id (FK), status, subtotal,
delivery_charge, gift_points_redeemed, gift_point_discount, grand_total,
tentative_delivery_date, created_at, updated_at.
`order_items` table: id, order_id (FK), book_id (FK), title_snapshot,
price_snapshot, quantity.

**Note:** `title_snapshot` and `price_snapshot` preserve the book's details
at time of purchase (prices may change later).

**Deliverables:**
- `Order`, `OrderItem` entities. `OrderRepository`, `OrderItemRepository`.
- `OrderStatus` enum: `CONFIRMED`, `PROCESSING`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`.

---

### TASK-ORD-002 — Create Order (via Payment)
**Req:** REQ-ORD-001, REQ-PAY-003
**Description:** Implement `OrderService.createOrder()`.
Called internally after simulated payment success (M7).
- Validates cart is not empty.
- Validates address belongs to user.
- Snapshots book titles and prices.
- Deducts gift points from user balance.
- Awards new gift points.
- Clears the cart.

**Deliverables:**
- `OrderService.createOrder()`.
- `CreateOrderDTO`, `OrderResponseDTO`.

---

### TASK-ORD-003 — Order History Endpoint
**Req:** REQ-ORD-002
**Description:** Implement `GET /api/orders`.
Returns all orders for authenticated user, most recent first.
Each entry: orderId, createdAt, status, itemCount, grandTotal.

**Deliverables:**
- `OrderController.listOrders()`.
- `OrderSummaryDTO`.

---

### TASK-ORD-004 — Buy Again Endpoint
**Req:** REQ-ORD-003
**Description:** Implement `POST /api/orders/{orderId}/buy-again`.
Adds all order items to the current cart. Skips out-of-stock items.
Returns list of skipped items in response.

**Deliverables:**
- `OrderController.buyAgain()`, `OrderService.buyAgain()`.
- `BuyAgainResponseDTO` (addedItems, skippedItems).

---

### TASK-ORD-005 — Cancel Order Endpoint
**Req:** REQ-ORD-004
**Description:** Implement `POST /api/orders/{orderId}/cancel`.
- Allows cancellation only for status `CONFIRMED` or `PROCESSING`.
- Returns 409 if status is `OUT_FOR_DELIVERY`, `DELIVERED`, or `CANCELLED`.
- Returns 403 if order does not belong to authenticated user.
- Redeemed gift points are NOT restored.

**Deliverables:**
- `OrderController.cancelOrder()`, `OrderService.cancelOrder()`.

**Tests:**
- TEST-ORD-001 — Create order clears cart and returns orderId.
- TEST-ORD-002 — `GET /api/orders` returns user's orders most-recent first.
- TEST-ORD-003 — `GET /api/orders` returns empty list (not 404) for new user.
- TEST-ORD-004 — Buy Again adds items to cart, skips out-of-stock.
- TEST-ORD-005 — Cancel CONFIRMED order → status = CANCELLED.
- TEST-ORD-006 — Cancel DELIVERED order → 409.
- TEST-ORD-007 — Cancel another user's order → 403.
- TEST-ORD-008 — Gift points NOT restored after cancellation.

---

## 10. Milestone M6 — Checkout

**Goal:** Checkout flow — address selection, order summary with ₹40 delivery
charge, and tentative delivery date.

**Requirements:** REQ-CHK-001–003

---

### TASK-CHK-001 — Checkout Summary Endpoint
**Req:** REQ-CHK-001, REQ-CHK-002, REQ-CHK-003
**Description:** Implement `GET /api/checkout/summary`.
Returns:
- Cart items.
- Selected address (or prompt to select).
- Subtotal.
- Delivery charge: ₹40 (flat, always).
- Gift point balance (for redemption UI).
- Tentative delivery date (today + 5 business days).
- Grand total (before gift point application).

**Deliverables:**
- `CheckoutController`, `CheckoutService`.
- `CheckoutSummaryDTO`.

---

### TASK-CHK-002 — Delivery Date Calculator
**Req:** REQ-CHK-003, REQ-CAT-006
**Description:** Implement `DeliveryDateCalculator.addBusinessDays(date, 5)`.
Skips Saturdays and Sundays. No public holiday logic.

**Deliverables:**
- `DeliveryDateCalculator.java` utility class.

**Tests:**
- TEST-CHK-001 — Checkout summary includes deliveryCharge = 40.
- TEST-CHK-002 — Checkout summary tentativeDeliveryDate is today + 5 business days.
- TEST-CHK-003 — DeliveryDateCalculator skips weekends correctly.

---

## 11. Milestone M7 — Payment

**Goal:** Simulated payment flow — card selection, gift point application,
payment initiation, order creation, and purchase confirmation.

**Requirements:** REQ-PAY-001–005, REQ-GFT-002

---

### TASK-PAY-001 — Payment Initiation Endpoint
**Req:** REQ-PAY-001, REQ-PAY-002
**Description:** Implement `POST /api/payment/initiate`.
Request: `{ addressId, paymentMethod (CREDIT_CARD|DEBIT_CARD), giftPointsToRedeem }`.
- Validates payment method.
- Validates gift points ≤ user balance.
- Simulates payment (always succeeds for valid inputs).
- Calls `OrderService.createOrder()`.
- Returns order confirmation.

**Deliverables:**
- `PaymentController`, `PaymentService`.
- `PaymentRequestDTO`, `PaymentResponseDTO`.

---

### TASK-PAY-002 — Purchase Confirmation Response
**Req:** REQ-PAY-004, REQ-PAY-005
**Description:** `PaymentResponseDTO` includes:
orderId, itemsPurchased, subtotal, deliveryCharge, giftPointsRedeemed,
giftPointDiscount, grandTotal, paymentMethod, paymentReference (UUID),
tentativeDeliveryDate, giftPointsEarned.

**Deliverables:**
- `PaymentResponseDTO` with all fields above.

**Tests:**
- TEST-PAY-001 — Valid payment request returns 200 with orderId.
- TEST-PAY-002 — Invalid paymentMethod returns 400.
- TEST-PAY-003 — Redeeming more points than balance returns 400.
- TEST-PAY-004 — Grand total = subtotal − giftPointDiscount + 40.
- TEST-PAY-005 — Cart is empty after successful payment.
- TEST-PAY-006 — Gift points are awarded after payment.

---

## 12. Milestone M8 — Gift Points

**Goal:** Gift points are earned on purchase, redeemable at checkout,
never expire, and are forfeited on cancellation.

**Requirements:** REQ-GFT-001–003

---

### TASK-GFT-001 — Gift Point Balance Field
**Req:** REQ-GFT-003
**Description:** `gift_point_balance` column already on `users` table (TASK-AUTH-001).
Implement `GET /api/user/giftpoints` returning current balance.

**Deliverables:**
- `UserController.getGiftPoints()`.
- `GiftPointBalanceDTO`.

---

### TASK-GFT-002 — Award Gift Points on Purchase
**Req:** REQ-GFT-001
**Description:** In `OrderService.createOrder()`, after order is saved:
`pointsEarned = floor(grandTotal / 50)`.
Add `pointsEarned` to `users.gift_point_balance`.

**Deliverables:**
- `GiftPointService.award(userId, grandTotal)`.

---

### TASK-GFT-003 — Redeem Gift Points at Checkout
**Req:** REQ-GFT-002
**Description:** In `PaymentService.initiate()`:
- Validate `giftPointsToRedeem ≤ balance`.
- `discount = giftPointsToRedeem × 2`.
- `grandTotal = max(subtotal − discount + 40, 40)`.
- Deduct `giftPointsToRedeem` from balance atomically with order creation.

**Deliverables:**
- `GiftPointService.redeem(userId, points)`.

**Tests:**
- TEST-GFT-001 — Order of ₹500 awards 10 points.
- TEST-GFT-002 — Order of ₹149 awards 2 points (floor(149/50)).
- TEST-GFT-003 — Redeeming 5 points gives ₹10 discount.
- TEST-GFT-004 — Redeeming more points than balance → 400.
- TEST-GFT-005 — `GET /api/user/giftpoints` returns correct balance.
- TEST-GFT-006 — Cancelled order does NOT restore redeemed points.

---

## 13. Milestone M9 — Recommendations

**Goal:** Rule-based recommendation engine returns books matching the user's
order history categories and authors. Hidden if no order history.

**Requirements:** REQ-REC-001–002

---

### TASK-REC-001 — Recommendation Engine
**Req:** REQ-REC-001
**Description:** Implement `RecommendationService.getRecommendations(userId, limit)`.
Algorithm:
1. Fetch all book IDs previously purchased by the user.
2. Extract distinct categories and primary authors from those books.
3. Query books NOT in purchased set, scoring:
   - +2 if category matches any purchased category.
   - +1 if primary author matches any purchased author.
4. Order by score DESC, then created_at DESC.
5. Return top `limit` books.
6. If no orders exist → return empty list.

**Deliverables:**
- `RecommendationService`, `RecommendationRepository` (or custom JPQL query).

---

### TASK-REC-002 — Recommendation Endpoints
**Req:** REQ-REC-002
**Description:**
- `GET /api/recommendations?limit=8` — for home/catalogue page (default 8).
- `GET /api/recommendations?limit=4` — for basket page (default 4).
Both return 200 with empty list if no order history (section hidden on frontend).
Requires valid JWT.

**Deliverables:**
- `RecommendationController`.
- `RecommendationResponseDTO` (list of `BookSummaryDTO`).

**Tests:**
- TEST-REC-001 — User with orders gets recommendations from same category.
- TEST-REC-002 — Purchased books do not appear in recommendations.
- TEST-REC-003 — User with no orders → empty list (200 OK).
- TEST-REC-004 — Limit=8 returns at most 8 books.
- TEST-REC-005 — Limit=4 returns at most 4 books.

---

## 14. Milestone M10 — Seed Pipeline

**Status: ✅ COMPLETE**

`script/books_fetch.py` executed successfully on 2026-08-24.
Output: `data/seed/books.json` — 113 books across 8 categories.

| Check | Result |
|---|---|
| ≥ 50 books | ✅ 113 books |
| ≥ 5 categories | ✅ 8 categories |
| At least 1 out-of-stock | ✅ Confirmed |
| All required fields present | ✅ Confirmed |

Requirement REQ-SEED-001: **SATISFIED**.
Requirement REQ-SEED-002: Implemented in TASK-CAT-003.

---

## 15. Milestone M11 — Frontend

**Goal:** All React pages and components implementing the full user journey.

**Requirements:** All REQ-USR, REQ-CAT, REQ-SRC, REQ-CRT, REQ-ORD, REQ-CHK, REQ-PAY, REQ-GFT, REQ-REC

---

### TASK-FE-001 — Frontend Foundation
**Description:** Set up React Router, global state (cart store), API client
(fetch wrapper with JWT header injection), and auth context.

**Deliverables:** `router.tsx`, `apiClient.ts`, `authContext.tsx`, `cartStore.ts`.

---

### TASK-FE-002 — Login & Register Pages
**Req:** REQ-USR-004, REQ-USR-005
**Deliverables:** `LoginPage.tsx`, `RegisterPage.tsx`.

---

### TASK-FE-003 — Home / Catalogue Page
**Req:** REQ-CAT-001, REQ-CAT-002, REQ-CAT-003, REQ-REC-002
**Deliverables:** `HomePage.tsx` — catalogue grid + category/publisher filter sidebar + recommendations section.

---

### TASK-FE-004 — Search & Filter Page
**Req:** REQ-SRC-001, REQ-SRC-002
**Deliverables:** `SearchPage.tsx` — search bar + filter panel + results grid.

---

### TASK-FE-005 — Book Detail Page
**Req:** REQ-CAT-004, REQ-CAT-005, REQ-CAT-006
**Deliverables:** `BookDetailPage.tsx` — full book info + related books + Add to Cart button.

---

### TASK-FE-006 — Cart Page
**Req:** REQ-CRT-001–005, REQ-REC-002
**Deliverables:** `CartPage.tsx` — line items, quantity controls, subtotal, delivery charge, recommendations strip.

---

### TASK-FE-007 — Checkout Page
**Req:** REQ-CHK-001–003, REQ-GFT-002
**Deliverables:** `CheckoutPage.tsx` — address selector, order summary, gift point redemption input, delivery date.

---

### TASK-FE-008 — Payment Page
**Req:** REQ-PAY-001–002
**Deliverables:** `PaymentPage.tsx` — card type selector, card detail form, confirm payment button.

---

### TASK-FE-009 — Purchase Confirmation Page
**Req:** REQ-PAY-005
**Deliverables:** `ConfirmationPage.tsx` — order summary, points earned, delivery date.

---

### TASK-FE-010 — Order History Page
**Req:** REQ-ORD-002, REQ-ORD-003, REQ-ORD-004
**Deliverables:** `OrderHistoryPage.tsx` — order list, Buy Again button, Cancel button.

---

### TASK-FE-011 — Profile Page
**Req:** REQ-USR-006, REQ-GFT-003
**Deliverables:** `ProfilePage.tsx` — user info, address management, gift point balance.

---

## 16. Milestone M12 — Testing & Spec Validation

**Goal:** Every REQ-* has a passing test. Spec validation report produced.

---

### TASK-TEST-001 — Unit Tests (Backend Services)
All service classes covered with JUnit 5 + Mockito unit tests.

### TASK-TEST-002 — Integration Tests (Backend API)
All controller endpoints covered with `@SpringBootTest` + `MockMvc` tests.

### TASK-TEST-003 — Spec Validation Report
Produce `specs/01-specification/validation-report.md` mapping every
requirement to its implementation class and test result.

---

## 17. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Open Library API unavailable at seed time | Low | High | Seed output already committed; re-run if needed |
| PostgreSQL schema drift | Medium | High | All changes via Flyway migrations only |
| JWT secret not set in environment | Medium | High | Application fails fast with clear error on startup |
| Gift point balance goes negative | Low | Medium | Validated in service layer before deduction |
| Cart/order race condition | Low | Medium | DB-level constraints + transactional service methods |

---

## 18. Deliverables Summary

| Milestone | Key Deliverables |
|---|---|
| M1 | Flyway setup, Security skeleton, Health endpoint |
| M2 | User entity, Register, Login, JWT, Addresses |
| M3 | Book/Category/Publisher entities, Seed loader, Browse, Search, Filter, Related |
| M4 | Cart entity, Add/Update/Delete, Merge, Guest localStorage cart (FE) |
| M5 | Order entity, Create, History, Buy Again, Cancel |
| M6 | Checkout summary, Delivery date calculator |
| M7 | Payment flow, Purchase confirmation |
| M8 | Gift point earn, redeem, balance |
| M9 | Recommendation engine, endpoints |
| M10 | ✅ books.json (complete) |
| M11 | All React pages and components |
| M12 | Unit + Integration tests, Spec Validation Report |

---

*End of Implementation Plan v1.0*

**AWAITING:** `PLAN APPROVED`
