# E-Bookstore — API Design

**Document:** API Design v1.0
**Based on:** Specification v1.0 + Implementation Plan v1.0
**Status:** AWAITING DESIGN APPROVAL
**Date:** 2026-08-24

---

## 1. Conventions

- **Base URL:** `/api`
- **Format:** JSON (`Content-Type: application/json`)
- **Auth:** `Authorization: Bearer <JWT>` on protected endpoints
- **Currency:** All monetary values in Indian Rupees (₹) as `number` with 2 decimal places
- **Pagination:** `page` (0-based), `size` (default 20), `totalElements`, `totalPages`
- **Errors:** Consistent error shape (see §2)

---

## 2. Error Response Shape

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Phone number must be exactly 10 digits",
  "timestamp": "2026-08-24T10:00:00Z"
}
```

| HTTP Status | Meaning |
|---|---|
| 200 | Success |
| 201 | Created |
| 400 | Validation failed / bad input |
| 401 | Missing or invalid JWT |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 409 | Conflict (duplicate / invalid state transition) |
| 500 | Internal server error (generic message only) |

---

## 3. Authentication Endpoints

### POST `/api/auth/register`
**Auth:** None

**Request:**
```json
{
  "fullName": "Vaibhav Tiwari",
  "email": "vaibhav@example.com",
  "phoneNumber": "9876543210",
  "password": "securePass123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGci...",
  "userId": 1,
  "fullName": "Vaibhav Tiwari"
}
```

**Errors:** 400 (validation), 409 (duplicate email or phone)

---

### POST `/api/auth/login`
**Auth:** None

**Request:**
```json
{
  "identifier": "vaibhav@example.com",
  "password": "securePass123"
}
```
> `identifier` accepts email address OR 10-digit phone number `[CR-001]`

**Response 200:**
```json
{
  "token": "eyJhbGci...",
  "userId": 1,
  "fullName": "Vaibhav Tiwari"
}
```

**Errors:** 401 (invalid credentials — generic message)

---

## 4. User Endpoints

### GET `/api/user/profile`
**Auth:** Required

**Response 200:**
```json
{
  "userId": 1,
  "fullName": "Vaibhav Tiwari",
  "email": "vaibhav@example.com",
  "phoneNumber": "9876543210",
  "giftPointBalance": 25
}
```

---

### GET `/api/user/giftpoints`
**Auth:** Required

**Response 200:**
```json
{
  "balance": 25
}
```

---

### POST `/api/user/addresses`
**Auth:** Required

**Request:**
```json
{
  "label": "Home",
  "street": "123 MG Road",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "isDefault": true
}
```

**Response 201:**
```json
{
  "id": 1,
  "label": "Home",
  "street": "123 MG Road",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "isDefault": true
}
```

---

### GET `/api/user/addresses`
**Auth:** Required

**Response 200:**
```json
[
  {
    "id": 1,
    "label": "Home",
    "street": "123 MG Road",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001",
    "isDefault": true
  }
]
```

---

## 5. Catalogue Endpoints

### GET `/api/books`
**Auth:** None

**Query params:**
| Param | Type | Description |
|---|---|---|
| `category` | string | Filter by category name |
| `publisher` | string | Filter by publisher name |
| `minPrice` | number | Minimum price in ₹ |
| `maxPrice` | number | Maximum price in ₹ |
| `inStock` | boolean | If true, only in-stock books |
| `page` | integer | Page number (0-based, default 0) |
| `size` | integer | Page size (default 20) |

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Clean Code",
      "authors": "Robert C. Martin",
      "coverImageUrl": "https://covers.openlibrary.org/b/id/8739161-M.jpg",
      "price": 649.00,
      "category": "Technology",
      "publisher": "Prentice Hall",
      "inStock": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 113,
  "totalPages": 6
}
```

---

### GET `/api/books/{id}`
**Auth:** None

**Response 200:**
```json
{
  "id": 1,
  "isbn": "9780132350884",
  "title": "Clean Code",
  "authors": "Robert C. Martin",
  "description": "A handbook of agile software craftsmanship...",
  "coverImageUrl": "https://covers.openlibrary.org/b/id/8739161-M.jpg",
  "price": 649.00,
  "category": "Technology",
  "publisher": "Prentice Hall",
  "publishedDate": "2008",
  "pageCount": 431,
  "language": "en",
  "inStock": true,
  "stockQuantity": 12,
  "tentativeDeliveryDate": "2026-08-31"
}
```

**Errors:** 404 (book not found)

---

### GET `/api/books/{id}/related`
**Auth:** None

**Response 200:**
```json
[
  {
    "id": 5,
    "title": "The Pragmatic Programmer",
    "authors": "David Thomas, Andrew Hunt",
    "coverImageUrl": "...",
    "price": 599.00,
    "category": "Technology",
    "publisher": "Addison-Wesley",
    "inStock": true
  }
]
```
> Returns up to 4 books. Empty array if no related books found.

---

### GET `/api/books/search`
**Auth:** None

**Query params:**
| Param | Type | Required | Description |
|---|---|---|---|
| `q` | string | Yes | Search term |
| `page` | integer | No | Default 0 |
| `size` | integer | No | Default 20 |

**Response 200:** Same shape as `GET /api/books` but ordered by relevance score.

**Errors:** 400 (empty `q`)

---

### GET `/api/categories`
**Auth:** None

**Response 200:**
```json
[
  { "id": 1, "name": "Fiction" },
  { "id": 2, "name": "Technology" }
]
```

---

### GET `/api/publishers`
**Auth:** None

**Response 200:**
```json
[
  { "id": 1, "name": "Prentice Hall" },
  { "id": 2, "name": "O'Reilly Media" }
]
```

---

