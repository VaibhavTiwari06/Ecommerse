# E-Bookstore — Formal Specification

**Document:** Formal Specification v1.0
**Project:** AI-Assisted E-Commerce Bookstore (Capstone)
**Status:** AWAITING APPROVAL
**Date:** 2026-08-24
**Source:** `ebookstore/docs/BuisnessRequirements.md`
**Protocol:** `ebookstore/docs/CONTROLLED_SPEC_DRIVEN_DEVELOPMENT_PROTOCOL.md`

---

## Traceability Key

Every requirement in this document is traceable to the business requirements document.
References are written as `[BR-XXX]` or `[§N]` pointing to the corresponding
section in `BuisnessRequirements.md`.

---

## Requirement Classification

| Classification | Meaning |
|---|---|
| **CONFIRMED** | Explicitly supported by the supplied capstone requirements or wireframes |
| **PROPOSED** | Suggested to make the application operational; not explicitly supplied but agreed by project owner |
| **OUT OF SCOPE** | Explicitly excluded |

---

## 1. System Overview

The E-Bookstore is a web-based eCommerce platform for browsing and purchasing
physical books. It is built as a modular monolith with a React + TypeScript
frontend and a Java Spring Boot backend backed by PostgreSQL.

All monetary values are in **Indian Rupees (₹)** exclusively.

The catalogue is populated by an offline Python seed script that fetches book
metadata from Open Library. There is no in-application administrative interface.

---

## 2. Actors

| Actor | Description | Classification |
|---|---|---|
| **Guest User** | Unauthenticated visitor | CONFIRMED `[§4]` |
| **Registered User** | Authenticated customer | CONFIRMED `[§4]` |
| **System** | Automated behaviour (seed loader, gift point engine) | PROPOSED |

> No Admin role exists. Catalogue maintenance is performed exclusively via the
> offline seed script. `[§3.2]`

---

## 3. User Management

---

### REQ-USR-001 — Guest Browsing
**Classification:** CONFIRMED `[§5.1, §5.2]`
**Actor:** Guest User

A Guest User may:
- Access the bookstore.
- Browse the book catalogue.
- Browse books by category.
- Browse books by publisher.
- View individual book detail pages.
- View related books on a book detail page.
- Search the catalogue.
- Filter the catalogue.

A Guest User may NOT:
- Proceed to checkout.
- Place an order.
- View order history.
- Access recommendations.

**Acceptance Criteria:**
- AC1: An unauthenticated request to the catalogue returns books without requiring login.
- AC2: An unauthenticated request to checkout redirects to the login page.
- AC3: An unauthenticated request to order history returns 401.

---

### REQ-USR-002 — Guest Cart (Browser Storage)
**Classification:** CONFIRMED `[§5.2, Decision OQ-002]`
**Actor:** Guest User

A Guest User may add books to a cart stored exclusively in the browser's
`localStorage`. This cart is never sent to or stored in the database.

**Acceptance Criteria:**
- AC1: Adding a book to the cart as a guest writes only to `localStorage`.
- AC2: No API call is made to the backend when a guest adds a book to the cart.
- AC3: The guest cart persists across page refreshes within the same browser session.
- AC4: The guest cart is cleared after a successful cart merge on login.

---

### REQ-USR-003 — Guest Cart Merge on Login
**Classification:** PROPOSED `[§5.2, Decision OQ-002]`
**Actor:** Guest User → Registered User

When a Guest User logs in, any items in the guest's `localStorage` cart are
merged into the registered user's server-side cart.

Merge rule: if the same book exists in both the guest cart and the server cart,
the quantities are summed.

**Acceptance Criteria:**
- AC1: After login, items from `localStorage` are sent to `POST /api/cart/merge`.
- AC2: After merge, the `localStorage` cart is cleared.
- AC3: If the guest had no cart items, merge is a no-op.
- AC4: Duplicate books result in summed quantities, not duplicate line items.

---

### REQ-USR-004 — User Registration
**Classification:** PROPOSED `[§6.1, CR-001]`
**Actor:** Guest User

