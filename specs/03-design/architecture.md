# E-Bookstore — Architecture Design

**Document:** Architecture Design v1.0
**Based on:** Specification v1.0 + Implementation Plan v1.0
**Status:** AWAITING DESIGN APPROVAL
**Date:** 2026-08-24

---

## 1. Architectural Style

**Modular Monolith.**

The application is a single deployable Spring Boot JAR with internally
separated modules. Modules communicate via direct Java method calls —
not HTTP. Each module owns its entities, repository, service, and
controller layers.

No microservices. No message brokers. No service mesh.

---

## 2. System Overview

```
┌──────────────────────────────────────────────────┐
│  Browser                                         │
│  React 19 + TypeScript + Vite 8                  │
│  Guest cart → localStorage                       │
└─────────────────┬────────────────────────────────┘
                  │  HTTPS  REST / JSON
                  ▼
┌──────────────────────────────────────────────────┐
│  Spring Boot 4.1  (Port 8080)                    │
│  Modular Monolith                                │
│                                                  │
│  auth │ user │ book │ category │ publisher       │
│  cart │ order │ payment │ recommendation         │
└──────────┬───────────────────────┬───────────────┘
           │                       │
           ▼                       ▼
    ┌─────────────┐       ┌─────────────────┐
    │ PostgreSQL  │       │ Railway Storage  │
    │ (metadata,  │       │ (cover images,   │
    │  orders,    │       │  future assets)  │
    │  users)     │       └─────────────────┘
    └─────────────┘
           │
    Seed on startup
           │
    ┌─────────────┐
    │ books.json  │
    │ (113 books) │
    └─────────────┘
```

---

## 3. Backend Architecture

### 3.1 Technology

| Component | Choice | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 4.1.0 |
| Build | Maven | (wrapper) |
| ORM | Spring Data JPA + Hibernate | (Boot-managed) |
| DB Driver | PostgreSQL JDBC | (Boot-managed) |
| Security | Spring Security + JWT | (Boot-managed) |
| Validation | Jakarta Bean Validation | (Boot-managed) |
| Migration | Flyway | (Boot-managed) |
| Logging | SLF4J + Logback | (Boot-managed) |

### 3.2 Module Structure

```
com.ebookstore.backend/
├── BackendApplication.java          ← entry point
│
├── config/
│   ├── SecurityConfig.java          ← Spring Security configuration
│   ├── JwtConfig.java               ← JWT properties from env vars
│   └── CorsConfig.java              ← CORS for React frontend
│
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── dto/
│   │   ├── LoginRequestDTO.java      ← { identifier, password }
│   │   ├── RegisterRequestDTO.java
│   │   └── AuthResponseDTO.java     ← { token, userId, fullName }
│
├── user/
│   ├── User.java                    ← entity
│   ├── UserRepository.java
│   ├── UserService.java
│   ├── UserController.java
│   ├── Address.java                 ← entity
│   ├── AddressRepository.java
│   ├── AddressService.java
│   ├── AddressController.java
│   └── dto/
│       ├── UserProfileDTO.java
│       ├── AddressDTO.java
│       └── GiftPointBalanceDTO.java
│
├── book/
│   ├── Book.java                    ← entity
│   ├── BookRepository.java
│   ├── BookService.java
│   ├── BookController.java
│   ├── BookLoader.java              ← startup seed loader
│   └── dto/
│       ├── BookSummaryDTO.java
│       ├── BookDetailDTO.java
│       └── BookFilterParams.java
│
├── category/
│   ├── Category.java
│   ├── CategoryRepository.java
│   ├── CategoryService.java
│   └── CategoryController.java
│
├── publisher/
│   ├── Publisher.java
│   ├── PublisherRepository.java
│   ├── PublisherService.java
│   └── PublisherController.java
│
├── cart/
│   ├── Cart.java
│   ├── CartItem.java
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   ├── CartService.java
│   ├── CartController.java
│   └── dto/
│       ├── AddToCartRequestDTO.java
│       ├── CartItemDTO.java
│       ├── CartResponseDTO.java
│       └── MergeCartRequestDTO.java
│
├── order/
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatus.java             ← enum
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── OrderService.java
│   ├── OrderController.java
│   └── dto/
│       ├── OrderSummaryDTO.java
│       ├── OrderDetailDTO.java
│       ├── OrderItemDTO.java
│       └── BuyAgainResponseDTO.java
│
├── checkout/
│   ├── CheckoutService.java
│   ├── CheckoutController.java
│   ├── DeliveryDateCalculator.java
│   └── dto/
│       └── CheckoutSummaryDTO.java
│
├── payment/
│   ├── PaymentService.java          ← simulated
│   ├── PaymentController.java
│   └── dto/
│       ├── PaymentRequestDTO.java
│       └── PaymentResponseDTO.java
│
├── giftpoint/
│   ├── GiftPointService.java
│   └── dto/
│       └── GiftPointBalanceDTO.java
│
└── recommendation/
    ├── RecommendationService.java
    ├── RecommendationController.java
    └── dto/
        └── RecommendationResponseDTO.java
```