## 6. Cart Endpoints

### GET `/api/cart`
**Auth:** Required

**Response 200:**
```json
{
  "items": [
    {
      "id": 1,
      "bookId": 1,
      "title": "Clean Code",
      "coverImageUrl": "...",
      "price": 649.00,
      "quantity": 2,
      "itemTotal": 1298.00
    }
  ],
  "subtotal": 1298.00,
  "deliveryCharge": 40.00,
  "grandTotal": 1338.00
}
```

---

### POST `/api/cart/items`
**Auth:** Required

**Request:**
```json
{
  "bookId": 1,
  "quantity": 1
}
```

**Response 200:**
```json
{
  "id": 1,
  "bookId": 1,
  "title": "Clean Code",
  "price": 649.00,
  "quantity": 1,
  "itemTotal": 649.00
}
```

**Errors:** 400 (out of stock), 404 (book not found)

---

### PUT `/api/cart/items/{itemId}`
**Auth:** Required

**Request:**
```json
{ "quantity": 3 }
```

**Response 200:** Updated cart item.

**Errors:** 400 (quantity < 1), 404 (item not found)

---

### DELETE `/api/cart/items/{itemId}`
**Auth:** Required

**Response 204:** No content.

**Errors:** 404 (item not found)

---

### POST `/api/cart/merge`
**Auth:** Required

**Request:**
```json
{
  "items": [
    { "bookId": 1, "quantity": 2 },
    { "bookId": 3, "quantity": 1 }
  ]
}
```

**Response 200:** Full merged cart (same shape as `GET /api/cart`).

---

## 7. Checkout Endpoints

### GET `/api/checkout/summary`
**Auth:** Required

**Response 200:**
```json
{
  "items": [ ... ],
  "subtotal": 1298.00,
  "deliveryCharge": 40.00,
  "grandTotal": 1338.00,
  "giftPointBalance": 25,
  "maxGiftPointDiscount": 50.00,
  "tentativeDeliveryDate": "2026-08-31",
  "savedAddresses": [ ... ]
}
```

---

## 8. Order Endpoints

### GET `/api/orders`
**Auth:** Required

**Response 200:**
```json
[
  {
    "id": 101,
    "createdAt": "2026-08-20T14:30:00Z",
    "status": "DELIVERED",
    "itemCount": 2,
    "grandTotal": 1338.00
  }
]
```

---

### GET `/api/orders/{orderId}`
**Auth:** Required

**Response 200:**
```json
{
  "id": 101,
  "createdAt": "2026-08-20T14:30:00Z",
  "status": "DELIVERED",
  "items": [
    {
      "bookId": 1,
      "titleSnapshot": "Clean Code",
      "priceSnapshot": 649.00,
      "quantity": 2,
      "itemTotal": 1298.00
    }
  ],
  "subtotal": 1298.00,
  "deliveryCharge": 40.00,
  "giftPointsRedeemed": 0,
  "giftPointDiscount": 0.00,
  "grandTotal": 1338.00,
  "tentativeDeliveryDate": "2026-08-25",
  "deliveryAddress": {
    "street": "123 MG Road",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001"
  }
}
```

**Errors:** 403 (not owner), 404 (not found)

---

### POST `/api/orders/{orderId}/buy-again`
**Auth:** Required

**Response 200:**
```json
{
  "addedItems": [
    { "bookId": 1, "title": "Clean Code", "quantity": 2 }
  ],
  "skippedItems": [
    { "bookId": 7, "title": "Out of Stock Book", "reason": "OUT_OF_STOCK" }
  ]
}
```

**Errors:** 403 (not owner), 404 (not found)

---

### POST `/api/orders/{orderId}/cancel`
**Auth:** Required

**Response 200:**
```json
{
  "orderId": 101,
  "status": "CANCELLED",
  "message": "Order cancelled successfully. Redeemed gift points are forfeited."
}
```

**Errors:** 403 (not owner), 404 (not found), 409 (non-cancellable status)

---

## 9. Payment Endpoints

### POST `/api/payment/initiate`
**Auth:** Required

**Request:**
```json
{
  "addressId": 1,
  "paymentMethod": "CREDIT_CARD",
  "giftPointsToRedeem": 5
}
```

> `paymentMethod`: `CREDIT_CARD` or `DEBIT_CARD`

**Response 200:**
```json
{
  "orderId": 102,
  "paymentReference": "PAY-550e8400-e29b-41d4-a716-446655440000",
  "paymentMethod": "CREDIT_CARD",
  "items": [ ... ],
  "subtotal": 1298.00,
  "deliveryCharge": 40.00,
  "giftPointsRedeemed": 5,
  "giftPointDiscount": 10.00,
  "grandTotal": 1328.00,
  "giftPointsEarned": 26,
  "tentativeDeliveryDate": "2026-08-31",
  "message": "Purchase successful! Thank you for your order."
}
```

**Errors:** 400 (empty cart, invalid payment method, insufficient gift points), 404 (address not found)

---

## 10. Recommendation Endpoints

### GET `/api/recommendations`
**Auth:** Required

**Query params:**
| Param | Type | Default | Description |
|---|---|---|---|
| `limit` | integer | 8 | Max books to return (use 4 for basket) |

**Response 200:**
```json
[
  {
    "id": 5,
    "title": "The Pragmatic Programmer",
    "authors": "David Thomas",
    "coverImageUrl": "...",
    "price": 599.00,
    "category": "Technology",
    "publisher": "Addison-Wesley",
    "inStock": true
  }
]
```
> Returns empty array `[]` if user has no order history. Section hidden on frontend.

---

## 11. Health Endpoint

### GET `/actuator/health`
**Auth:** None

**Response 200:**
```json
{ "status": "UP" }
```