A Guest User may register for an account by providing:
- Full name
- Email address (unique)
- Phone number (unique, 10-digit Indian mobile number)
- Password (minimum 8 characters)

**Acceptance Criteria:**
- AC1: Registering with a duplicate email returns a 409 Conflict error.
- AC2: Registering with a duplicate phone number returns a 409 Conflict error.
- AC3: Password is stored as a BCrypt hash — never plaintext.
- AC4: A newly registered user receives a JWT token and is logged in immediately.
- AC5: Registration with a missing required field returns 400 Bad Request.
- AC6: Phone number must be exactly 10 digits — non-numeric or wrong length returns 400.

---

### REQ-USR-005 — User Login and Authentication
**Classification:** CONFIRMED `[§6.1, BR-001, CR-001]`
**Actor:** Registered User

A Registered User must authenticate using **email or phone number** and password.
On success the system issues a JWT access token.
All protected endpoints require a valid JWT in the `Authorization: Bearer` header.

Login request accepts an `identifier` field which may be either
the user's email address or their 10-digit phone number.

**Acceptance Criteria:**
- AC1: Valid credentials (email + password) return 200 OK with a JWT token.
- AC2: Valid credentials (phone number + password) return 200 OK with a JWT token.
- AC3: Invalid credentials return 401 Unauthorized with a generic error message (no detail about which field was wrong).
- AC4: A request to a protected endpoint without a JWT returns 401.
- AC5: A request to a protected endpoint with an expired JWT returns 401.
- AC6: Passwords are verified using BCrypt — never compared in plaintext.

---

### REQ-USR-006 — User Profile and Delivery Addresses
**Classification:** PROPOSED `[§12.1, Decision OQ-002]`
**Actor:** Registered User

A Registered User may:
- View their profile (name, email).
- Add a delivery address.
- View all saved delivery addresses.
- Select a delivery address during checkout.

A user may save **multiple delivery addresses**. `[Decision OQ-002]`

**Acceptance Criteria:**
- AC1: A user can add more than one delivery address.
- AC2: All saved addresses are returned by `GET /api/user/addresses`.
- AC3: A user can select any saved address during checkout.

---

## 4. Book Catalogue

---

### REQ-CAT-001 — Browse All Books
**Classification:** CONFIRMED `[§7.1, BR-002]`
**Actor:** Guest User, Registered User

Any user may access the full book catalogue with pagination.

**Acceptance Criteria:**
- AC1: `GET /api/books` returns a paginated list of books.
- AC2: Each book entry includes: title, author(s), cover image, price (₹), category, publisher, availability.
- AC3: Out-of-stock books are included in results with an `inStock: false` flag.

---

### REQ-CAT-002 — Browse by Category
**Classification:** CONFIRMED `[§7.2, BR-002]`
**Actor:** Guest User, Registered User

Any user may filter the catalogue by a single category.

**Categories seeded:** Fiction, Technology, History, Business, Self-Help, Science, Biography, Philosophy.

**Acceptance Criteria:**
- AC1: `GET /api/books?category={name}` returns only books in that category.
- AC2: `GET /api/categories` returns the list of all available categories.
- AC3: A request for a non-existent category returns an empty list, not a 404.

---

### REQ-CAT-003 — Browse by Publisher
**Classification:** CONFIRMED `[§7.3, BR-003]`
**Actor:** Guest User, Registered User

Any user may filter the catalogue by publisher ("brand"). `[Decision OQ-002: brand = publisher]`

**Acceptance Criteria:**
- AC1: `GET /api/books?publisher={name}` returns only books from that publisher.
- AC2: `GET /api/publishers` returns the list of all publishers present in the catalogue.

---

### REQ-CAT-004 — Book Detail Page
**Classification:** CONFIRMED `[§8]`
**Actor:** Guest User, Registered User

Any user may view the full details of a single book.

**Required fields on the detail page:**
- Title
- Author(s)
- Cover image
- Description
- Price (₹)
- Publisher
- Published date
- Page count
- Category
- ISBN
- Availability (in stock / out of stock)
- Tentative delivery date (when in stock)