### 3.3 Request Lifecycle

```
HTTP Request
     ↓
CorsFilter
     ↓
JwtAuthenticationFilter
  → validates Bearer token
  → sets SecurityContext (or passes through for public routes)
     ↓
DispatcherServlet
     ↓
Controller  (@RestController)
  → validates request DTO (@Valid)
  → calls Service
     ↓
Service  (@Service, @Transactional)
  → business logic
  → calls Repository
     ↓
Repository  (JpaRepository / @Query)
  → SQL via Hibernate
     ↓
PostgreSQL
```

### 3.4 Security Design

- **Public endpoints** (no JWT required):
  - `GET /api/books/**`
  - `GET /api/categories`
  - `GET /api/publishers`
  - `POST /api/auth/login`
  - `POST /api/auth/register`
  - `GET /actuator/health`

- **Protected endpoints** (JWT required):
  - All `/api/cart/**`
  - All `/api/orders/**`
  - All `/api/checkout/**`
  - All `/api/payment/**`
  - All `/api/user/**`
  - All `/api/recommendations/**`

- **JWT Configuration:**
  - Algorithm: HS256
  - Secret: injected from `JWT_SECRET` environment variable
  - Expiry: 24 hours
  - Claims: `sub` (userId), `email`, `iat`, `exp`

- **Password Storage:** BCrypt with strength 12

### 3.5 Error Handling

All errors return a consistent JSON shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Phone number must be exactly 10 digits",
  "timestamp": "2026-08-24T10:00:00Z"
}
```

A global `@RestControllerAdvice` (`GlobalExceptionHandler`) handles:
- `MethodArgumentNotValidException` → 400
- `EntityNotFoundException` → 404
- `DuplicateResourceException` → 409
- `InsufficientGiftPointsException` → 400
- `OrderCancellationException` → 409
- `AuthenticationException` → 401
- `AccessDeniedException` → 403
- `Exception` (catch-all) → 500 with generic message (no stack trace exposed)

---

## 4. Frontend Architecture

### 4.1 Technology

| Component | Choice | Version |
|---|---|---|
| Framework | React | 19 |
| Language | TypeScript | 6 |
| Build | Vite | 8 |
| Routing | React Router | v7 |
| State (auth) | React Context | — |
| State (cart) | Zustand or React Context | — |
| HTTP Client | Native `fetch` with wrapper | — |
| Linter | oxlint | 1.79.0 |

### 4.2 Page Structure

```
src/
├── main.tsx                  ← app entry
├── App.tsx                   ← router setup
├── api/
│   └── apiClient.ts          ← fetch wrapper (injects JWT, base URL)
├── context/
│   ├── AuthContext.tsx        ← user session, JWT storage
│   └── CartContext.tsx        ← guest/registered cart state
├── hooks/
│   ├── useAuth.ts
│   ├── useCart.ts
│   └── useCartMerge.ts       ← merge localStorage cart on login
├── pages/
│   ├── HomePage.tsx           ← catalogue + recommendations
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── CategoryPage.tsx
│   ├── PublisherPage.tsx
│   ├── SearchPage.tsx
│   ├── BookDetailPage.tsx
│   ├── CartPage.tsx
│   ├── CheckoutPage.tsx
│   ├── PaymentPage.tsx
│   ├── ConfirmationPage.tsx
│   ├── OrderHistoryPage.tsx
│   └── ProfilePage.tsx
├── components/
│   ├── BookCard.tsx
│   ├── BookGrid.tsx
│   ├── CategorySidebar.tsx
│   ├── SearchBar.tsx
│   ├── CartItem.tsx
│   ├── RecommendationStrip.tsx
│   ├── AddressSelector.tsx
│   ├── GiftPointInput.tsx
│   └── ProtectedRoute.tsx    ← redirects to login if not authenticated
└── types/
    └── index.ts              ← shared TypeScript interfaces
