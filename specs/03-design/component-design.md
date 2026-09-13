# E-Bookstore — Component Design

**Document:** Component Design v1.0
**Based on:** Specification v1.0 + Implementation Plan v1.0
**Status:** AWAITING DESIGN APPROVAL
**Date:** 2026-08-24

---

## 1. Backend — Layer Pattern Per Module

Every module follows this three-layer pattern:

```
Controller  (@RestController)
    ↓   calls
Service     (@Service, @Transactional)
    ↓   calls
Repository  (JpaRepository / @Query)
    ↓   maps
Entity      (@Entity)
```

DTOs carry data in/out of controllers. Entities never leave the service layer.

---

## 2. Backend Modules

---

### 2.1 auth module

| Class | Responsibility |
|---|---|
| `AuthController` | `POST /api/auth/register`, `POST /api/auth/login` |
| `AuthService` | Register user (BCrypt hash), validate login (email OR phone), issue JWT |
| `JwtService` | Generate JWT, validate JWT, extract claims |
| `JwtAuthenticationFilter` | Extract + validate Bearer token per request, set `SecurityContext` |
| `RegisterRequestDTO` | `fullName`, `email`, `phoneNumber`, `password` |
| `LoginRequestDTO` | `identifier` (email or phone), `password` |
| `AuthResponseDTO` | `token`, `userId`, `fullName` |

**Key logic in `AuthService.login()`:**
```java
User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
    .orElseThrow(() -> new AuthenticationException());
if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
    throw new AuthenticationException();
}
return jwtService.generateToken(user);
```

---

### 2.2 user module

| Class | Responsibility |
|---|---|
| `User` | Entity — users table |
| `UserRepository` | `findByEmail`, `findByPhoneNumber`, `findByEmailOrPhoneNumber` |
| `UserService` | Get profile, get gift point balance |
| `UserController` | `GET /api/user/profile`, `GET /api/user/giftpoints` |
| `Address` | Entity — addresses table |
| `AddressRepository` | `findByUserId` |
| `AddressService` | Add address, list addresses by user |
| `AddressController` | `POST /api/user/addresses`, `GET /api/user/addresses` |

---

### 2.3 book module

| Class | Responsibility |
|---|---|
| `Book` | Entity — books table (includes `search_vector` field mapped as read-only) |
| `BookRepository` | `findAll(Pageable)`, `findById`, `findRelated`, `search` (native query) |
| `BookService` | Orchestrates browse, filter, detail, related, search |
| `BookController` | All catalogue endpoints |
| `BookLoader` | `ApplicationRunner` — reads `books.json`, inserts if DB empty |
| `BookSummaryDTO` | id, title, authors, coverImageUrl, price, category, publisher, inStock |
| `BookDetailDTO` | All fields + inStock + tentativeDeliveryDate |
| `BookFilterParams` | category, publisher, minPrice, maxPrice, inStock, page, size |
| `DeliveryDateCalculator` | `addBusinessDays(LocalDate date, int days)` — skips weekends |

**Key query in `BookRepository.search()`:**
```sql
SELECT b.*, ts_rank(b.search_vector, query) AS rank
FROM books b
JOIN categories c ON b.category_id = c.id
JOIN publishers p ON b.publisher_id = p.id,
     to_tsquery('english', :tsQuery) query
WHERE b.search_vector @@ query
   OR c.name ILIKE :rawTerm
   OR p.name ILIKE :rawTerm
ORDER BY rank DESC
```

**`DeliveryDateCalculator` logic:**
```java
public static LocalDate addBusinessDays(LocalDate from, int days) {
    LocalDate result = from;
    int added = 0;
    while (added < days) {
        result = result.plusDays(1);
        if (result.getDayOfWeek() != SATURDAY && result.getDayOfWeek() != SUNDAY) {
            added++;
        }
    }
    return result;
}
```

---

### 2.4 category module

| Class | Responsibility |
|---|---|
| `Category` | Entity — categories table |
| `CategoryRepository` | `findByName`, `findAll` |
| `CategoryService` | List all categories, find or create by name (used by BookLoader) |
| `CategoryController` | `GET /api/categories` |

---

### 2.5 publisher module

| Class | Responsibility |
|---|---|
| `Publisher` | Entity — publishers table |
| `PublisherRepository` | `findByName`, `findAll` |
| `PublisherService` | List all publishers, find or create by name (used by BookLoader) |
| `PublisherController` | `GET /api/publishers` |

---

### 2.6 cart module