**Acceptance Criteria:**
- AC1: `GET /api/books/{id}` returns all fields listed above.
- AC2: A request for a non-existent book ID returns 404.

---

### REQ-CAT-005 — Related Books
**Classification:** CONFIRMED `[§7.6, BR-004]`
**Actor:** Guest User, Registered User

When a user views a book's detail page, up to 4 related books are displayed.

**Related book rule:** books sharing the same category OR the same primary author,
excluding the book currently being viewed, ordered by price descending.

**Acceptance Criteria:**
- AC1: `GET /api/books/{id}/related` returns up to 4 books.
- AC2: None of the returned books is the book with `{id}`.
- AC3: All returned books share the same category or primary author as `{id}`.

---

### REQ-CAT-006 — Product Availability Display
**Classification:** CONFIRMED `[§7.5]`
**Actor:** Guest User, Registered User

Books with `stockQuantity > 0` are shown as **In Stock** with a tentative delivery date.
Books with `stockQuantity = 0` are shown as **Out of Stock**.

**Tentative delivery date rule:** current date + 5 business days (calculated at the
time of page load). `[Decision — proposed default]`

**Acceptance Criteria:**
- AC1: A book with `stockQuantity = 0` returns `inStock: false` and no delivery date.
- AC2: A book with `stockQuantity > 0` returns `inStock: true` and a `tentativeDeliveryDate`.
- AC3: `tentativeDeliveryDate` is current date + 5 business days.

---

## 5. Search and Filtering

---

### REQ-SRC-001 — Full-Text Search
**Classification:** CONFIRMED `[§9.1, Decision OQ-006, CR-002]`
**Actor:** Guest User, Registered User

Any user may search the catalogue. Search is performed across:
**Title, Author, Publisher, Category, ISBN.** `[Decision OQ-006: Option B]`

Search is implemented using **PostgreSQL Full-Text Search** (`tsvector` + `tsquery`)
with a GIN index for fast, relevance-ranked results. `[CR-002]`

Results are returned ordered by relevance (highest match score first).

**Acceptance Criteria:**
- AC1: `GET /api/books/search?q={term}` returns books matching the term in any of the five fields.
- AC2: Search is case-insensitive.
- AC3: Results are ordered by relevance — exact title matches appear before partial matches.
- AC4: A search with no matches returns an empty list, not a 404.
- AC5: A search with an empty `q` parameter returns 400 Bad Request.

---

### REQ-SRC-002 — Catalogue Filtering
**Classification:** CONFIRMED `[§9.2]`
**Actor:** Guest User, Registered User

The catalogue supports filtering by:
- Category
- Publisher
- Price range (min / max in ₹)
- Availability (in stock only)

Filters may be combined.

**Acceptance Criteria:**
- AC1: `GET /api/books?category=Fiction&inStock=true` returns only in-stock Fiction books.
- AC2: `GET /api/books?minPrice=200&maxPrice=500` returns only books priced ₹200–₹500.
- AC3: Multiple filters applied together narrow the results (AND logic).

---

## 6. Shopping Cart

---

### REQ-CRT-001 — Add Book to Cart (Registered)
**Classification:** CONFIRMED `[§10.1, BR-005]`
**Actor:** Registered User

A Registered User may add a book to their server-side cart.
Adding the same book again increments the quantity.

**Acceptance Criteria:**
- AC1: `POST /api/cart/items` adds a book to the user's cart.
- AC2: Adding the same book twice results in quantity = 2, not two separate line items.
- AC3: Adding an out-of-stock book returns 400 Bad Request.
- AC4: Requires valid JWT — returns 401 without one.

---

### REQ-CRT-002 — View Cart
**Classification:** CONFIRMED `[§10.2]`
**Actor:** Registered User

A Registered User may view their current cart with line items, quantities,
individual prices, subtotal, delivery charge (₹40), and grand total.