```

### 4.3 Guest Cart (localStorage)

```typescript
// Key used in localStorage
const GUEST_CART_KEY = 'ebookstore_guest_cart';

// Shape stored
interface GuestCartItem {
  bookId: number;
  title: string;
  coverImageUrl: string;
  price: number;
  quantity: number;
}
```

- All cart operations for unauthenticated users read/write only to `localStorage`.
- On login success → `useCartMerge` reads `localStorage`, calls `POST /api/cart/merge`, clears `localStorage`.

### 4.4 Protected Routes

Any route requiring authentication uses `<ProtectedRoute>` which checks
`AuthContext`. If not authenticated, redirects to `/login` with the
intended path stored for post-login redirect.

---

## 5. Database Architecture

Full schema is defined in `specs/03-design/database-design.md`.

Summary of tables:

| Table | Module | Key Relationships |
|---|---|---|
| `users` | user | — |
| `addresses` | user | `user_id → users.id` |
| `categories` | category | — |
| `publishers` | publisher | — |
| `books` | book | `category_id → categories.id`, `publisher_id → publishers.id` |
| `carts` | cart | `user_id → users.id` (unique) |
| `cart_items` | cart | `cart_id → carts.id`, `book_id → books.id` |
| `orders` | order | `user_id → users.id`, `address_id → addresses.id` |
| `order_items` | order | `order_id → orders.id`, `book_id → books.id` |
| `payments` | payment | `order_id → orders.id` (unique) |

---

## 6. Deployment Architecture

```
┌──────────────────────────────────────────────────┐
│  Railway Platform                                │
│                                                  │
│  ┌──────────────┐   ┌──────────────────────────┐ │
│  │ Spring Boot  │   │ PostgreSQL               │ │
│  │ Service      │──▶│ (Railway managed)        │ │
│  │ Port 8080    │   └──────────────────────────┘ │
│  └──────┬───────┘                                │
│         │                                        │
│  ┌──────▼───────────────────────────────────┐   │
│  │  Railway Storage Bucket                  │   │
│  │  (cover images, future digital assets)   │   │
│  └──────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
         ▲
         │ HTTPS
┌────────┴────────────────────────────────────────┐
│  React Frontend (served as static build)        │
│  Deployed on Railway / Vercel / Netlify         │
└─────────────────────────────────────────────────┘
```

### Environment Variables Required

| Variable | Used By | Description |
|---|---|---|
| `DB_URL` | Spring Boot | PostgreSQL JDBC URL |
| `DB_USERNAME` | Spring Boot | DB username |
| `DB_PASSWORD` | Spring Boot | DB password |
| `JWT_SECRET` | Spring Boot | HS256 signing secret (min 32 chars) |
| `VITE_API_BASE_URL` | React | Backend base URL |

---

## 7. CORS Configuration

The Spring Boot backend allows requests from the React frontend origin.

```java
// CorsConfig.java
allowedOrigins: ["http://localhost:5173", "${FRONTEND_URL}"]
allowedMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
allowedHeaders: ["*"]
allowCredentials: true
```

---

## 8. Flyway Migration Strategy

Migrations are sequential and irreversible.

| Version | File | Milestone |
|---|---|---|
| V1 | `V1__baseline.sql` | M1 |
| V2 | `V2__users.sql` | M2 |
| V3 | `V3__addresses.sql` | M2 |
| V4 | `V4__categories.sql` | M3 |
| V5 | `V5__publishers.sql` | M3 |
| V6 | `V6__books.sql` | M3 |
| V7 | `V7__cart.sql` | M4 |
| V8 | `V8__orders.sql` | M5 |
| V9 | `V9__payments.sql` | M7 |

Rules:
- Never modify an existing migration file after it has been applied.
- All schema changes go through new migration files.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate validates against
  the Flyway-managed schema, never auto-alters it.