| Class | Responsibility |
|---|---|
| `Cart` | Entity — carts table |
| `CartItem` | Entity — cart_items table |
| `CartRepository` | `findByUserId` |
| `CartItemRepository` | `findByCartId`, `findByCartIdAndBookId` |
| `CartService` | Get or create cart, add item (upsert quantity), update, delete, merge |
| `CartController` | All `/api/cart/**` endpoints |
| `AddToCartRequestDTO` | `bookId`, `quantity` |
| `CartItemDTO` | id, bookId, title, coverImageUrl, price, quantity, itemTotal |
| `CartResponseDTO` | items, subtotal, deliveryCharge (40), grandTotal |
| `MergeCartRequestDTO` | `items: [{bookId, quantity}]` |

**Merge logic in `CartService.merge()`:**
```java
for (MergeItem item : request.getItems()) {
    CartItem existing = cartItemRepo.findByCartIdAndBookId(cart.getId(), item.getBookId());
    if (existing != null) {
        existing.setQuantity(existing.getQuantity() + item.getQuantity());
    } else {
        cartItemRepo.save(new CartItem(cart, book, item.getQuantity()));
    }
}
```

---

### 2.7 order module

| Class | Responsibility |
|---|---|
| `Order` | Entity — orders table |
| `OrderItem` | Entity — order_items table |
| `OrderStatus` | Enum: `CONFIRMED`, `PROCESSING`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED` |
| `OrderRepository` | `findByUserIdOrderByCreatedAtDesc` |
| `OrderItemRepository` | `findByOrderId` |
| `OrderService` | `createOrder()`, `listOrders()`, `getOrder()`, `buyAgain()`, `cancelOrder()` |
| `OrderController` | All `/api/orders/**` endpoints |
| `OrderSummaryDTO` | id, createdAt, status, itemCount, grandTotal |
| `OrderDetailDTO` | Full order including items, address, totals |
| `BuyAgainResponseDTO` | addedItems, skippedItems |

**Cancel guard in `OrderService.cancelOrder()`:**
```java
Set<OrderStatus> cancellable = Set.of(CONFIRMED, PROCESSING);
if (!cancellable.contains(order.getStatus())) {
    throw new OrderCancellationException("Cannot cancel order with status: " + order.getStatus());
}
order.setStatus(CANCELLED);
// Note: redeemed gift points are NOT restored per Decision D-010
```

---

### 2.8 checkout module

| Class | Responsibility |
|---|---|
| `CheckoutService` | Build checkout summary: cart + addresses + delivery date + gift point balance |
| `CheckoutController` | `GET /api/checkout/summary` |
| `CheckoutSummaryDTO` | items, subtotal, deliveryCharge, grandTotal, giftPointBalance, maxGiftPointDiscount, tentativeDeliveryDate, savedAddresses |

---

### 2.9 payment module

| Class | Responsibility |
|---|---|
| `PaymentService` | Validate request, apply gift points, call `OrderService.createOrder()`, save Payment record |
| `PaymentController` | `POST /api/payment/initiate` |
| `PaymentRequestDTO` | `addressId`, `paymentMethod`, `giftPointsToRedeem` |
| `PaymentResponseDTO` | Full purchase confirmation (orderId, totals, pointsEarned, deliveryDate) |

**Transaction in `PaymentService.initiate()`:**
```
@Transactional:
  1. Validate cart not empty
  2. Validate address belongs to user
  3. Validate giftPointsToRedeem ≤ balance
  4. Calculate grand total
  5. Create Order (CONFIRMED)
  6. Create Payment record (simulated UUID reference)
  7. Deduct gift points redeemed from user balance
  8. Award new gift points: floor(grandTotal / 50)
  9. Clear cart
  10. Return PaymentResponseDTO
```

---

### 2.10 giftpoint service

Embedded within the `payment` and `user` modules (no separate controller):

| Method | Logic |
|---|---|
| `award(userId, grandTotal)` | `balance += floor(grandTotal / 50)` |
| `redeem(userId, points)` | Validates `points ≤ balance`, then `balance -= points` |
| `getBalance(userId)` | Returns `user.giftPointBalance` |

---

### 2.11 recommendation module

| Class | Responsibility |
|---|---|
| `RecommendationService` | Rule-based scoring: +2 same category, +1 same author, exclude purchased |
| `RecommendationController` | `GET /api/recommendations?limit=8` |
| `RecommendationResponseDTO` | List of `BookSummaryDTO` |

**Algorithm in `RecommendationService.getRecommendations(userId, limit)`:**
```
1. SELECT DISTINCT book_id FROM order_items WHERE order_id IN
   (SELECT id FROM orders WHERE user_id = :userId)
   → purchasedBookIds

2. If purchasedBookIds is empty → return []

3. SELECT DISTINCT category_id, primary_author FROM books
   WHERE id IN :purchasedBookIds
   → purchasedCategories, purchasedAuthors

4. SELECT id, title, ... ,
     (CASE WHEN category_id IN :purchasedCategories THEN 2 ELSE 0 END +
      CASE WHEN split_part(authors, ',', 1) IN :purchasedAuthors THEN 1 ELSE 0 END)
     AS score
   FROM books
   WHERE id NOT IN :purchasedBookIds
   AND score > 0
   ORDER BY score DESC, created_at DESC
   LIMIT :limit
```

---

## 3. Frontend Component Design

---

### 3.1 Global State

| Store | Technology | Contents |
|---|---|---|
| `AuthContext` | React Context | `user`, `token`, `login()`, `logout()` |
| `CartContext` | React Context | Cart items, guest/registered logic, `addToCart()`, `removeItem()`, `updateQty()` |

**Cart routing logic in `CartContext`:**
```typescript
// If authenticated → API calls
// If guest → localStorage reads/writes
const addToCart = (item: CartItem) => {
  if (isAuthenticated) {
    apiClient.post('/api/cart/items', item);
  } else {
    updateLocalStorage(item);
  }
};
```

---

### 3.2 API Client

```typescript
// src/api/apiClient.ts
const apiClient = {
  get: (url, params?) => fetch(BASE_URL + url + toQueryString(params), {
    headers: { Authorization: `Bearer ${getToken()}` }
  }).then(handleResponse),

  post: (url, body) => fetch(BASE_URL + url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${getToken()}` },
    body: JSON.stringify(body)
  }).then(handleResponse),
  // put, delete similarly
};
```

---

### 3.3 Page → API Mapping

| Page | API Calls |
|---|---|
| `HomePage` | `GET /api/books`, `GET /api/categories`, `GET /api/recommendations?limit=8` |
| `LoginPage` | `POST /api/auth/login`, then `POST /api/cart/merge` |
| `RegisterPage` | `POST /api/auth/register` |
| `CategoryPage` | `GET /api/books?category=X` |
| `PublisherPage` | `GET /api/books?publisher=X` |
| `SearchPage` | `GET /api/books/search?q=X` |
| `BookDetailPage` | `GET /api/books/{id}`, `GET /api/books/{id}/related` |
| `CartPage` | `GET /api/cart` (or localStorage), `GET /api/recommendations?limit=4` |
| `CheckoutPage` | `GET /api/checkout/summary`, `GET /api/user/addresses` |
| `PaymentPage` | `POST /api/payment/initiate` |
| `ConfirmationPage` | Display data from `PaymentResponseDTO` |
| `OrderHistoryPage` | `GET /api/orders`, `POST /api/orders/{id}/buy-again`, `POST /api/orders/{id}/cancel` |
| `ProfilePage` | `GET /api/user/profile`, `GET /api/user/giftpoints`, `GET /api/user/addresses` |

---

### 3.4 Protected Route

```typescript
// src/components/ProtectedRoute.tsx
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return children;
};
```

Pages requiring auth: `CartPage` (registered cart), `CheckoutPage`, `PaymentPage`,
`ConfirmationPage`, `OrderHistoryPage`, `ProfilePage`.

---

### 3.5 Cart Merge Hook

```typescript
// src/hooks/useCartMerge.ts
export const useCartMerge = () => {
  const mergeCart = async () => {
    const guestItems = getLocalStorageCart(); // read localStorage
    if (guestItems.length > 0) {
      await apiClient.post('/api/cart/merge', { items: guestItems });
      clearLocalStorageCart();
    }
  };
  return { mergeCart };
};
```

Called immediately after successful login in `LoginPage`.

---

## 4. Key Design Decisions Summary

| Decision | Implementation |
|---|---|
| Guest cart never in DB | localStorage only — `CartContext` routes by auth state |
| Login by email OR phone | `AuthService` queries `findByEmailOrPhoneNumber` |
| Full-text search | `tsvector` + GIN index + `ts_rank` ordering `[CR-002]` |
| Simulated payment | `PaymentService` always succeeds, generates UUID reference |
| Gift point atomicity | Entire payment flow in single `@Transactional` method |
| Price snapshots | `order_items.price_snapshot` captured at order creation |
| Delivery date | `DeliveryDateCalculator.addBusinessDays(today, 5)` |
| Error handling | Global `@RestControllerAdvice` — no stack traces to client |
| Secrets | All via env vars — `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