**Acceptance Criteria:**
- AC1: `GET /api/cart` returns all cart items for the authenticated user.
- AC2: Response includes `subtotal`, `deliveryCharge` (₹40), and `grandTotal`.
- AC3: An empty cart returns an empty items array, not a 404.

---

### REQ-CRT-003 — Update Cart
**Classification:** CONFIRMED `[§10.3]`
**Actor:** Registered User

A Registered User may:
- Change the quantity of a cart item (minimum 1).
- Remove a cart item entirely.

**Acceptance Criteria:**
- AC1: `PUT /api/cart/items/{itemId}` updates the quantity.
- AC2: `DELETE /api/cart/items/{itemId}` removes the item.
- AC3: Setting quantity to 0 via PUT returns 400 — use DELETE instead.

---

### REQ-CRT-004 — Guest Cart (localStorage)
**Classification:** CONFIRMED `[REQ-USR-002]`
**Actor:** Guest User

Covered fully by REQ-USR-002. Recorded here for traceability.

---

### REQ-CRT-005 — Cart Merge on Login
**Classification:** PROPOSED `[REQ-USR-003]`
**Actor:** Guest User → Registered User

Covered fully by REQ-USR-003. Recorded here for traceability.

---

## 7. Order Management

---

### REQ-ORD-001 — Create Order
**Classification:** CONFIRMED `[§11.1, BR-005]`
**Actor:** Registered User

A Registered User may place an order from their current cart.
An order requires a selected delivery address.
An order is only created if payment succeeds (see REQ-PAY-003).

**Acceptance Criteria:**
- AC1: A successful payment creates an order with status `CONFIRMED`.
- AC2: The cart is cleared after a successful order.
- AC3: An order includes all cart line items, delivery address, delivery charge (₹40), subtotal, gift points redeemed, and grand total.
- AC4: An order cannot be created from an empty cart — returns 400.

---

### REQ-ORD-002 — Order History
**Classification:** CONFIRMED `[§11.2, BR-006]`
**Actor:** Registered User

A Registered User may view a list of all their past orders, most recent first.

**Acceptance Criteria:**
- AC1: `GET /api/orders` returns all orders for the authenticated user.
- AC2: Each order entry includes order ID, date, status, items summary, and grand total (₹).
- AC3: A user with no orders receives an empty list, not a 404.
- AC4: Returns 401 without a valid JWT.

---

### REQ-ORD-003 — Buy Again
**Classification:** CONFIRMED `[§11.3, BR-007]`
**Actor:** Registered User

From the order history, a Registered User may re-add all items from a past order
to their current cart.

**Acceptance Criteria:**
- AC1: `POST /api/orders/{orderId}/buy-again` adds all order items to the current cart.
- AC2: Out-of-stock items from the original order are skipped and reported in the response.
- AC3: Returns 403 if the order does not belong to the authenticated user.

---

### REQ-ORD-004 — Cancel Order
**Classification:** CONFIRMED `[§11.4, BR-014, Decision OQ-004]`
**Actor:** Registered User

A Registered User may cancel an order **only if the order has not yet been delivered**.

Cancellable statuses: `CONFIRMED`, `PROCESSING`.
Non-cancellable statuses: `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`.

**Gift points on cancellation:** redeemed gift points are **forfeited** (not restored).
`[Decision OQ-004]`

**Acceptance Criteria:**
- AC1: `POST /api/orders/{orderId}/cancel` sets status to `CANCELLED` if the order is in a cancellable status.
- AC2: Cancelling an order with status `DELIVERED` or `OUT_FOR_DELIVERY` returns 409 Conflict.
- AC3: Returns 403 if the order does not belong to the authenticated user.
- AC4: Redeemed gift points are NOT restored on cancellation.

---

## 8. Checkout and Delivery

---

### REQ-CHK-001 — Delivery Address Selection
**Classification:** CONFIRMED `[§12.1, BR-009]`
**Actor:** Registered User

During checkout, the Registered User must select one of their saved delivery
addresses. A user may also add a new address during checkout.

**Acceptance Criteria:**
- AC1: Checkout cannot proceed without a selected delivery address.
- AC2: The user may select from all previously saved addresses.
- AC3: The user may add a new address inline during checkout.

---

### REQ-CHK-002 — Delivery Charge
**Classification:** CONFIRMED `[Decision OQ-001]`
**Actor:** Registered User

A flat delivery charge of **₹40** is applied to every order regardless of
order value, number of items, or delivery location.

**Acceptance Criteria:**
- AC1: Every order summary displays a delivery charge of ₹40.
- AC2: Grand total = subtotal − gift point discount + ₹40 delivery charge.

---

### REQ-CHK-003 — Tentative Delivery Date Display
**Classification:** CONFIRMED `[§12.2, REQ-CAT-006]`
**Actor:** Registered User

During checkout, the order summary displays a tentative delivery date
of **current date + 5 business days**.

**Acceptance Criteria:**
- AC1: Order summary includes a `tentativeDeliveryDate` field.
- AC2: The date is calculated as current date + 5 business days (excluding weekends).

---

## 9. Payment

---

### REQ-PAY-001 — Simulated Payment Flow
**Classification:** CONFIRMED `[§13, Decision OQ-003]`
**Actor:** Registered User

Payment is **simulated** — there is no real payment gateway integration.
The system mimics a payment request and always returns success for valid inputs.

**Acceptance Criteria:**
- AC1: `POST /api/payment/initiate` accepts card details and returns a simulated success response.
- AC2: No real financial transaction is made.
- AC3: Payment failure can be simulated by the frontend for testing purposes.

---

### REQ-PAY-002 — Payment Method Selection
**Classification:** CONFIRMED `[§13.2]`
**Actor:** Registered User

The user may select one of two payment methods:
- Credit card
- Debit card

**Acceptance Criteria:**
- AC1: Payment request includes a `paymentMethod` field with value `CREDIT_CARD` or `DEBIT_CARD`.
- AC2: Any other value returns 400 Bad Request.

---

### REQ-PAY-003 — Complete Payment and Create Order
**Classification:** CONFIRMED `[§13.3]`
**Actor:** Registered User

On simulated payment success:
1. An order is created with status `CONFIRMED`.
2. Gift points used are deducted from the user's balance.
3. New gift points are awarded based on the order total.
4. The cart is cleared.

**Acceptance Criteria:**
- AC1: Payment success triggers order creation (REQ-ORD-001).
- AC2: Gift points balance is updated atomically with order creation.
- AC3: Cart is cleared after successful order creation.

---

### REQ-PAY-004 — Payment Confirmation
**Classification:** CONFIRMED `[§13.4, BR-012]`
**Actor:** Registered User

After payment is processed, the system returns a payment confirmation
indicating success or failure.

**Acceptance Criteria:**
- AC1: Successful payment returns order ID, total paid (₹), and payment reference.
- AC2: Failed payment returns an error; no order is created.

---

### REQ-PAY-005 — Purchase Confirmation Screen
**Classification:** CONFIRMED `[§13.5, §16, BR-013]`
**Actor:** Registered User

After a successful purchase, the frontend displays a purchase confirmation screen.

**Required content:**
- Confirmation message
- Order ID
- Items purchased
- Total paid (₹)
- Tentative delivery date
- Gift points earned on this order

**Acceptance Criteria:**
- AC1: Confirmation screen is shown only after a successful payment.
- AC2: All fields listed above are displayed.

---

## 10. Gift Points

---

### REQ-GFT-001 — Earn Gift Points
**Classification:** CONFIRMED `[§14, Decision OQ-005 updated]`
**Actor:** System (on order completion)

For every **₹50 spent** on an order (grand total before gift point discount),
the user earns **1 gift point**.

Formula: `pointsEarned = floor(grandTotal / 50)`

Points are awarded at the time the order is created (payment success).
Gift points **never expire**. `[Decision OQ-003]`

**Acceptance Criteria:**
- AC1: An order of ₹500 awards 10 gift points.
- AC2: An order of ₹149 awards 2 gift points (floor(149/50) = 2).
- AC3: Gift points are added to the user's balance atomically with order creation.
- AC4: Points do not expire — they remain until redeemed or forfeited.

---

### REQ-GFT-002 — Redeem Gift Points
**Classification:** CONFIRMED `[§14.1, BR-011]`
**Actor:** Registered User

During checkout, a Registered User may redeem any number of their available
gift points up to their full balance.

**Redemption value:** 1 gift point = **₹2** discount. `[Decision OQ-005]`

The gift point discount is applied to the subtotal before adding the
delivery charge.

**Acceptance Criteria:**
- AC1: User may redeem between 0 and their full point balance.
- AC2: Redeeming N points reduces the order subtotal by ₹(N × 2).
- AC3: A user cannot redeem more points than their current balance — returns 400.
- AC4: Grand total = subtotal − (pointsRedeemed × 2) + ₹40 delivery charge.
- AC5: Grand total cannot be negative — minimum grand total is ₹40 (delivery only).

---

### REQ-GFT-003 — Gift Points Balance
**Classification:** PROPOSED `[§14]`
**Actor:** Registered User

A Registered User may view their current gift point balance at any time.

**Acceptance Criteria:**
- AC1: `GET /api/user/giftpoints` returns the user's current point balance.
- AC2: Balance is always ≥ 0.

---

## 11. Recommendations

---

### REQ-REC-001 — Recommendation Engine
**Classification:** CONFIRMED `[§15, BR-008, Decision OQ-007]`
**Actor:** System

The system generates book recommendations for Registered Users based on their
order history using a rule-based scoring algorithm.

**Algorithm:**
1. Extract all categories and authors from the user's past orders.
2. Score all books NOT already purchased by the user:
   - +2 points for same category as a purchased book.
   - +1 point for same primary author as a purchased book.
3. Return the top-scoring books (ties broken by most recently added to catalogue).

**No order history:** if the user has no past orders, no recommendations are
returned and the recommendation section is hidden. `[Decision OQ-007: Option A]`

**Acceptance Criteria:**
- AC1: `GET /api/recommendations` returns recommended books for the authenticated user.
- AC2: No book already purchased by the user appears in recommendations.
- AC3: Returned books score higher than books with no category/author match.
- AC4: A user with no order history receives an empty list (200 OK, not 404).

---

### REQ-REC-002 — Recommendation Display
**Classification:** CONFIRMED `[§15.1, Decision OQ-007b]`
**Actor:** Registered User

Recommendations are displayed in two locations:
- **Home / Catalogue page:** up to **8 recommendations**.
- **Basket page:** up to **4 recommendations**.

The section is hidden entirely if the user has no order history.

**Acceptance Criteria:**
- AC1: Home page recommendation endpoint returns up to 8 books.
- AC2: Basket page recommendation endpoint returns up to 4 books.
- AC3: Section is not rendered when the API returns an empty list.

---

## 12. Catalogue Seed Pipeline

---

### REQ-SEED-001 — Offline Seed Script
**Classification:** CONFIRMED `[§3.1, §19 A-001–A-003]`
**Actor:** Developer (offline, one-time)

An offline Python script (`script/books_fetch.py`) fetches book metadata from
the Open Library API and writes it to `data/seed/books.json`.

**Requirements:**
- Minimum 50 books across at least 5 categories.
- Each book must include: title, author(s), ISBN, cover image URL, description,
  publisher, published date, page count, category, price (₹), stock quantity.
- Script requires only Python 3.10+ standard library (no pip installs).

**Current seed output:** 113 books across 8 categories. ✅

**Acceptance Criteria:**
- AC1: Running the script produces a valid `books.json` with ≥ 50 books.
- AC2: All required fields are present on every book record.
- AC3: At least one book has `stockQuantity = 0` (out-of-stock path).

---

### REQ-SEED-002 — Catalogue Loader (Spring Boot Startup)
**Classification:** CONFIRMED `[§3.1, Decision A-003]`
**Actor:** System (on application startup)

On application startup, if the `books` table is empty, the Spring Boot
application reads `data/seed/books.json` and inserts all records into
the database.

**Acceptance Criteria:**
- AC1: On first startup (empty DB), all books from `books.json` are inserted.
- AC2: On subsequent startups (non-empty DB), the loader does not re-insert.
- AC3: Loader logs the number of books inserted or skipped.

---

## 13. Non-Functional Requirements

---

### REQ-NFR-001 — Authentication Security
**Classification:** CONFIRMED `[BR-001, Protocol §12]`

- All protected endpoints require a valid JWT token.
- Passwords stored as BCrypt hashes only.
- No plaintext or reversible password storage.
- JWT secret injected via environment variable — never hardcoded.

---

### REQ-NFR-002 — Currency
**Classification:** CONFIRMED `[Decision — all transactions in ₹]`

All monetary values (prices, totals, delivery charges, gift point values)
are expressed in **Indian Rupees (₹)** as decimal numbers rounded to 2 places.

---

### REQ-NFR-003 — Input Validation
**Classification:** CONFIRMED `[Protocol §12]`

All API inputs are validated server-side. Invalid requests return 400 Bad Request
with a structured error response. Client-side validation is supplementary only.

---

### REQ-NFR-004 — No Admin Interface
**Classification:** CONFIRMED `[§3.2]`

No administrative interface exists in the application. Catalogue maintenance
is performed exclusively by re-running the offline seed script.

---

### REQ-NFR-005 — Secrets Management
**Classification:** CONFIRMED `[Protocol Security Rules]`

No secrets (DB credentials, JWT secret, API keys) are hardcoded in source code
or committed to version control. All secrets are injected via environment variables.

---

### REQ-NFR-006 — Structured Logging
**Classification:** CONFIRMED `[Protocol Security Rules]`

The backend uses structured logging (Spring Boot default JSON-capable logger).
No sensitive data (passwords, tokens, card details) is logged at any level.

---

## 14. Out of Scope

The following are explicitly excluded from this project. `[§3.2]`

| Item | Reason |
|---|---|
| Real payment gateway integration | Capstone — simulated only |
| Refund processing | Not in business requirements |
| Email / SMS notifications | Not in business requirements |
| Shipping provider integration | Not in business requirements |
| E-book download / DRM | Physical books only |
| Admin UI (CRUD for books, users, orders) | Explicitly excluded `[§3.2]` |
| Multi-vendor marketplace | Not in business requirements |
| International taxation | Not in business requirements |
| Subscription purchasing | Not in business requirements |
| Advanced AI recommendation algorithms | Rule-based is sufficient |

---

## 15. Requirement Summary

| ID | Title | Classification | Phase |
|---|---|---|---|
| REQ-USR-001 | Guest Browsing | CONFIRMED | M2 |
| REQ-USR-002 | Guest Cart (localStorage) | CONFIRMED | M4 |
| REQ-USR-003 | Guest Cart Merge on Login | PROPOSED | M4 |
| REQ-USR-004 | User Registration | PROPOSED | M2 |
| REQ-USR-005 | User Login & Authentication | CONFIRMED | M2 |
| REQ-USR-006 | User Profile & Addresses | PROPOSED | M2 |
| REQ-CAT-001 | Browse All Books | CONFIRMED | M3 |
| REQ-CAT-002 | Browse by Category | CONFIRMED | M3 |
| REQ-CAT-003 | Browse by Publisher | CONFIRMED | M3 |
| REQ-CAT-004 | Book Detail Page | CONFIRMED | M3 |
| REQ-CAT-005 | Related Books | CONFIRMED | M3 |
| REQ-CAT-006 | Product Availability Display | CONFIRMED | M3 |
| REQ-SRC-001 | Full-Text Search | CONFIRMED | M3 |
| REQ-SRC-002 | Catalogue Filtering | CONFIRMED | M3 |
| REQ-CRT-001 | Add Book to Cart (Registered) | CONFIRMED | M4 |
| REQ-CRT-002 | View Cart | CONFIRMED | M4 |
| REQ-CRT-003 | Update Cart | CONFIRMED | M4 |
| REQ-CRT-004 | Guest Cart (localStorage) | CONFIRMED | M4 |
| REQ-CRT-005 | Cart Merge on Login | PROPOSED | M4 |
| REQ-ORD-001 | Create Order | CONFIRMED | M5 |
| REQ-ORD-002 | Order History | CONFIRMED | M5 |
| REQ-ORD-003 | Buy Again | CONFIRMED | M5 |
| REQ-ORD-004 | Cancel Order | CONFIRMED | M5 |
| REQ-CHK-001 | Delivery Address Selection | CONFIRMED | M6 |
| REQ-CHK-002 | Delivery Charge (₹40 flat) | CONFIRMED | M6 |
| REQ-CHK-003 | Tentative Delivery Date | CONFIRMED | M6 |
| REQ-PAY-001 | Simulated Payment Flow | CONFIRMED | M7 |
| REQ-PAY-002 | Payment Method Selection | CONFIRMED | M7 |
| REQ-PAY-003 | Complete Payment & Create Order | CONFIRMED | M7 |
| REQ-PAY-004 | Payment Confirmation | CONFIRMED | M7 |
| REQ-PAY-005 | Purchase Confirmation Screen | CONFIRMED | M7 |
| REQ-GFT-001 | Earn Gift Points | CONFIRMED | M8 |
| REQ-GFT-002 | Redeem Gift Points | CONFIRMED | M8 |
| REQ-GFT-003 | Gift Points Balance | PROPOSED | M8 |
| REQ-REC-001 | Recommendation Engine | CONFIRMED | M9 |
| REQ-REC-002 | Recommendation Display | CONFIRMED | M9 |
| REQ-SEED-001 | Offline Seed Script | CONFIRMED | M10 ✅ Done |
| REQ-SEED-002 | Catalogue Loader (Startup) | CONFIRMED | M3 |
| REQ-NFR-001 | Authentication Security | CONFIRMED | M2 |
| REQ-NFR-002 | Currency (₹ only) | CONFIRMED | All |
| REQ-NFR-003 | Input Validation | CONFIRMED | All |
| REQ-NFR-004 | No Admin Interface | CONFIRMED | All |
| REQ-NFR-005 | Secrets Management | CONFIRMED | M1 |
| REQ-NFR-006 | Structured Logging | CONFIRMED | M1 |

**Total requirements: 44**
**CONFIRMED: 36 | PROPOSED: 8 | OUT OF SCOPE: 0**

---

## 16. Decision Log

All project owner decisions that shaped this specification.

| Decision ID | Decision | Impact |
|---|---|---|
| D-001 | Physical books only | Excludes e-book download, DRM |
| D-002 | Currency = Indian Rupees (₹) | All monetary values in ₹ |
| D-003 | Guest cart = localStorage only | No guest cart DB calls |
| D-004 | Brand = Publisher | Publisher module maps to "brand" |
| D-005 | Payment = Simulated | No real gateway dependency |
| D-006 | Cancellation = before delivery only | No 48-hr timer |
| D-007 | Gift points earn = ₹50 spent = 1 pt | REQ-GFT-001 formula |
| D-008 | Gift points redeem = 1 pt = ₹2 | REQ-GFT-002 value |
| D-009 | Gift points never expire | REQ-GFT-001 |
| D-010 | Redeemed points forfeited on cancel | REQ-ORD-004 |
| D-011 | Delivery charge = ₹40 flat | REQ-CHK-002 |
| D-012 | Multiple delivery addresses = yes | REQ-USR-006 |
| D-013 | Search scope = Title+Author+Publisher+Category+ISBN | REQ-SRC-001 |
| D-014 | No order history → hide recommendations | REQ-REC-001 |
| D-015 | Recommendation count = 8 home, 4 basket | REQ-REC-002 |
| D-016 | Catalogue source = Open Library (offline) | REQ-SEED-001 |
| D-017 | No admin UI | §3.2, REQ-NFR-004 |

---

*End of Specification v1.0*

**AWAITING:** `SPECIFICATION APPROVED`
